import {
  formatExerciseStructureType,
  formatExerciseType,
  formatMovementType
} from "../../exercise/lib/exercise-formatters";
import type {
  ExerciseCategoryOption,
  SystemExerciseOption
} from "../../exercise/types/exercise";

type ExercisePickerDialogProps = {
  open: boolean;
  mode: "append" | "replace";
  categories: ExerciseCategoryOption[];
  selectedCategoryCode: string;
  selectedMuscleId: number | null;
  keyword: string;
  results: SystemExerciseOption[];
  isLoadingFilters: boolean;
  isLoadingResults: boolean;
  errorMessage: string | null;
  onClose: () => void;
  onKeywordChange: (value: string) => void;
  onKeywordSubmit: () => void;
  onCategoryChange: (categoryCode: string) => void;
  onMuscleChange: (muscleId: number | null) => void;
  onSelectExercise: (option: SystemExerciseOption) => void;
};

export function ExercisePickerDialog({
  open,
  mode,
  categories,
  selectedCategoryCode,
  selectedMuscleId,
  keyword,
  results,
  isLoadingFilters,
  isLoadingResults,
  errorMessage,
  onClose,
  onKeywordChange,
  onKeywordSubmit,
  onCategoryChange,
  onMuscleChange,
  onSelectExercise
}: ExercisePickerDialogProps) {
  if (!open) {
    return null;
  }

  const selectedCategory =
    categories.find((category) => category.categoryCode === selectedCategoryCode) ?? null;
  const muscles = selectedCategory?.children ?? [];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 px-4 py-8 backdrop-blur">
      <section className="flex h-[min(84vh,780px)] w-full max-w-6xl flex-col overflow-hidden rounded-[32px] border border-white/10 bg-stone-950 shadow-2xl shadow-black/40">
        <div className="flex items-start justify-between gap-4 border-b border-white/10 px-6 py-5">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              Exercise Picker
            </p>
            <h2 className="mt-2 text-2xl font-semibold text-white">
              {mode === "append" ? "添加动作" : "更换动作"}
            </h2>
            <p className="mt-2 text-sm text-stone-400">
              先筛选并确认动作，再插入到当前训练日。
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/10"
          >
            关闭
          </button>
        </div>

        <div className="border-b border-white/10 px-6 py-4">
          <div className="grid gap-3 lg:grid-cols-[1fr_auto]">
            <label className="space-y-2">
              <span className="text-sm text-stone-300">搜索动作</span>
              <input
                value={keyword}
                onChange={(event) => onKeywordChange(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    event.preventDefault();
                    onKeywordSubmit();
                  }
                }}
                className={inputClass}
                placeholder="输入卧推、深蹲、跑步等关键词"
              />
            </label>
            <button
              type="button"
              onClick={onKeywordSubmit}
              disabled={isLoadingFilters || isLoadingResults || !selectedCategoryCode}
              className={primaryButtonClass}
            >
              {isLoadingResults ? "查询中..." : "立即查询"}
            </button>
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-2">
            <span className="text-xs uppercase tracking-[0.18em] text-stone-500">
              细分肌肉
            </span>
            <ChipButton
              active={selectedMuscleId === null}
              onClick={() => onMuscleChange(null)}
            >
              全部
            </ChipButton>
            {muscles.map((muscle) => (
              <ChipButton
                key={muscle.muscleId}
                active={selectedMuscleId === muscle.muscleId}
                onClick={() => onMuscleChange(muscle.muscleId)}
              >
                {muscle.muscleName}
              </ChipButton>
            ))}
          </div>
        </div>

        <div className="grid min-h-0 flex-1 lg:grid-cols-[220px_1fr]">
          <aside className="border-r border-white/10 bg-white/[0.03] p-4">
            <p className="px-2 text-xs uppercase tracking-[0.2em] text-stone-500">
              一级分类
            </p>
            <div className="mt-4 space-y-2">
              {isLoadingFilters ? (
                <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-stone-300">
                  正在加载分类...
                </div>
              ) : categories.length > 0 ? (
                categories.map((category) => (
                  <button
                    key={category.categoryCode}
                    type="button"
                    onClick={() => onCategoryChange(category.categoryCode)}
                    className={[
                      "w-full rounded-2xl px-4 py-3 text-left text-sm transition",
                      category.categoryCode === selectedCategoryCode
                        ? "bg-amber-400 text-stone-950"
                        : "bg-white/5 text-stone-200 hover:bg-white/10"
                    ].join(" ")}
                  >
                    {category.categoryName}
                  </button>
                ))
              ) : (
                <div className="rounded-2xl border border-dashed border-white/12 bg-white/5 px-4 py-4 text-sm text-stone-300">
                  当前没有可用分类。
                </div>
              )}
            </div>
          </aside>

          <div className="min-h-0 p-5">
            <div className="mb-4 flex items-center justify-between gap-3">
              <div>
                <p className="text-xs uppercase tracking-[0.18em] text-stone-500">
                  当前分类
                </p>
                <p className="mt-1 text-lg font-semibold text-white">
                  {selectedCategory?.categoryName ?? "未选择"}
                </p>
              </div>
              <p className="text-sm text-stone-400">
                {isLoadingResults ? "正在加载动作..." : `共 ${results.length} 个结果`}
              </p>
            </div>

            {errorMessage ? (
              <div className="mb-4 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
                {errorMessage}
              </div>
            ) : null}

            <div className="h-full overflow-y-auto pr-1">
              {isLoadingResults ? (
                <div className="rounded-3xl border border-white/10 bg-white/5 px-4 py-8 text-center text-stone-300">
                  正在查询动作列表...
                </div>
              ) : results.length > 0 ? (
                <div className="grid gap-3">
                  {results.map((option) => (
                    <button
                      key={option.exerciseId}
                      type="button"
                      onClick={() => onSelectExercise(option)}
                      className="rounded-3xl border border-white/10 bg-white/[0.04] p-4 text-left transition hover:border-amber-300/40 hover:bg-amber-300/10"
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

                      <div className="mt-3 space-y-2 text-xs text-stone-300">
                        {option.primaryMuscles.length > 0 ? (
                          <p>
                            主要肌群：
                            {option.primaryMuscles
                              .map((muscle) => muscle.muscleName)
                              .join("、")}
                          </p>
                        ) : null}
                        {option.secondaryMuscles.length > 0 ? (
                          <p>
                            次要肌群：
                            {option.secondaryMuscles
                              .map((muscle) => muscle.muscleName)
                              .join("、")}
                          </p>
                        ) : null}
                        {option.equipmentNames.length > 0 ? (
                          <p>{`器械：${option.equipmentNames.join("、")}`}</p>
                        ) : null}
                      </div>
                    </button>
                  ))}
                </div>
              ) : (
                <div className="rounded-3xl border border-dashed border-white/12 bg-white/5 px-4 py-8 text-center text-stone-300">
                  当前条件下没有找到可用动作，试试切换分类、细分肌肉或关键词。
                </div>
              )}
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

function Tag({ children }: { children: string }) {
  return (
    <span className="rounded-full border border-white/10 bg-white/8 px-2.5 py-1">
      {children}
    </span>
  );
}

function ChipButton({
  active,
  onClick,
  children
}: {
  active: boolean;
  onClick: () => void;
  children: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={[
        "rounded-full px-3 py-1.5 text-xs transition",
        active
          ? "bg-amber-400 text-stone-950"
          : "border border-white/10 bg-white/6 text-stone-200 hover:bg-white/12"
      ].join(" ")}
    >
      {children}
    </button>
  );
}

const inputClass =
  "w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300/60";

const primaryButtonClass =
  "rounded-2xl bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60";
