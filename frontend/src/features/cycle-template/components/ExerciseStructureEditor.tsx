import { useState } from "react";
import type { ReactNode } from "react";
import { searchSystemExercises } from "../../exercise/api/exercise";
import { getExerciseErrorMessage } from "../../exercise/lib/exercise-enums";
import {
  formatExerciseStructureType,
  formatExerciseType,
  formatMovementType
} from "../../exercise/lib/exercise-formatters";
import type { SystemExerciseOption } from "../../exercise/types/exercise";
import { ExerciseItemEditor } from "./ExerciseItemEditor";
import type {
  CycleTemplateFieldErrors,
  EditorExerciseForm,
  EditorItemForm,
  EditorMetricForm
} from "../types/cycle-template";

type ExerciseStructureEditorProps = {
  accessToken: string;
  dayIndex: number;
  exerciseIndex: number;
  exercise: EditorExerciseForm;
  locked: boolean;
  fieldErrors: CycleTemplateFieldErrors;
  onSelectSystemExercise: (option: SystemExerciseOption) => void;
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
  accessToken,
  dayIndex,
  exerciseIndex,
  exercise,
  locked,
  fieldErrors,
  onSelectSystemExercise,
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
  const [keyword, setKeyword] = useState(exercise.exerciseName);
  const [options, setOptions] = useState<SystemExerciseOption[]>([]);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const errorPrefix = `day.${dayIndex}.exercise.${exerciseIndex}`;

  async function handleSearch() {
    const nextKeyword = keyword.trim();
    setSearchError(null);
    setHasSearched(true);

    if (!nextKeyword) {
      setOptions([]);
      setSearchError("请输入动作关键词后再搜索。");
      return;
    }

    setIsSearching(true);
    try {
      const response = await searchSystemExercises(accessToken, {
        keyword: nextKeyword,
        page: 1,
        pageSize: 20
      });
      setOptions(response.records ?? []);
    } catch (error) {
      setOptions([]);
      setSearchError(getExerciseErrorMessage(error, "动作搜索失败，请稍后再试。"));
    } finally {
      setIsSearching(false);
    }
  }

  function handleSelectOption(option: SystemExerciseOption) {
    onSelectSystemExercise(option);
    setKeyword(option.exerciseName);
    setOptions([]);
    setHasSearched(false);
    setSearchError(null);
  }

  return (
    <div
      draggable={!locked}
      onDragStart={(event) => {
        event.dataTransfer.setData("text/plain", exercise.localId);
      }}
      className="rounded-3xl border border-white/10 bg-stone-950/50 p-4"
    >
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.2em] text-amber-300">
            动作 {exerciseIndex + 1}
          </p>
          <p className="mt-1 text-lg font-semibold text-white">
            {exercise.exerciseName || "还没有选择动作"}
          </p>
          {exercise.structureType ? (
            <p className="mt-1 text-xs text-stone-400">
              结构类型：{formatExerciseStructureType(exercise.structureType)}
            </p>
          ) : null}
        </div>

        <div className="flex flex-wrap gap-2">
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

      <div className="mt-4 rounded-3xl border border-white/10 bg-black/20 p-4">
        <div className="grid gap-3 lg:grid-cols-[1fr_auto]">
          <label className="space-y-2">
            <span className="text-sm text-stone-300">搜索系统动作</span>
            <input
              value={keyword}
              disabled={locked}
              onChange={(event) => setKeyword(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  void handleSearch();
                }
              }}
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
            {isSearching ? "搜索中..." : "查询动作"}
          </button>
        </div>

        {searchError ? (
          <div className="mt-3 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-3 py-2 text-sm text-rose-100">
            {searchError}
          </div>
        ) : null}

        {options.length > 0 ? (
          <div className="mt-4 space-y-3">
            <div className="flex items-center justify-between gap-3">
              <p className="text-xs uppercase tracking-[0.18em] text-stone-400">
                共找到 {options.length} 个候选动作
              </p>
            </div>
            <div className="grid gap-3">
              {options.map((option) => (
                <button
                  key={option.exerciseId}
                  type="button"
                  disabled={locked}
                  onClick={() => handleSelectOption(option)}
                  className="rounded-3xl border border-white/10 bg-white/6 p-4 text-left transition hover:border-amber-300/40 hover:bg-amber-300/10 disabled:opacity-60"
                >
                  <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                    <div>
                      <p className="text-base font-semibold text-white">
                        {option.exerciseName}
                      </p>
                      <div className="mt-2 flex flex-wrap gap-2 text-xs text-stone-300">
                        <Tag>{formatExerciseStructureType(option.defaultStructureType)}</Tag>
                        <Tag>{formatExerciseType(option.exerciseType)}</Tag>
                        {option.movementType ? (
                          <Tag>{formatMovementType(option.movementType)}</Tag>
                        ) : null}
                        {option.defaultUnit ? <Tag>{option.defaultUnit}</Tag> : null}
                      </div>
                    </div>
                    <span className="text-xs text-amber-200">点击选择</span>
                  </div>

                  {(option.primaryMuscles.length > 0 ||
                    option.secondaryMuscles.length > 0 ||
                    option.equipmentNames.length > 0) && (
                    <div className="mt-3 space-y-2 text-xs text-stone-300">
                      {option.primaryMuscles.length > 0 ? (
                        <p>主要肌群：{option.primaryMuscles.join("、")}</p>
                      ) : null}
                      {option.secondaryMuscles.length > 0 ? (
                        <p>次要肌群：{option.secondaryMuscles.join("、")}</p>
                      ) : null}
                      {option.equipmentNames.length > 0 ? (
                        <p>器械：{option.equipmentNames.join("、")}</p>
                      ) : null}
                    </div>
                  )}
                </button>
              ))}
            </div>
          </div>
        ) : null}

        {hasSearched && !searchError && !isSearching && options.length === 0 ? (
          <div className="mt-4 rounded-2xl border border-dashed border-white/12 bg-white/5 px-4 py-5 text-sm text-stone-300">
            没有找到匹配的系统动作，请换一个关键词再试。
          </div>
        ) : null}
      </div>

      <FieldError message={fieldErrors[`${errorPrefix}.exerciseId`]} />
      <FieldError message={fieldErrors[`${errorPrefix}.structureType`]} />
      <FieldError message={fieldErrors[`${errorPrefix}.items`]} />

      <div className="mt-4">
        <label className="space-y-2">
          <span className="text-sm text-stone-300">动作备注</span>
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
    </div>
  );
}

function FieldError({ message }: { message?: string }) {
  if (!message) {
    return null;
  }

  return <p className="mt-2 text-xs text-rose-200">{message}</p>;
}

function Tag({ children }: { children: ReactNode }) {
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

const searchButtonClass =
  "rounded-2xl border border-amber-300/20 bg-amber-300/10 px-5 py-3 text-sm font-medium text-amber-100 transition hover:bg-amber-300/20 disabled:cursor-not-allowed disabled:opacity-50";
