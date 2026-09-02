import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import {
  addFavorite,
  removeFavorite,
  searchFoods
} from "../api/diet";
import { FoodDetailDialog } from "../components/FoodDetailDialog";
import { FoodSourceTag } from "../components/FoodPickerDialog";
import { UploadFoodDialog } from "../components/UploadFoodDialog";
import { foodFilterOptions } from "../lib/diet-enums";
import { getDietErrorMessage } from "../lib/diet-formatters";
import type { FoodFilter, FoodItem } from "../types/diet";

export function DietFoodsPage() {
  const { accessToken } = useAuth();
  const [keyword, setKeyword] = useState("");
  const [filter, setFilter] = useState<FoodFilter>("all");
  const [foods, setFoods] = useState<FoodItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [detailFood, setDetailFood] = useState<FoodItem | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);

  useEffect(() => {
    if (!accessToken) {
      return;
    }
    void load(accessToken);
  }, [accessToken, keyword, filter]);

  async function load(token: string) {
    setIsLoading(true);
    setError(null);
    try {
      const response = await searchFoods(token, { keyword, filter });
      setFoods(response.foods);
    } catch (error) {
      setError(getDietErrorMessage(error, "加载食物库失败，请稍后再试。"));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleToggleFavorite(food: FoodItem) {
    if (!accessToken) {
      return;
    }
    if (food.favorited) {
      await removeFavorite(accessToken, food.foodId);
    } else {
      await addFavorite(accessToken, food.foodId);
    }
    const updated = { ...food, favorited: !food.favorited };
    setDetailFood(updated);
    setFoods((previous) =>
      previous.map((item) =>
        item.foodId === updated.foodId ? updated : item
      )
    );
    if (filter === "favorite") {
      await load(accessToken);
    }
  }

  return (
    <section className="space-y-8">
      <header className="rounded-[36px] border border-white/10 bg-white/6 p-6 backdrop-blur">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              Food Library
            </p>
            <h1 className="mt-2 text-3xl font-semibold text-white sm:text-4xl">
              食物库
            </h1>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setUploadOpen(true)}
              className="rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 hover:bg-amber-300"
            >
              上传食物
            </button>
            <Link
              to="/diet"
              className="rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 hover:bg-white/12"
            >
              返回日记
            </Link>
          </div>
        </div>

        <div className="mt-5 flex flex-wrap items-center gap-2">
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
                "rounded-full px-4 py-2 text-sm font-semibold transition",
                filter === option.value
                  ? "bg-amber-400 text-stone-950"
                  : "border border-white/10 bg-white/8 text-stone-200 hover:bg-white/12"
              ].join(" ")}
            >
              {option.label}
            </button>
          ))}
        </div>
      </header>

      {error ? (
        <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {error}
        </div>
      ) : null}

      {isLoading ? (
        <div className="flex items-center justify-center py-10">
          <span className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
            正在加载食物...
          </span>
        </div>
      ) : foods.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-white/10 bg-black/20 px-5 py-10 text-center text-sm text-stone-400">
          没有找到匹配的食物。
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {foods.map((food) => (
            <button
              key={food.foodId}
              type="button"
              onClick={() => setDetailFood(food)}
              className="flex items-center justify-between gap-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-left transition hover:bg-white/10"
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

      <FoodDetailDialog
        food={detailFood}
        onClose={() => setDetailFood(null)}
        onToggleFavorite={handleToggleFavorite}
      />

      <UploadFoodDialog
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        onSaved={async () => {
          if (accessToken) {
            await load(accessToken);
          }
        }}
      />
    </section>
  );
}
