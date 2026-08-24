import { useState } from "react";
import { formatExerciseStructureType } from "../../exercise/lib/exercise-formatters";
import type { ExerciseCardMeta } from "../../exercise/types/exercise";
import { ExerciseItemEditor } from "./ExerciseItemEditor";
import type {
  CycleTemplateFieldErrors,
  EditorExerciseForm,
  EditorItemForm,
  EditorMetricForm
} from "../types/cycle-template";

type ExerciseStructureEditorProps = {
  dayIndex: number;
  exerciseIndex: number;
  exercise: EditorExerciseForm;
  exerciseMeta?: ExerciseCardMeta;
  locked: boolean;
  fieldErrors: CycleTemplateFieldErrors;
  expanded?: boolean;
  onExpandedChange?: (expanded: boolean) => void;
  onRequestReplace: () => void;
  onUpdateExercise: (patch: Partial<EditorExerciseForm>) => void;
  onRemoveExercise: () => void;
  onMoveExercise: (direction: -1 | 1) => void;
  onAddItem: () => void;
  onUpdateItem: (itemLocalId: string, patch: Partial<EditorItemForm>) => void;
  onRemoveItem: (itemLocalId: string) => void;
  onMoveItem: (itemLocalId: string, direction: -1 | 1) => void;
  onAddMetric: (itemLocalId: string) => void;
  onUpdateMetric: (
    itemLocalId: string,
    metricLocalId: string,
    patch: Partial<EditorMetricForm>
  ) => void;
  onRemoveMetric: (itemLocalId: string, metricLocalId: string) => void;
  onMoveMetric: (
    itemLocalId: string,
    metricLocalId: string,
    direction: -1 | 1
  ) => void;
};

