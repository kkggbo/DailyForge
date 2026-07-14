import { useState } from "react";
import { searchSystemExercises } from "../api/cycle-template";
import { goalTypeOptions } from "../lib/cycle-template-enums";
import type {
  CycleTemplateEditorForm,
  CycleTemplateFieldErrors,
  EditorExerciseForm,
  SystemExerciseOption
} from "../types/cycle-template";

type CycleTemplateEditorProps = {
  accessToken: string;
  form: CycleTemplateEditorForm;
  fieldErrors: CycleTemplateFieldErrors;
  isDirty: boolean;
  canUndo: boolean;
  isSubmitting: boolean;
  submitLabel: string;
  lockedBeforeDayIndex?: number;
  disableCycleLength?: boolean;
  cycleLengthMode?: "input" | "select";
  allowEmptyCycleLengthOption?: boolean;
  onRootFieldChange: <K extends keyof CycleTemplateEditorForm>(
    key: K,
    value: CycleTemplateEditorForm[K]
  ) => void;
  onDayChange: (
    dayIndex: number,
    patch: Partial<CycleTemplateEditorForm["days"][number]>
  ) => void;
  onAddExercise: (dayIndex: number) => void;
  onUpdateExercise: (
    dayIndex: number,
    localId: string,
    patch: Partial<EditorExerciseForm>
  ) => void;
  onRemoveExercise: (dayIndex: number, localId: string) => void;
  onMoveExercise: (dayIndex: number, localId: string, direction: -1 | 1) => void;
  onReorderExercise: (dayIndex: number, fromLocalId: string, toLocalId: string) => void;
  onUndo: () => void;
  onReset: () => void;
  onSubmit: () => void;
};

