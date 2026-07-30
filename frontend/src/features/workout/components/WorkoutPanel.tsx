import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  exerciseStatuses,
  failureReasons,
  firstMetricValidationError,
  metricActualError,
  metricInputRule,
  metricLabel,
  sessionLabel,
  sessionTypeLabel,
  toForm,
  toPayload,
  type SessionForm
} from "../lib/workout";
import type { DayNavItem, RecentWorkouts, SavePayload, SessionExercise, WorkoutSession } from "../types/workout";

const inputClass = "w-full rounded-xl border border-white/10 bg-stone-950/70 px-3 py-2 text-sm text-white outline-none transition focus:border-amber-300/60";
const secondaryButton = "rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12 disabled:opacity-60";
const primaryButton = "rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60";

export function DayNavigator({ days, selectedDayIndex, disabled, onSelect }: { days: DayNavItem[]; selectedDayIndex: number | null; disabled: boolean; onSelect: (dayIndex: number) => void }) {
  return <nav aria-label="训练日导航" className="flex gap-2 overflow-x-auto pb-2">{days.map((day) => <button key={day.dayIndex} type="button" disabled={disabled} onClick={() => onSelect(day.dayIndex)} className={["min-w-28 rounded-2xl border px-4 py-3 text-left text-sm transition disabled:opacity-60", day.dayIndex === selectedDayIndex ? "border-amber-300 bg-amber-300 text-stone-950" : "border-white/10 bg-white/6 text-stone-200 hover:bg-white/10"].join(" ")}><span className="block text-xs uppercase tracking-[0.14em] opacity-75">Day {day.dayIndex}</span><span className="mt-1 block truncate font-semibold">{day.dayName}</span><span className="mt-1 block text-xs opacity-75">{day.isRestDay ? "休息" : "训练"} · {day.dayState === "completed" ? "已完成" : day.dayState === "current" ? "当前" : "待训练"}</span></button>)}</nav>;
}

export function SessionEditor({ session, isSaving, isCompleting, error, onSave, onComplete }: { session: WorkoutSession; isSaving: boolean; isCompleting: boolean; error: string | null; onSave: (payload: SavePayload) => void; onComplete: (payload: SavePayload) => void }) {
  const [form, setForm] = useState<SessionForm>(() => toForm(session));
  const [validationError, setValidationError] = useState<string | null>(null);
  const [actualEditorKeys, setActualEditorKeys] = useState<Set<string>>(() => expandedActualEditorKeys(session));
  const [noteEditorIds, setNoteEditorIds] = useState<Set<number>>(() => expandedNoteEditorIds(session));

  useEffect(() => {
    setForm(toForm(session));
    setValidationError(null);
    setActualEditorKeys(expandedActualEditorKeys(session));
    setNoteEditorIds(expandedNoteEditorIds(session));
  }, [session]);

  const updateExercise = (index: number, patch: Partial<SessionForm["exercises"][number]>) => setForm((previous) => ({
    ...previous,
    exercises: previous.exercises.map((exercise, current) => current === index ? { ...exercise, ...patch } : exercise)
  }));

  const updateMetric = (exerciseIndex: number, itemIndex: number, metricIndex: number, actual: string) => setForm((previous) => ({
    ...previous,
    exercises: previous.exercises.map((exercise, currentExercise) => currentExercise !== exerciseIndex ? exercise : {
      ...exercise,
      items: exercise.items.map((item, currentItem) => currentItem !== itemIndex ? item : {
        ...item,
        metrics: item.metrics.map((metric, currentMetric) => currentMetric === metricIndex ? { ...metric, actual } : metric)
      })
    })
  }));

  const toggleActualEditor = (key: string) => setActualEditorKeys((previous) => {
    const next = new Set(previous);
    if (next.has(key)) next.delete(key);
    else next.add(key);
    return next;
  });

  const toggleNoteEditor = (sessionExerciseId: number) => setNoteEditorIds((previous) => {
    const next = new Set(previous);
    if (next.has(sessionExerciseId)) next.delete(sessionExerciseId);
    else next.add(sessionExerciseId);
    return next;
  });

  const validateMetrics = () => {
    const nextError = firstMetricValidationError(form);
    setValidationError(nextError);
    return !nextError;
  };

  const save = () => {
    if (validateMetrics()) onSave(toPayload(form));
  };

  const complete = () => {
    if (!validateMetrics()) return;
    if (session.sessionType === "workout" && form.exercises.some((exercise) => !exercise.exerciseStatus)) {
      setValidationError("请先为每个动作选择完成状态，再完成打卡。");
      return;
    }
    setValidationError(null);
    onComplete(toPayload(form));
  };

  return <div className="space-y-5">
    {session.sessionType === "rest_day" ? <section className="rounded-[28px] border border-sky-300/20 bg-sky-300/10 p-6"><p className="text-sm uppercase tracking-[0.24em] text-sky-200">Rest Day</p><h2 className="mt-2 text-2xl font-semibold text-white">完成休息日打卡</h2><p className="mt-2 text-stone-200">今天没有计划动作。可选地填写训练备注后完成打卡。</p></section> : session.exercises.map((exercise, exerciseIndex) => <ExerciseEditor key={exercise.sessionExerciseId ?? exercise.exerciseId} exercise={exercise} formExercise={form.exercises[exerciseIndex]!} actualEditorKeys={actualEditorKeys} noteEditorOpen={noteEditorIds.has(form.exercises[exerciseIndex]!.sessionExerciseId)} onPatch={(patch) => updateExercise(exerciseIndex, patch)} onMetric={(itemIndex, metricIndex, actual) => updateMetric(exerciseIndex, itemIndex, metricIndex, actual)} onToggleActualEditor={toggleActualEditor} onToggleNoteEditor={() => toggleNoteEditor(form.exercises[exerciseIndex]!.sessionExerciseId)} />)}
    <section className="rounded-[28px] border border-white/10 bg-black/20 p-5"><TextArea label="训练备注" value={form.notes} maxLength={1000} onChange={(notes) => setForm((previous) => ({ ...previous, notes }))} />{validationError || error ? <div className="mt-4 rounded-xl border border-rose-400/20 bg-rose-400/10 px-3 py-2 text-sm text-rose-100">{validationError ?? error}</div> : null}<div className="mt-5 flex flex-wrap justify-end gap-3"><button type="button" disabled={isSaving || isCompleting} onClick={save} className={secondaryButton}>{isSaving ? "保存中..." : "手动保存"}</button><button type="button" disabled={isSaving || isCompleting} onClick={complete} className={primaryButton}>{isCompleting ? "完成中..." : session.sessionType === "rest_day" ? "完成休息日打卡" : "完成训练打卡"}</button></div></section>
  </div>;
}

