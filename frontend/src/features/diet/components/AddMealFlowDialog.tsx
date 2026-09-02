import { useCallback, useEffect, useRef, useState } from "react";
import { useAuth } from "../../../app/providers/AuthProvider";
import {
  addFavorite,
  createMealLog,
  removeFavorite,
  searchFoods
} from "../api/diet";
import { FoodSourceTag } from "./FoodSourceTag";
import { UploadFoodDialog } from "./UploadFoodDialog";
import { foodFilterOptions, getMealTypeLabel } from "../lib/diet-enums";
import { getDietErrorMessage } from "../lib/diet-formatters";
import type { FoodFilter, FoodItem, MealType } from "../types/diet";

const PAGE_SIZE = 20;
const MAX_GRAMS = 5000;

type AddMealFlowDialogProps = {
  open: boolean;
  date: string;
  mealType: MealType;
  onClose: () => void;
  onSaved: () => Promise<void>;
};

export function AddMealFlowDialog({
  open,
  date,
  mealType,
  onClose,
  onSaved
}: AddMealFlowDialogProps) {
  const { accessToken } = useAuth();
  const [step, setStep] = useState<1 | 2>(1);

  // 第一步：选择食物
  const [keyword, setKeyword] = useState("");
  const [filter, setFilter] = useState<FoodFilter>("all");
  const [foods, setFoods] = useState<FoodItem[]>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);
  const [isInitialLoading, setIsInitialLoading] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const loadMoreInFlight = useRef(false);
  const [selectedFood, setSelectedFood] = useState<FoodItem | null>(null);

  // 第二步：填克数
  const [gramsText, setGramsText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const [uploadOpen, setUploadOpen] = useState(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    setStep(1);
    setSelectedFood(null);
    setGramsText("");
    setSubmitError(null);
    // 每次打开重置搜索/筛选，避免残留上次状态引起困惑
    setKeyword("");
    setFilter("all");
    setFoods([]);
    setPage(1);
    setHasMore(false);
  }, [open, date, mealType]);

  const loadPage = useCallback(
    async (targetPage: number, append: boolean) => {
      if (!accessToken) {
        return;
      }
      if (append && loadMoreInFlight.current) {
        // 同一帧内的快速滚动重复触发：丢弃后一次，避免重复追加
        return;
      }
      const token = accessToken;
      if (append) {
        loadMoreInFlight.current = true;
        setIsLoadingMore(true);
      } else {
        setIsInitialLoading(true);
      }
      setListError(null);
      try {
        const response = await searchFoods(token, {
          keyword,
          filter,
          page: targetPage,
          pageSize: PAGE_SIZE
        });
        setFoods((previous) =>
          append ? [...previous, ...response.foods] : response.foods
        );
        setPage(targetPage);
        setHasMore(response.hasMore);
      } catch (error) {
        setListError(getDietErrorMessage(error, "加载食物库失败，请稍后再试。"));
      } finally {
        loadMoreInFlight.current = false;
        setIsInitialLoading(false);
        setIsLoadingMore(false);
      }
    },
    [accessToken, keyword, filter]
  );

  useEffect(() => {
    if (!open) {
      return;
    }
    void loadPage(1, false);
  }, [open, accessToken, keyword, filter, loadPage]);

  function handleScroll() {
    const el = scrollRef.current;
    if (!el || !hasMore || isLoadingMore || isInitialLoading) {
      return;
    }
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 40) {
      void loadPage(page + 1, true);
    }
  }

  async function toggleFavorite(food: FoodItem) {
    if (!accessToken) {
      return;
    }
    try {
      if (food.favorited) {
        await removeFavorite(accessToken, food.foodId);
      } else {
        await addFavorite(accessToken, food.foodId);
      }
      const next = { ...food, favorited: !food.favorited };
      setFoods((previous) =>
        previous.map((item) =>
          item.foodId === next.foodId ? next : item
        )
      );
      if (filter === "favorite" && !next.favorited) {
        void loadPage(1, false);
      }
    } catch {
      setListError("操作失败，请稍后再试。");
    }
  }

  function handleSelectFood(food: FoodItem) {
    setSelectedFood(food);
    setGramsText("");
    setSubmitError(null);
    setStep(2);
  }

  const grams = Number(gramsText);
  const gramsValid = Number.isFinite(grams) && grams > 0 && grams <= MAX_GRAMS;

  async function handleSubmit() {
    if (!accessToken || !selectedFood || !gramsValid) {
      return;
    }
    setIsSubmitting(true);
    setSubmitError(null);
    try {
      await createMealLog(accessToken, {
        date,
        mealType,
        foodId: selectedFood.foodId,
        grams
      });
      await onSaved();
      onClose();
    } catch (error) {
      setSubmitError(getDietErrorMessage(error, "保存记录失败，请稍后再试。"));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 px-4 py-8 backdrop-blur">
      <section className="flex h-[min(86vh,680px)] w-full max-w-lg flex-col overflow-hidden rounded-[32px] border border-white/10 bg-stone-950 shadow-2xl shadow-black/40">
        <div className="flex items-start justify-between gap-4 border-b border-white/10 px-6 py-4">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              Add Meal
            </p>
            <h3 className="mt-1 text-xl font-semibold text-white">
              添加到 {getMealTypeLabel(mealType)}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/10"
          >
            关闭
          </button>
        </div>

        {step === 1 ? (
          <>
            <div className="border-b border-white/10 px-6 py-3">
              <div className="flex flex-wrap items-center gap-2">
                <input
                  type="search"
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="搜索食物名称"
                  className="min-w-0 flex-1 rounded-full border border-white/10 bg-stone-950/70 px-4 py-2 text-sm text-white outline-none placeholder:text-stone-500 focus:border-amber-300/60"
                />
                <button
                  type="button"
                  onClick={() => setUploadOpen(true)}
                  className="rounded-full border border-amber-300/30 px-3 py-1.5 text-xs font-semibold text-amber-200 hover:bg-amber-300/10"
                >
                  上传食物
                </button>
              </div>
              <div className="mt-2 flex flex-wrap gap-2">
                {foodFilterOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setFilter(option.value)}
                    className={[
                      "rounded-full px-3 py-1.5 text-xs font-semibold transition",
                      filter === option.value
                        ? "bg-amber-400 text-stone-950"
                        : "border border-white/10 bg-white/8 text-stone-200 hover:bg-white/12"
                    ].join(" ")}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <div
              ref={scrollRef}
              onScroll={handleScroll}
              className="min-h-0 flex-1 overflow-y-auto px-6 py-4"
            >
              {isInitialLoading ? (
                <div className="py-10 text-center text-sm text-stone-400">
                  正在加载食物...
                </div>
              ) : listError ? (
                <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
                  {listError}
                </div>
              ) : foods.length === 0 ? (
                <div className="py-10 text-center text-sm text-stone-400">
                  没有找到匹配的食物。
                </div>
              ) : (
                <div className="space-y-2">
                  {foods.map((food) => (
                    <button
                      key={food.foodId}
                      type="button"
                      onClick={() => handleSelectFood(food)}
                      className="flex w-full items-center justify-between gap-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-left transition hover:bg-white/10"
                    >
                      <div className="min-w-0">
                        <p className="truncate font-medium text-white">
                          {food.name}
                        </p>
                        <p className="text-xs text-stone-400">
                          {food.caloriesKcal} 千卡 / 100g
                        </p>
                      </div>
                      <div className="flex shrink-0 items-center gap-2">
                        <button
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            void toggleFavorite(food);
                          }}
                          aria-label={food.favorited ? "取消收藏" : "收藏"}
                          className="text-lg leading-none"
                        >
                          <span
                            className={food.favorited ? "text-amber-400" : "text-stone-500"}
                          >
                            {food.favorited ? "★" : "☆"}
                          </span>
                        </button>
                        <FoodSourceTag food={food} />
                      </div>
                    </button>
                  ))}
                  {isLoadingMore ? (
                    <p className="py-3 text-center text-xs text-stone-500">
                      正在加载更多...
                    </p>
                  ) : null}
                </div>
              )}
            </div>
          </>
        ) : (
          <div className="min-h-0 flex-1 space-y-5 overflow-y-auto px-6 py-5">
            {selectedFood ? (
              <div>
                <label className="mb-2 block text-sm text-stone-300">食物</label>
                <div className="flex items-center justify-between gap-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
                  <div className="min-w-0">
                    <p className="truncate font-medium text-white">
                      {selectedFood.name}
                    </p>
                    <p className="text-xs text-stone-400">
                      {selectedFood.caloriesKcal} 千卡 / 100g
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => setStep(1)}
                    className="rounded-full border border-white/10 px-3 py-1.5 text-xs text-stone-200 hover:bg-white/10"
                  >
                    更换
                  </button>
                </div>
              </div>
            ) : null}

            <div>
              <label className="mb-2 block text-sm text-stone-300">克数</label>
              <input
                type="number"
                min="1"
                max={MAX_GRAMS}
                value={gramsText}
                onChange={(event) => setGramsText(event.target.value)}
                placeholder="例如 150"
                className="w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-white outline-none focus:border-amber-300/60"
              />
              <div className="mt-2 flex flex-wrap gap-2">
                {[100, 150, 200].map((value) => (
                  <button
                    key={value}
                    type="button"
                    onClick={() => setGramsText(String(value))}
                    className="rounded-full border border-white/10 bg-white/8 px-3 py-1.5 text-xs text-stone-200 hover:bg-white/12"
                  >
                    {value}g
                  </button>
                ))}
              </div>
              {selectedFood && gramsValid ? (
                <div className="mt-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-stone-200">
                  约 {Math.round((selectedFood.caloriesKcal * grams) / 100)}{" "}
                  千卡 · 蛋白 {Math.round((selectedFood.proteinG * grams) / 100)}
                  g · 碳水 {Math.round((selectedFood.carbsG * grams) / 100)}g ·
                  脂肪 {Math.round((selectedFood.fatG * grams) / 100)}g
                </div>
              ) : null}
            </div>

            {submitError ? (
              <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
                {submitError}
              </div>
            ) : null}

            <button
              type="button"
              disabled={!selectedFood || !gramsValid || isSubmitting}
              onClick={() => void handleSubmit()}
              className="w-full rounded-2xl bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmitting ? "保存中..." : "保存记录"}
            </button>
          </div>
        )}
      </section>

      <UploadFoodDialog
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        onSaved={async () => {
          if (accessToken) {
            await loadPage(1, false);
          }
        }}
      />
    </div>
  );
}