export function CycleTemplateEditor({
  accessToken,
  form,
  fieldErrors,
  isDirty,
  canUndo,
  isSubmitting,
  submitLabel,
  lockedBeforeDayIndex = 1,
  disableCycleLength = false,
  cycleLengthMode = "input",
  allowEmptyCycleLengthOption = false,
  onRootFieldChange,
  onDayChange,
  onAddExercise,
  onUpdateExercise,
  onRemoveExercise,
  onMoveExercise,
  onReorderExercise,
  onUndo,
  onReset,
  onSubmit
}: CycleTemplateEditorProps) {
  const [activeDayIndex, setActiveDayIndex] = useState(1);
  const activeDay =
    form.days.find((day) => day.dayIndex === activeDayIndex) ?? form.days[0];
  const isDayLocked = activeDay ? activeDay.dayIndex < lockedBeforeDayIndex : false;

  return (
    <div className="space-y-6">
      <section className="rounded-[28px] border border-white/10 bg-white/8 p-5 shadow-2xl shadow-black/20 backdrop-blur">
        <div className="grid gap-4 lg:grid-cols-[1.4fr_0.8fr_0.8fr]">
          <label className="space-y-2">
            <span className="text-sm text-stone-300">模板名称</span>
            <input
              value={form.templateName}
              onChange={(event) =>
                onRootFieldChange("templateName", event.target.value)
              }
              className={inputClass}
              placeholder="例如：Push Pull Legs"
            />
            <FieldError message={fieldErrors.templateName} />
          </label>

          <label className="space-y-2">
            <span className="text-sm text-stone-300">训练目标</span>
            <select
              value={form.goalType}
              onChange={(event) => onRootFieldChange("goalType", event.target.value)}
              className={inputClass}
            >
              {goalTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <FieldError message={fieldErrors.goalType} />
          </label>

          <label className="space-y-2">
            <span className="text-sm text-stone-300">周期长度</span>
            {cycleLengthMode === "select" ? (
              <select
                value={form.cycleLength}
                disabled={disableCycleLength}
                onChange={(event) =>
                  onRootFieldChange("cycleLength", event.target.value)
                }
                className={inputClass}
              >
                {allowEmptyCycleLengthOption ? <option value="">未设置</option> : null}
                {Array.from({ length: 7 }, (_, index) => String(index + 1)).map(
                  (value) => (
                    <option key={value} value={value}>
                      {value} 天
                    </option>
                  )
                )}
              </select>
            ) : (
              <input
                type="number"
                min={1}
                max={7}
                step={1}
                value={form.cycleLength}
                disabled={disableCycleLength}
                onChange={(event) =>
                  onRootFieldChange("cycleLength", event.target.value)
                }
                className={inputClass}
                placeholder="1-7"
              />
            )}
            <FieldError message={fieldErrors.cycleLength} />
          </label>
        </div>

        <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-stone-400">
            {isDirty ? "有未保存修改" : "当前内容已保存或未修改"}
          </p>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              disabled={!canUndo || isSubmitting}
              onClick={onUndo}
              className={secondaryButtonClass}
            >
              撤销上一步
            </button>
            <button
              type="button"
              disabled={!isDirty || isSubmitting}
              onClick={onReset}
              className={secondaryButtonClass}
            >
              放弃本次修改
            </button>
            <button
              type="button"
              disabled={isSubmitting}
              onClick={onSubmit}
              className={primaryButtonClass}
            >
              {isSubmitting ? "保存中..." : submitLabel}
            </button>
          </div>
        </div>
      </section>

      <section className="rounded-[28px] border border-white/10 bg-black/20 p-5 backdrop-blur">
        <div className="flex flex-wrap gap-2">
          {form.days.map((day) => (
            <button
              key={day.dayIndex}
              type="button"
              onClick={() => setActiveDayIndex(day.dayIndex)}
              className={[
                "rounded-full px-4 py-2 text-sm transition",
                activeDay?.dayIndex === day.dayIndex
                  ? "bg-amber-400 text-stone-950"
                  : "bg-white/8 text-stone-200 hover:bg-white/12",
                day.dayIndex < lockedBeforeDayIndex ? "opacity-60" : ""
              ].join(" ")}
            >
              {`Day ${day.dayIndex}`}
              {day.exercises.length === 0
                ? " · 休息日"
                : ` · ${day.exercises.length} 个动作`}
            </button>
          ))}
        </div>

        {activeDay ? (
          <div className="mt-6 space-y-5">
            <div className="grid gap-4 lg:grid-cols-[1fr_auto] lg:items-end">
              <label className="space-y-2">
                <span className="text-sm text-stone-300">训练日名称</span>
                <input
                  value={activeDay.dayName}
                  disabled={isDayLocked}
                  onChange={(event) =>
                    onDayChange(activeDay.dayIndex, { dayName: event.target.value })
                  }
                  className={inputClass}
                  placeholder={`Day ${activeDay.dayIndex}`}
                />
                <FieldError
                  message={fieldErrors[`day.${activeDay.dayIndex}.dayName`]}
                />
              </label>
              <button
                type="button"
                disabled={isDayLocked}
                onClick={() => onAddExercise(activeDay.dayIndex)}
                className={primaryButtonClass}
              >
                添加动作
              </button>
            </div>

            {isDayLocked ? (
              <div className="rounded-2xl border border-amber-300/20 bg-amber-300/10 px-4 py-3 text-sm text-amber-100">
                这一天已经开始或完成打卡，当前版本不允许继续编辑。
              </div>
            ) : null}

            {activeDay.exercises.length === 0 ? (
              <div className="rounded-3xl border border-dashed border-white/12 bg-white/5 p-8 text-center text-stone-300">
                当前训练日还没有动作，保存后会按休息日处理。
              </div>
            ) : (
              <div className="space-y-4">
                {activeDay.exercises.map((exercise, index) => (
                  <ExerciseEditor
                    key={exercise.localId}
                    accessToken={accessToken}
                    dayIndex={activeDay.dayIndex}
                    index={index}
                    exercise={exercise}
                    locked={isDayLocked}
                    fieldErrors={fieldErrors}
                    onUpdateExercise={onUpdateExercise}
                    onRemoveExercise={onRemoveExercise}
                    onMoveExercise={onMoveExercise}
                    onReorderExercise={onReorderExercise}
                  />
                ))}
              </div>
            )}
          </div>
        ) : null}
      </section>
    </div>
  );
}

type ExerciseEditorProps = {
  accessToken: string;
  dayIndex: number;
  index: number;
  exercise: EditorExerciseForm;
  locked: boolean;
  fieldErrors: CycleTemplateFieldErrors;
  onUpdateExercise: (
    dayIndex: number,
    localId: string,
    patch: Partial<EditorExerciseForm>
  ) => void;
  onRemoveExercise: (dayIndex: number, localId: string) => void;
  onMoveExercise: (dayIndex: number, localId: string, direction: -1 | 1) => void;
  onReorderExercise: (dayIndex: number, fromLocalId: string, toLocalId: string) => void;
};

function ExerciseEditor({
  accessToken,
  dayIndex,
  index,
  exercise,
  locked,
  fieldErrors,
  onUpdateExercise,
  onRemoveExercise,
  onMoveExercise,
  onReorderExercise
}: ExerciseEditorProps) {
  const [keyword, setKeyword] = useState(exercise.exerciseName);
  const [options, setOptions] = useState<SystemExerciseOption[]>([]);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const errorPrefix = `day.${dayIndex}.exercise.${exercise.localId}`;

  async function handleSearch() {
    setIsSearching(true);
    setSearchError(null);

    try {
      const response = await searchSystemExercises(accessToken, {
        keyword,
        page: 1,
        pageSize: 20
      });
      setOptions(response.records ?? []);
    } catch {
      setSearchError(
        "动作搜索接口暂不可用，请确认后端已经提供 /api/exercises/system。"
      );
    } finally {
      setIsSearching(false);
    }
  }

  return (
    <div
      draggable={!locked}
      onDragStart={(event) => {
        event.dataTransfer.setData("text/plain", exercise.localId);
      }}
      onDragOver={(event) => event.preventDefault()}
      onDrop={(event) => {
        const fromLocalId = event.dataTransfer.getData("text/plain");
        onReorderExercise(dayIndex, fromLocalId, exercise.localId);
      }}
      className="rounded-3xl border border-white/10 bg-stone-950/50 p-4"
    >
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.2em] text-amber-300">
            动作 {index + 1}
          </p>
          <p className="mt-1 text-lg font-semibold text-white">
            {exercise.exerciseName || "还没有选择动作"}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            disabled={locked}
            onClick={() => onMoveExercise(dayIndex, exercise.localId, -1)}
            className={secondaryButtonClass}
          >
            上移
          </button>
          <button
            type="button"
            disabled={locked}
            onClick={() => onMoveExercise(dayIndex, exercise.localId, 1)}
            className={secondaryButtonClass}
          >
            下移
          </button>
          <button
            type="button"
            disabled={locked}
            onClick={() => onRemoveExercise(dayIndex, exercise.localId)}
            className="rounded-full border border-rose-300/25 px-3 py-2 text-sm text-rose-100 transition hover:bg-rose-400/10 disabled:cursor-not-allowed disabled:opacity-50"
          >
            删除
          </button>
        </div>
      </div>

      <div className="mt-4 grid gap-3 lg:grid-cols-[1fr_auto]">
        <label className="space-y-2">
          <span className="text-sm text-stone-300">搜索系统动作</span>
          <input
            value={keyword}
            disabled={locked}
            onChange={(event) => setKeyword(event.target.value)}
            className={inputClass}
            placeholder="输入卧推、深蹲、跑步等关键词"
          />
        </label>
        <button
          type="button"
          disabled={locked || isSearching}
          onClick={() => {
            void handleSearch();
          }}
          className={searchButtonClass}
        >
          {isSearching ? "搜索中..." : "搜索动作"}
        </button>
      </div>

      {searchError ? (
        <div className="mt-2 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-3 py-2 text-sm text-rose-100">
          {searchError}
        </div>
      ) : null}

      {options.length > 0 ? (
        <div className="mt-3 flex flex-wrap gap-2">
          {options.map((option) => (
            <button
              key={option.exerciseId}
              type="button"
              disabled={locked}
              onClick={() => {
                onUpdateExercise(dayIndex, exercise.localId, {
                  exerciseId: option.exerciseId,
                  exerciseName: option.exerciseName
                });
                setKeyword(option.exerciseName);
                setOptions([]);
              }}
              className="rounded-full border border-white/10 bg-white/8 px-3 py-2 text-sm text-stone-100 transition hover:bg-amber-400 hover:text-stone-950"
            >
              {option.exerciseName}
            </button>
          ))}
        </div>
      ) : null}

      <FieldError message={fieldErrors[`${errorPrefix}.exerciseId`]} />

      <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <NumberField
          label="组数"
          value={exercise.targetSets}
          locked={locked}
          onChange={(value) =>
            onUpdateExercise(dayIndex, exercise.localId, { targetSets: value })
          }
          error={fieldErrors[`${errorPrefix}.targetSets`]}
        />
        <NumberField
          label="最小次数"
          value={exercise.targetRepsMin}
          locked={locked}
          onChange={(value) =>
            onUpdateExercise(dayIndex, exercise.localId, { targetRepsMin: value })
          }
          error={fieldErrors[`${errorPrefix}.targetRepsMin`]}
        />
        <NumberField
          label="最大次数"
          value={exercise.targetRepsMax}
          locked={locked}
          onChange={(value) =>
            onUpdateExercise(dayIndex, exercise.localId, { targetRepsMax: value })
          }
          error={fieldErrors[`${errorPrefix}.targetRepsMax`]}
        />
        <NumberField
          label="重量 kg"
          value={exercise.targetWeightKg}
          locked={locked}
          step="0.1"
          onChange={(value) =>
            onUpdateExercise(dayIndex, exercise.localId, { targetWeightKg: value })
          }
          error={fieldErrors[`${errorPrefix}.targetWeightKg`]}
        />
        <NumberField
          label="时长 秒"
          value={exercise.targetDurationSeconds}
          locked={locked}
          onChange={(value) =>
            onUpdateExercise(dayIndex, exercise.localId, {
              targetDurationSeconds: value
            })
          }
          error={fieldErrors[`${errorPrefix}.targetDurationSeconds`]}
        />
        <NumberField
          label="休息 秒"
          value={exercise.restSeconds}
          locked={locked}
          onChange={(value) =>
            onUpdateExercise(dayIndex, exercise.localId, { restSeconds: value })
          }
          error={fieldErrors[`${errorPrefix}.restSeconds`]}
        />
        <NumberField
          label="RPE"
          value={exercise.targetRpe}
          locked={locked}
          step="0.5"
          onChange={(value) =>
            onUpdateExercise(dayIndex, exercise.localId, { targetRpe: value })
          }
          error={fieldErrors[`${errorPrefix}.targetRpe`]}
        />
      </div>

      <div className="mt-4">
        <label className="space-y-2">
          <span className="text-sm text-stone-300">备注</span>
          <textarea
            value={exercise.note}
            disabled={locked}
            onChange={(event) =>
              onUpdateExercise(dayIndex, exercise.localId, {
                note: event.target.value
              })
            }
            className={`${inputClass} min-h-28 resize-y`}
            placeholder="例如：最后一组接近力竭"
          />
          <FieldError message={fieldErrors[`${errorPrefix}.note`]} />
        </label>
      </div>
    </div>
  );
}

function NumberField({
  label,
  value,
  locked,
  step = "1",
  error,
  onChange
}: {
  label: string;
  value: string;
  locked: boolean;
  step?: string;
  error?: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="space-y-2">
      <span className="text-sm text-stone-300">{label}</span>
      <input
        type="number"
        value={value}
        step={step}
        disabled={locked}
        onChange={(event) => onChange(event.target.value)}
        className={inputClass}
      />
      <FieldError message={error} />
    </label>
  );
}

function FieldError({ message }: { message?: string }) {
  if (!message) {
    return null;
  }

  return <p className="text-xs text-rose-200">{message}</p>;
}

const inputClass =
  "w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300/60 disabled:cursor-not-allowed disabled:opacity-60";

const primaryButtonClass =
  "rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60";

const secondaryButtonClass =
  "rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12 disabled:cursor-not-allowed disabled:opacity-50";

const searchButtonClass =
  "rounded-2xl border border-amber-300/20 bg-amber-300/10 px-5 py-3 text-sm font-medium text-amber-100 transition hover:bg-amber-300/20 disabled:cursor-not-allowed disabled:opacity-50";