export function ExerciseStructureEditor({
  dayIndex,
  exerciseIndex,
  exercise,
  exerciseMeta,
  locked,
  fieldErrors,
  expanded,
  onExpandedChange,
  onRequestReplace,
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
}: ExerciseStructureEditorProps) {
  const [localExpanded, setLocalExpanded] = useState(false);
  const isExpanded = expanded ?? localExpanded;
  const errorPrefix = `day.${dayIndex}.exercise.${exerciseIndex}`;
  const errorCount = countExerciseErrors(fieldErrors, errorPrefix);

  function handleToggleExpanded() {
    const nextExpanded = !isExpanded;
    onExpandedChange?.(nextExpanded);
    setLocalExpanded(nextExpanded);
  }

  return (
    <div
      id={`exercise-${exercise.localId}`}
      className={`rounded-3xl border border-white/10 bg-stone-950/50 p-4 [content-visibility:auto] ${
        isExpanded
          ? "[contain-intrinsic-size:auto_480px]"
          : "[contain-intrinsic-size:auto_120px]"
      }`}
    >
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="space-y-2">
          <p className="text-sm uppercase tracking-[0.2em] text-amber-300">
            动作 {exerciseIndex + 1}
          </p>
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-lg font-semibold text-white">
              {exercise.exerciseName || "未选择动作"}
            </p>
            {!isExpanded && errorCount > 0 ? (
              <span className="rounded-full border border-rose-300/30 bg-rose-400/10 px-2.5 py-1 text-xs text-rose-200">
                {errorCount} 项待修正
              </span>
            ) : null}
          </div>
          {exercise.structureType ? (
            <div className="flex flex-wrap gap-2 text-xs text-stone-300">
              <Tag>{formatExerciseStructureType(exercise.structureType)}</Tag>
              {exerciseMeta?.primaryMuscles.length ? (
                <Tag>{`主要肌群：${exerciseMeta.primaryMuscles.join("、")}`}</Tag>
              ) : null}
              {exerciseMeta?.equipmentNames.length ? (
                <Tag>{`器械：${exerciseMeta.equipmentNames.join("、")}`}</Tag>
              ) : null}
            </div>
          ) : null}
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            aria-expanded={isExpanded}
            onClick={handleToggleExpanded}
            className={secondaryButtonClass}
          >
            {isExpanded ? "收起" : "展开"}
          </button>
          <button
            type="button"
            disabled={locked}
            onClick={onRequestReplace}
            className={secondaryButtonClass}
          >
            更换动作
          </button>
          <button
            type="button"
            disabled={locked}
            onClick={() => onMoveExercise(-1)}
            className={secondaryButtonClass}
          >
            上移
          </button>
          <button
            type="button"
            disabled={locked}
            onClick={() => onMoveExercise(1)}
            className={secondaryButtonClass}
          >
            下移
          </button>
          <button
            type="button"
            disabled={locked}
            onClick={onRemoveExercise}
            className={dangerButtonClass}
          >
            删除动作
          </button>
        </div>
      </div>

      {isExpanded ? (
        <>
          <div className="mt-4 space-y-2">
            <span className="text-sm text-stone-300">动作备注</span>
            <label className="space-y-2">
              <textarea
                value={exercise.note}
                disabled={locked}
                onChange={(event) => onUpdateExercise({ note: event.target.value })}
                className={`${inputClass} min-h-28 resize-y`}
                placeholder="例如：最后一组接近力竭"
              />
              <FieldError message={fieldErrors[`${errorPrefix}.note`]} />
            </label>
          </div>

          <FieldError message={fieldErrors[`${errorPrefix}.exerciseId`]} />
          <FieldError message={fieldErrors[`${errorPrefix}.structureType`]} />
          <FieldError message={fieldErrors[`${errorPrefix}.items`]} />

          <div className="mt-5 space-y-4">
            {exercise.items.map((item, itemIndex) => {
              const itemErrorPrefix = `${errorPrefix}.item.${itemIndex}`;
              return (
                <ExerciseItemEditor
                  key={item.localId}
                  item={item}
                  itemIndex={itemIndex}
                  structureType={exercise.structureType}
                  locked={locked}
                  fieldErrors={fieldErrors}
                  errorPrefix={itemErrorPrefix}
                  onChange={(patch) => onUpdateItem(item.localId, patch)}
                  onAddMetric={() => onAddMetric(item.localId)}
                  onUpdateMetric={(metricLocalId, patch) =>
                    onUpdateMetric(item.localId, metricLocalId, patch)
                  }
                  onRemoveMetric={(metricLocalId) =>
                    onRemoveMetric(item.localId, metricLocalId)
                  }
                  onMoveMetric={(metricLocalId, direction) =>
                    onMoveMetric(item.localId, metricLocalId, direction)
                  }
                  onMoveItem={(direction) => onMoveItem(item.localId, direction)}
                  onRemoveItem={() => onRemoveItem(item.localId)}
                />
              );
            })}
          </div>

          {exercise.structureType === "set_based" ? (
            <div className="mt-4">
              <button
                type="button"
                disabled={locked}
                onClick={onAddItem}
                className={secondaryButtonClass}
              >
                新增一组
              </button>
            </div>
          ) : null}
        </>
      ) : null}
    </div>
  );
}

function countExerciseErrors(fieldErrors: CycleTemplateFieldErrors, errorPrefix: string) {
  const prefix = `${errorPrefix}.`;
  return Object.keys(fieldErrors).filter((key) => {
    return key.startsWith(prefix) && Boolean(fieldErrors[key]);
  }).length;
}

function FieldError({ message }: { message?: string }) {
  if (!message) {
    return null;
  }

  return <p className="mt-2 text-xs text-rose-200">{message}</p>;
}

function Tag({ children }: { children: string }) {
  return (
    <span className="rounded-full border border-white/10 bg-white/8 px-2.5 py-1">
      {children}
    </span>
  );
}

const inputClass =
  "w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300/60 disabled:cursor-not-allowed disabled:opacity-60";

const secondaryButtonClass =
  "rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12 disabled:cursor-not-allowed disabled:opacity-50";

const dangerButtonClass =
  "rounded-full border border-rose-300/25 px-4 py-2 text-sm text-rose-100 transition hover:bg-rose-400/10 disabled:cursor-not-allowed disabled:opacity-50";
