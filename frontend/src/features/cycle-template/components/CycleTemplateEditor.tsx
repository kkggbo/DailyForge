import { useEffect, useMemo, useState } from "react";
import {
  getSystemExerciseDetail,
  getSystemExerciseFilterOptions,
  searchSystemExercises
} from "../../exercise/api/exercise";
import { getExerciseErrorMessage } from "../../exercise/lib/exercise-enums";
import {
  mapSystemExerciseDetailToCardMeta,
  mapSystemExerciseOptionToCardMeta
} from "../../exercise/lib/exercise-mappers";
import type {
  ExerciseCardMeta,
  ExerciseCategoryOption,
  SystemExerciseOption
} from "../../exercise/types/exercise";
import { ExercisePickerDialog } from "./ExercisePickerDialog";
import { ExerciseStructureEditor } from "./ExerciseStructureEditor";
import type {
  CycleTemplateEditorForm,
  CycleTemplateFieldErrors,
  EditorExerciseForm,
  EditorItemForm,
  EditorMetricForm
} from "../types/cycle-template";

type CycleTemplateEditorProps = {
  accessToken: string;
  form: CycleTemplateEditorForm;
  fieldErrors: CycleTemplateFieldErrors;
  isDirty: boolean;
  canUndo: boolean;
  isSubmitting: boolean;
  submitLabel: string;
  selectedDayIndex: number;
  onSelectedDayIndexChange: (dayIndex: number) => void;
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
  onAppendExerciseFromSystemOption: (
    dayIndex: number,
    option: SystemExerciseOption
  ) => void;
  onReplaceExerciseFromSystemOption: (
    dayIndex: number,
    exerciseLocalId: string,
    option: SystemExerciseOption
  ) => void;
  onUpdateExercise: (
    dayIndex: number,
    localId: string,
    patch: Partial<EditorExerciseForm>
  ) => void;
  onRemoveExercise: (dayIndex: number, localId: string) => void;
  onMoveExercise: (dayIndex: number, localId: string, direction: -1 | 1) => void;
  onAddItem: (dayIndex: number, exerciseLocalId: string) => void;
  onUpdateItem: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    patch: Partial<EditorItemForm>
  ) => void;
  onRemoveItem: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string
  ) => void;
  onMoveItem: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    direction: -1 | 1
  ) => void;
  onAddMetric: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string
  ) => void;
  onUpdateMetric: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    metricLocalId: string,
    patch: Partial<EditorMetricForm>
  ) => void;
  onRemoveMetric: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    metricLocalId: string
  ) => void;
  onMoveMetric: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    metricLocalId: string,
    direction: -1 | 1
  ) => void;
  onUndo: () => void;
  onReset: () => void;
  onSubmit: () => void;
};

type PickerTarget =
  | {
      mode: "append";
      dayIndex: number;
    }
  | {
      mode: "replace";
      dayIndex: number;
      exerciseLocalId: string;
    }
  | null;

