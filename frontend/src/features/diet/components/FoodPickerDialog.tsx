import { useEffect, useState } from "react";
import { useAuth } from "../../../app/providers/AuthProvider";
import { searchFoods } from "../api/diet";
import { foodFilterOptions } from "../lib/diet-enums";
import { getDietErrorMessage } from "../lib/diet-formatters";
import type {
  FoodFilter,
  FoodItem
} from "../types/diet";

type FoodPickerDialogProps = {
  open: boolean;
  onClose: () => void;
  onSelect: (food: FoodItem) => void;
};

export function FoodPickerDialog({
  open,
  onClose,
  onSelect
}: FoodPickerDialogProps) {
  const { accessToken } = useAuth();
  const [keyword, setKeyword] = useState("");
  const [filter, setFilter] = useState<FoodFilter>("all");
  const [foods, setFoods] = useState<FoodItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !accessToken) {
      return;
    }

    const token = accessToken;
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setError(null);
      try {
        const response = await searchFoods(token, {
          keyword,
          filter
        });
        if (!cancelled) {
          setFoods(response.foods);
        }
      } catch (error) {
        if (!cancelled) {
          setError(getDietErrorMessage(error, "加载食物库失败，请稍后再试。"));
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [open, accessToken, keyword, filter]);

  if (!open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 px-4 py-8 backdrop-blur">
      <section className="flex h-[min(80vh,640px)] w-full max-w-2xl flex-col overflow-hidden rounded-[32px] border border-white/10 bg-stone-950 shadow-2xl shadow-black/40">
        <div className="flex items-start justify-between gap-4 border-b border-white/10 px-6 py-4">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              Pick Food
            </p>
            <h3 className="mt-1 text-xl font-semibold text-white">选择食物</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/10"
          >
            关闭
          </button>
        </div>

        <div className="border-b border-white/10 px-6 py-3">
          <div className="flex flex-wrap items-center gap-2">
            <input
              type="search"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="搜索食物名称"
              className="min-w-0 flex-1 rounded-full border border-white/10 bg-stone-950/70 px-4 py-2 text-sm text-white outline-none placeholder:text-stone-500 focus:border-amber-300/60"
            />
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

        <div className="min-h-0 flex-1 overflow-y-auto px-6 py-4">
          {isLoading ? (
            <div className="py-10 text-center text-sm text-stone-400">
              正在加载食物...
            </div>
          ) : error ? (
            <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
              {error}
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
                  onClick={() => onSelect(food)}
                  className="flex w-full items-center justify-between gap-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-left transition hover:bg-white/10"
                >
                  <div className="min-w-0">
                    <p className="truncate font-medium text-white">{food.name}</p>
                    <p className="text-xs text-stone-400">
                      {food.caloriesKcal} 千卡 / 100g
                    </p>
                  </div>
                  <FoodSourceTag food={food} />
                </button>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

export function FoodSourceTag({ food }: { food: FoodItem }) {
  const isUser = food.source === "user";
  return (
    <span
      className={[
        "shrink-0 rounded-full px-3 py-1 text-xs",
        isUser
          ? "border border-sky-300/20 bg-sky-300/10 text-sky-100"
          : "border border-white/10 bg-white/8 text-stone-300"
      ].join(" ")}
    >
      {food.sourceLabel}
      {isUser && food.ownerNickname ? ` · ${food.ownerNickname}` : ""}
    </span>
  );
}