function ExerciseEditor({ exercise, formExercise, actualEditorKeys, noteEditorOpen, onPatch, onMetric, onToggleActualEditor, onToggleNoteEditor }: { exercise: SessionExercise; formExercise: SessionForm["exercises"][number]; actualEditorKeys: Set<string>; noteEditorOpen: boolean; onPatch: (patch: Partial<SessionForm["exercises"][number]>) => void; onMetric: (itemIndex: number, metricIndex: number, actual: string) => void; onToggleActualEditor: (key: string) => void; onToggleNoteEditor: () => void }) {
  const showFailureReason = Boolean(formExercise.exerciseStatus) && formExercise.exerciseStatus !== "completed";

  return <article className="rounded-[28px] border border-white/10 bg-white/6 p-5">
    <div><p className="text-xs uppercase tracking-[0.2em] text-amber-300">动作 {exercise.sortOrder}</p><h2 className="mt-1 text-2xl font-semibold text-white">{exercise.exerciseName}</h2></div>
    <div className="mt-5 space-y-3">{exercise.items.map((item, itemIndex) => {
      const itemKey = actualEditorKey(formExercise.sessionExerciseId, item.itemIndex);
      const actualEditorOpen = actualEditorKeys.has(itemKey);
      const hasActualValues = formExercise.items[itemIndex]!.metrics.some((metric) => metric.actual.trim() !== "");
      return <section key={item.itemIndex} className="rounded-2xl border border-white/10 bg-stone-950/45 p-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between"><div><p className="font-semibold text-white">{item.itemName ?? (item.itemType === "set" ? `第 ${item.itemIndex} 组` : `第 ${item.itemIndex} 段`)}</p>{item.note ? <p className="mt-1 text-sm text-stone-400">{item.note}</p> : null}</div><button type="button" onClick={() => onToggleActualEditor(itemKey)} className={secondaryButton}>{actualEditorOpen ? "收起实际值" : hasActualValues ? "修改实际值" : "记录实际值"}</button></div>
        <div className="mt-4 flex flex-wrap gap-2 text-sm text-stone-300">{item.metrics.map((metric) => <span key={metric.metricKey} className="rounded-full border border-white/10 bg-black/20 px-3 py-1">{metricLabel(metric.metricKey)}：{metric.plannedValueNumber ?? "未设定"} {metric.metricUnit}</span>)}</div>
        {actualEditorOpen ? <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{item.metrics.map((metric, metricIndex) => {
          const actual = formExercise.items[itemIndex]?.metrics[metricIndex]?.actual ?? "";
          const inputRule = metricInputRule(metric.metricKey);
          const inputError = metricActualError(metric.metricKey, actual);
          const inputId = `workout-${formExercise.sessionExerciseId}-${item.itemIndex}-${metric.metricKey}`;
          return <label key={metric.metricKey} htmlFor={inputId}><span className="text-sm text-stone-300">{metricLabel(metric.metricKey)}</span><span className="mt-1 block text-xs text-stone-500">留空则按计划完成</span><div className={["mt-2 flex rounded-xl border bg-black/25 focus-within:border-amber-300/60", inputError ? "border-rose-400/60" : "border-white/10"].join(" ")}><input id={inputId} type="number" min="0" step={inputRule.step} inputMode={inputRule.inputMode} value={actual} onChange={(event) => onMetric(itemIndex, metricIndex, event.target.value)} className="min-w-0 flex-1 bg-transparent px-3 py-2 text-sm text-white outline-none" placeholder="实际值" />{metric.metricUnit ? <span className="flex items-center px-3 text-xs text-stone-400">{metric.metricUnit}</span> : null}</div>{inputError ? <span className="mt-1 block text-xs text-rose-200">{inputError}</span> : null}</label>;
        })}</div> : null}
      </section>;
    })}</div>
    <div className="mt-5 space-y-4">
      <button type="button" onClick={onToggleNoteEditor} className={secondaryButton}>{noteEditorOpen ? "收起感受/备注" : "添加感受/备注"}</button>
      {noteEditorOpen ? <TextArea label="感受/备注" value={formExercise.note} maxLength={500} onChange={(note) => onPatch({ note })} /> : null}
      <label className="block"><span className="text-sm text-stone-300">动作完成状态</span><select aria-label={`${exercise.exerciseName} 完成状态`} value={formExercise.exerciseStatus} onChange={(event) => onPatch({ exerciseStatus: event.target.value, failureReason: event.target.value === "completed" ? "" : formExercise.failureReason })} className={`${inputClass} mt-2`}><option value="">选择完成状态</option>{exerciseStatuses.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
      {showFailureReason ? <label className="block"><span className="text-sm text-stone-300">失败 / 跳过原因</span><select value={formExercise.failureReason} onChange={(event) => onPatch({ failureReason: event.target.value })} className={`${inputClass} mt-2`}><option value="">不填写</option>{failureReasons.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>{formExercise.failureReason === "other" ? <span className="mt-2 block text-xs text-amber-200">如方便，请在“感受/备注”中补充具体原因，非必填。</span> : null}</label> : null}
    </div>
  </article>;
}

export function SessionReadOnly({ session }: { session: WorkoutSession }) {
  if (session.sessionType === "rest_day") return <section className="rounded-[28px] border border-sky-300/20 bg-sky-300/10 p-6"><p className="text-sm uppercase tracking-[0.24em] text-sky-200">Rest Day</p><h2 className="mt-2 text-2xl font-semibold text-white">{sessionLabel(session.sessionStatus)}</h2><p className="mt-2 text-stone-200">这一天没有计划动作，已作为休息日记录保留。</p><Notes session={session} /></section>;

  return <div className="space-y-5">{session.exercises.map((exercise) => <article key={exercise.sessionExerciseId ?? exercise.exerciseId} className="rounded-[28px] border border-white/10 bg-white/6 p-5"><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs uppercase tracking-[0.2em] text-amber-300">动作 {exercise.sortOrder}</p><h2 className="mt-1 text-xl font-semibold text-white">{exercise.exerciseName}</h2></div><span className="rounded-full border border-white/10 bg-white/8 px-3 py-1 text-xs text-stone-200">{exercise.exerciseStatus ? exerciseStatuses.find((option) => option.value === exercise.exerciseStatus)?.label : "未标记"}</span></div><div className="mt-4 space-y-3">{exercise.items.map((item) => <section key={item.itemIndex} className="rounded-2xl border border-white/10 bg-stone-950/45 p-4"><p className="font-medium text-white">{item.itemName ?? `执行项 ${item.itemIndex}`}</p><div className="mt-3 grid gap-2 sm:grid-cols-2">{item.metrics.map((metric) => <div key={metric.metricKey} className="rounded-xl border border-white/8 bg-white/5 px-3 py-2 text-sm"><p className="text-stone-400">{metricLabel(metric.metricKey)}</p><p className="mt-1 text-stone-300">计划：{metric.plannedValueNumber ?? "未设定"} {metric.metricUnit}</p><p className="text-white">实际：{metric.actualValueNumber ?? metric.plannedValueNumber ?? "未填写"} {(metric.actualValueNumber ?? metric.plannedValueNumber) === null ? "" : metric.metricUnit}</p></div>)}</div></section>)}</div><div className="mt-4 grid gap-3 text-sm sm:grid-cols-2"><Detail label="失败原因" value={exercise.failureReason ? failureReasons.find((option) => option.value === exercise.failureReason)?.label ?? exercise.failureReason : "未填写"} /><Detail label="感受/备注" value={exercise.feedback ?? "未填写"} /></div></article>)}<Notes session={session} /></div>;
}

function Notes({ session }: { session: WorkoutSession }) {
  return <section className="grid gap-3 rounded-[24px] border border-white/10 bg-black/20 p-5 sm:grid-cols-3"><Detail label="训练备注" value={session.notes ?? "未填写"} /><Detail label="记录类型" value={sessionTypeLabel(session.sessionType)} /><Detail label="当前状态" value={sessionLabel(session.sessionStatus)} /></section>;
}

function Detail({ label, value }: { label: string; value: string }) {
  return <div><p className="text-xs uppercase tracking-[0.14em] text-stone-500">{label}</p><p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-stone-200">{value}</p></div>;
}

function TextArea({ label, value, maxLength, onChange }: { label: string; value: string; maxLength: number; onChange: (value: string) => void }) {
  return <label><span className="text-sm text-stone-300">{label}</span><textarea value={value} maxLength={maxLength} onChange={(event) => onChange(event.target.value)} className={`${inputClass} mt-2 min-h-24 resize-y`} /></label>;
}

function actualEditorKey(sessionExerciseId: number, itemIndex: number) {
  return `${sessionExerciseId}-${itemIndex}`;
}

function expandedActualEditorKeys(session: WorkoutSession) {
  return new Set(session.exercises.flatMap((exercise) => exercise.items.filter((item) => item.metrics.some((metric) => metric.actualValueNumber !== null)).map((item) => actualEditorKey(exercise.sessionExerciseId ?? 0, item.itemIndex))));
}

function expandedNoteEditorIds(session: WorkoutSession) {
  return new Set(session.exercises.filter((exercise) => Boolean(exercise.feedback)).map((exercise) => exercise.sessionExerciseId ?? 0));
}

export function RecentList({ data, error }: { data: RecentWorkouts | null; error: string | null }) {
  return <section className="rounded-[28px] border border-white/10 bg-black/20 p-6"><div className="flex items-center justify-between"><div><p className="text-sm uppercase tracking-[0.24em] text-amber-300">Recent Workouts</p><h2 className="mt-2 text-2xl font-semibold text-white">最近训练记录</h2></div>{data ? <span className="text-sm text-stone-400">{data.total} 条</span> : null}</div>{error ? <p className="mt-4 text-sm text-rose-200">{error}</p> : data?.records.length ? <div className="mt-5 divide-y divide-white/8">{data.records.map((record) => <Link key={record.sessionId} to={`/workout/history/${record.sessionId}`} className="flex flex-col gap-2 py-4 transition hover:bg-white/[0.03] sm:flex-row sm:items-center sm:justify-between sm:px-3"><div><p className="font-medium text-white">{record.templateName} · Day {record.dayIndex} · {record.dayName}</p><p className="mt-1 text-sm text-stone-400">{sessionTypeLabel(record.sessionType)} · {sessionLabel(record.sessionStatus)}</p></div><p className="text-sm text-stone-300">{record.completedAt ?? record.startedAt ?? "未记录"}</p></Link>)}</div> : <p className="mt-5 text-sm text-stone-400">还没有训练记录。完成首个训练日或休息日打卡后会在这里出现。</p>}</section>;
}



