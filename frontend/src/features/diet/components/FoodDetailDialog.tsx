import { FoodSourceTag } from "./FoodPickerDialog";
import { getFoodCategoryLabel } from "../lib/diet-enums";
import type { FoodItem } from "../types/diet";

type FoodDetailDialogProps = {
  food: FoodItem | null;
  onClose: () => void;
  onToggleFavorite?: (food: FoodItem) => Promise<void>;
};

export function FoodDetailDialog({
  food,
  onClose,
  onToggleFavorite
}: FoodDetailDialogProps) {
  if (!food) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 px-4 py-8 backdrop-blur">
      <section className="w-full max-w-md rounded-[32px] border border-white/10 bg-stone-950 p-6 shadow-2xl shadow-black/40">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h3 className="truncate text-2xl font-semibold text-white">
              {food.name}
            </h3>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <FoodSourceTag food={food} />
              {getFoodCategoryLabel(food.category) ? (
                <span className="rounded-full border border-white/10 bg-white/8 px-3 py-1 text-xs text-stone-300">
                  {getFoodCategoryLabel(food.category)}
                </span>
              ) : null}
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/10"
          >
            关闭
          </button>
        </div>

        <p className="mt-6 text-xs text-stone-400">每 100g 营养</p>
        <div className="mt-2 grid grid-cols-2 gap-3">
          <Nutrient label="热量" value={`${Math.round(food.caloriesKcal)} 千卡`} />
          <Nutrient label="蛋白质" value={`${food.proteinG} g`} />
          <Nutrient label="碳水" value={`${food.carbsG} g`} />
          <Nutrient label="脂肪" value={`${food.fatG} g`} />
        </div>

        {onToggleFavorite ? (
          <button
            type="button"
            onClick={() => void onToggleFavorite(food)}
            className="mt-6 w-full rounded-2xl border border-amber-300/30 px-5 py-3 font-medium text-amber-200 transition hover:bg-amber-300/10"
          >
            {food.favorited ? "取消收藏" : "收藏"}
          </button>
        ) : null}
      </section>
    </div>
  );
}

function Nutrient({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
      <p className="text-xs text-stone-500">{label}</p>
      <p className="mt-1 font-semibold text-white">{value}</p>
    </div>
  );
}