export function CycleTemplateEditor({
  accessToken,
  form,
  fieldErrors,
  isDirty,
  canUndo,
  isSubmitting,
  submitLabel,
  selectedDayIndex,
  onSelectedDayIndexChange,
  lockedBeforeDayIndex = 1,
  disableCycleLength = false,
  cycleLengthMode = "input",
  allowEmptyCycleLengthOption = false,
  onRootFieldChange,
  onDayChange,
  onAppendExerciseFromSystemOption,
  onReplaceExerciseFromSystemOption,
  onUpdateExercise,
  onRemoveExercise,
  onMoveExercise,
  onAddItem,
  onUpdateItem,
  onRemoveItem,
  onMoveItem,
  onAddMetric,
  onUpdateMetric,
  onRemoveMetric,
  onMoveMetric,
  onUndo,
  onReset,
  onSubmit
}: CycleTemplateEditorProps) {
  const activeDay =
    form.days.find((day) => day.dayIndex === selectedDayIndex) ?? form.days[0];
  const [pickerTarget, setPickerTarget] = useState<PickerTarget>(null);
  const [pickerCategories, setPickerCategories] = useState<ExerciseCategoryOption[]>([]);
  const [selectedCategoryCode, setSelectedCategoryCode] = useState("");
  const [selectedMuscleId, setSelectedMuscleId] = useState<number | null>(null);
  const [pickerKeywordInput, setPickerKeywordInput] = useState("");
  const [pickerKeywordQuery, setPickerKeywordQuery] = useState("");
  const [pickerResults, setPickerResults] = useState<SystemExerciseOption[]>([]);
  const [pickerError, setPickerError] = useState<string | null>(null);
  const [isLoadingFilterOptions, setIsLoadingFilterOptions] = useState(false);
  const [isLoadingResults, setIsLoadingResults] = useState(false);
  const [manualSearchNonce, setManualSearchNonce] = useState(0);
  const [exerciseMetaById, setExerciseMetaById] = useState<Record<number, ExerciseCardMeta>>(
    {}
  );

  const exerciseIdsInForm = useMemo(
    () =>
      Array.from(
        new Set(
          form.days
            .flatMap((day) => day.exercises.map((exercise) => exercise.exerciseId))
            .filter((exerciseId): exerciseId is number => Number.isFinite(exerciseId))
        )
      ),
    [form.days]
  );

  useEffect(() => {
    if (!pickerTarget) {
      return;
    }

    setPickerKeywordInput("");
    setPickerKeywordQuery("");
    setSelectedMuscleId(null);
    setPickerResults([]);
    setPickerError(null);

    let cancelled = false;
    setIsLoadingFilterOptions(true);

    void getSystemExerciseFilterOptions(accessToken)
      .then((response) => {
        if (cancelled) {
          return;
        }

        const categories = response.categories ?? [];
        setPickerCategories(categories);
        setSelectedCategoryCode(categories[0]?.categoryCode ?? "");
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }

        setPickerCategories([]);
        setSelectedCategoryCode("");
        setPickerError(getExerciseErrorMessage(error, "加载动作分类失败，请稍后再试。"));
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoadingFilterOptions(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, pickerTarget]);

  useEffect(() => {
    if (!pickerTarget) {
      return;
    }

    const timer = window.setTimeout(() => {
      setPickerKeywordQuery(pickerKeywordInput.trim());
    }, 300);

    return () => window.clearTimeout(timer);
  }, [pickerKeywordInput, pickerTarget]);

  useEffect(() => {
    if (!pickerTarget || !selectedCategoryCode) {
      return;
    }

    let cancelled = false;
    setIsLoadingResults(true);
    setPickerError(null);

    void searchSystemExercises(accessToken, {
      categoryCode: selectedCategoryCode,
      muscleId: selectedMuscleId ?? undefined,
      keyword: pickerKeywordQuery || undefined,
      page: 1,
      pageSize: 20
    })
      .then((response) => {
        if (cancelled) {
          return;
        }

        setPickerResults(response.records ?? []);
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }

        setPickerResults([]);
        setPickerError(getExerciseErrorMessage(error, "加载动作列表失败，请稍后再试。"));
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoadingResults(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [
    accessToken,
    manualSearchNonce,
    pickerKeywordQuery,
    pickerTarget,
    selectedCategoryCode,
    selectedMuscleId
  ]);

  useEffect(() => {
    const missingExerciseIds = exerciseIdsInForm.filter(
      (exerciseId) => !exerciseMetaById[exerciseId]
    );

    if (!missingExerciseIds.length) {
      return;
    }

    let cancelled = false;

    void Promise.allSettled(
      missingExerciseIds.map((exerciseId) =>
        getSystemExerciseDetail(accessToken, exerciseId)
      )
    ).then((results) => {
      if (cancelled) {
        return;
      }

      const nextMetaEntries = results.flatMap((result) => {
        if (result.status !== "fulfilled") {
          return [];
        }

        const detail = result.value;
        return [[detail.exerciseId, mapSystemExerciseDetailToCardMeta(detail)] as const];
      });

      if (!nextMetaEntries.length) {
        return;
      }

      setExerciseMetaById((current) => ({
        ...current,
        ...Object.fromEntries(nextMetaEntries)
      }));
    });

    return () => {
      cancelled = true;
    };
  }, [accessToken, exerciseIdsInForm, exerciseMetaById]);

  function openAppendPicker(dayIndex: number) {
    setPickerTarget({
      mode: "append",
      dayIndex
    });
  }

  function openReplacePicker(dayIndex: number, exerciseLocalId: string) {
    setPickerTarget({
      mode: "replace",
      dayIndex,
      exerciseLocalId
    });
  }

  function closePicker() {
    setPickerTarget(null);
    setPickerError(null);
    setPickerResults([]);
  }

  function handleCategoryChange(categoryCode: string) {
    setSelectedCategoryCode(categoryCode);
    setSelectedMuscleId(null);
  }

  function handleKeywordSubmit() {
    setPickerKeywordQuery(pickerKeywordInput.trim());
    setManualSearchNonce((current) => current + 1);
  }

  function handleSelectExercise(option: SystemExerciseOption) {
    setExerciseMetaById((current) => ({
      ...current,
      [option.exerciseId]: mapSystemExerciseOptionToCardMeta(option)
    }));

    if (!pickerTarget) {
      return;
    }

    if (pickerTarget.mode === "append") {
      onAppendExerciseFromSystemOption(pickerTarget.dayIndex, option);
    } else {
      onReplaceExerciseFromSystemOption(
        pickerTarget.dayIndex,
        pickerTarget.exerciseLocalId,
        option
      );
    }

    closePicker();
  }

  return (
    <div className="space-y-6">
      <section className="rounded-[28px] border border-white/10 bg-white/8 p-5 shadow-2xl shadow-black/20 backdrop-blur">
        <div className="grid gap-4 lg:grid-cols-[1.4fr_0.8fr_0.8fr]">
          <label className="space-y-2">
            <span className="text-sm text-stone-300">模板名称</span>
            <input
              value={form.templateName}
              onChange={(event) => onRootFieldChange("templateName", event.target.value)}
              className={inputClass}
              placeholder="例如：Push Pull Legs"
            />
            <FieldError message={fieldErrors.templateName} />
          </label>

          <label className="space-y-2">
            <span className="text-sm text-stone-300">训练目标</span>
            <input
              value={form.goalType}
              onChange={(event) => onRootFieldChange("goalType", event.target.value)}
              className={inputClass}
              placeholder="例如：muscle_gain"
            />
            <FieldError message={fieldErrors.goalType} />
          </label>

          <label className="space-y-2">
            <span className="text-sm text-stone-300">周期长度</span>
            {cycleLengthMode === "select" ? (
              <select
                value={form.cycleLength}
                disabled={disableCycleLength}
                onChange={(event) => onRootFieldChange("cycleLength", event.target.value)}
                className={inputClass}
              >
                {allowEmptyCycleLengthOption ? <option value="">未设置</option> : null}
                {Array.from({ length: 7 }, (_, index) => String(index + 1)).map((value) => (
                  <option key={value} value={value}>
                    {value} 天
                  </option>
                ))}
              </select>
            ) : (
              <input
                type="number"
                min={1}
                max={7}
                step={1}
                value={form.cycleLength}
                disabled={disableCycleLength}
                onChange={(event) => onRootFieldChange("cycleLength", event.target.value)}
                className={inputClass}
                placeholder="1-7"
              />
            )}
            <FieldError message={fieldErrors.cycleLength} />
          </label>
        </div>

        <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-stone-400">
            {isDirty ? "有未保存修改" : "当前内容已保存或尚未修改"}
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
              onClick={() => onSelectedDayIndexChange(day.dayIndex)}
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
          <CycleTemplateDayEditor
            day={activeDay}
            exerciseMetaById={exerciseMetaById}
            lockedBeforeDayIndex={lockedBeforeDayIndex}
            fieldErrors={fieldErrors}
            onDayChange={onDayChange}
            onOpenAppendPicker={openAppendPicker}
            onOpenReplacePicker={openReplacePicker}
            onUpdateExercise={onUpdateExercise}
            onRemoveExercise={onRemoveExercise}
            onMoveExercise={onMoveExercise}
            onAddItem={onAddItem}
            onUpdateItem={onUpdateItem}
            onRemoveItem={onRemoveItem}
            onMoveItem={onMoveItem}
            onAddMetric={onAddMetric}
            onUpdateMetric={onUpdateMetric}
            onRemoveMetric={onRemoveMetric}
            onMoveMetric={onMoveMetric}
          />
        ) : null}
      </section>

      <ExercisePickerDialog
        open={Boolean(pickerTarget)}
        mode={pickerTarget?.mode ?? "append"}
        categories={pickerCategories}
        selectedCategoryCode={selectedCategoryCode}
        selectedMuscleId={selectedMuscleId}
        keyword={pickerKeywordInput}
        results={pickerResults}
        isLoadingFilters={isLoadingFilterOptions}
        isLoadingResults={isLoadingResults}
        errorMessage={pickerError}
        onClose={closePicker}
        onKeywordChange={setPickerKeywordInput}
        onKeywordSubmit={handleKeywordSubmit}
        onCategoryChange={handleCategoryChange}
        onMuscleChange={setSelectedMuscleId}
        onSelectExercise={handleSelectExercise}
      />
    </div>
  );
}

function CycleTemplateDayEditor({
  day,
  exerciseMetaById,
  lockedBeforeDayIndex,
  fieldErrors,
  onDayChange,
  onOpenAppendPicker,
  onOpenReplacePicker,
  onUpdateExercise,
  onRemoveExercise,
  onMoveExercise,
  onAddItem,
  onUpdateItem,
  onRemoveItem,
  onMoveItem,
  onAddMetric,
  onUpdateMetric,
  onRemoveMetric,
  onMoveMetric
}: {
  day: CycleTemplateEditorForm["days"][number];
  exerciseMetaById: Record<number, ExerciseCardMeta>;
  lockedBeforeDayIndex: number;
  fieldErrors: CycleTemplateFieldErrors;
  onDayChange: (
    dayIndex: number,
    patch: Partial<CycleTemplateEditorForm["days"][number]>
  ) => void;
  onOpenAppendPicker: (dayIndex: number) => void;
  onOpenReplacePicker: (dayIndex: number, exerciseLocalId: string) => void;
  onUpdateExercise: (
    dayIndex: number,
    localId: string,
    patch: Partial<EditorExerciseForm>
  ) => void;
  onRemoveExercise: (dayIndex: number, localId: string) => void;
  onMoveExercise: (dayIndex: number, localId: string, direction: -1 | 1) => void;
  onAddItem: (dayIndex: number, exerciseLocalId: string) => void;
  onUpdateItem: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    patch: Partial<EditorItemForm>
  ) => void;
  onRemoveItem: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string
  ) => void;
  onMoveItem: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    direction: -1 | 1
  ) => void;
  onAddMetric: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string
  ) => void;
  onUpdateMetric: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    metricLocalId: string,
    patch: Partial<EditorMetricForm>
  ) => void;
  onRemoveMetric: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    metricLocalId: string
  ) => void;
  onMoveMetric: (
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    metricLocalId: string,
    direction: -1 | 1
  ) => void;
}) {
  const isDayLocked = day.dayIndex < lockedBeforeDayIndex;

  return (
    <div className="mt-6 space-y-5">
      <div className="grid gap-4 lg:grid-cols-[1fr_auto] lg:items-end">
        <label className="space-y-2">
          <span className="text-sm text-stone-300">训练日名称</span>
          <input
            value={day.dayName}
            disabled={isDayLocked}
            onChange={(event) => onDayChange(day.dayIndex, { dayName: event.target.value })}
            className={inputClass}
            placeholder={`Day ${day.dayIndex}`}
          />
          <FieldError message={fieldErrors[`day.${day.dayIndex - 1}.dayName`]} />
        </label>

        <button
          type="button"
          disabled={isDayLocked}
          onClick={() => onOpenAppendPicker(day.dayIndex)}
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

      {day.exercises.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-white/12 bg-white/5 p-8 text-center text-stone-300">
          当前训练日还没有动作，保存后会按休息日处理。
        </div>
      ) : (
        <div className="space-y-4">
          {day.exercises.map((exercise, exerciseIndex) => (
            <ExerciseStructureEditor
              key={exercise.localId}
              dayIndex={day.dayIndex - 1}
              exerciseIndex={exerciseIndex}
              exercise={exercise}
              exerciseMeta={exercise.exerciseId ? exerciseMetaById[exercise.exerciseId] : undefined}
              locked={isDayLocked}
              fieldErrors={fieldErrors}
              onRequestReplace={() => onOpenReplacePicker(day.dayIndex, exercise.localId)}
              onUpdateExercise={(patch) =>
                onUpdateExercise(day.dayIndex, exercise.localId, patch)
              }
              onRemoveExercise={() => onRemoveExercise(day.dayIndex, exercise.localId)}
              onMoveExercise={(direction) =>
                onMoveExercise(day.dayIndex, exercise.localId, direction)
              }
              onAddItem={() => onAddItem(day.dayIndex, exercise.localId)}
              onUpdateItem={(itemLocalId, patch) =>
                onUpdateItem(day.dayIndex, exercise.localId, itemLocalId, patch)
              }
              onRemoveItem={(itemLocalId) =>
                onRemoveItem(day.dayIndex, exercise.localId, itemLocalId)
              }
              onMoveItem={(itemLocalId, direction) =>
                onMoveItem(day.dayIndex, exercise.localId, itemLocalId, direction)
              }
              onAddMetric={(itemLocalId) =>
                onAddMetric(day.dayIndex, exercise.localId, itemLocalId)
              }
              onUpdateMetric={(itemLocalId, metricLocalId, patch) =>
                onUpdateMetric(
                  day.dayIndex,
                  exercise.localId,
                  itemLocalId,
                  metricLocalId,
                  patch
                )
              }
              onRemoveMetric={(itemLocalId, metricLocalId) =>
                onRemoveMetric(day.dayIndex, exercise.localId, itemLocalId, metricLocalId)
              }
              onMoveMetric={(itemLocalId, metricLocalId, direction) =>
                onMoveMetric(
                  day.dayIndex,
                  exercise.localId,
                  itemLocalId,
                  metricLocalId,
                  direction
                )
              }
            />
          ))}
        </div>
      )}
    </div>
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
