import { useState } from "react";
import { useAuth } from "../../../app/providers/AuthProvider";
import { createMealLog } from "../api/diet";
import { FoodPickerDialog } from "./FoodPickerDialog";
import { mealTypeOptions } from "../lib/diet-enums";
import { getDietErrorMessage } from "../lib/diet-formatters";
import type { FoodItem, MealType } from "../types/diet";

const MAX_GRAMS = 5000;

type AddMealLogDialogProps = {
  open: boolean;
  date: string;
  defaultMealType?: MealType;
  onClose: () => void;
  onSaved: () => Promise<void>;
};

export function AddMealLogDialog({
  open,
  date,
  defaultMealType = "breakfast",
  onClose,
  onSaved
}: AddMealLogDialogProps) {
  const { accessToken } = useAuth();
  const [mealType, setMealType] = useState<MealType>(defaultMealType);
  const [foodPickerOpen, setFoodPickerOpen] = useState(false);
  const [food, setFood] = useState<FoodItem | null>(null);
  const [gramsText, setGramsText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open) {
    return null;
  }

  const grams = Number(gramsText);
  const gramsValid = Number.isFinite(grams) && grams > 0 && grams <= MAX_GRAMS;

  function handleSelectFood(selected: FoodItem) {
    setFood(selected);
    setFoodPickerOpen(false);
    setError(null);
  }

  async function handleSubmit() {
    if (!accessToken || !food || !gramsValid) {
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      await createMealLog(accessToken, {
        date,
        mealType,
        foodId: food.foodId,
        grams
      });
      setFood(null);
      setGramsText("");
      await onSaved();
      onClose();
    } catch (error) {
      setError(getDietErrorMessage(error, "保存记录失败，请稍后再试。"));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 px-4 py-8 backdrop-blur">
      <section className="flex max-h-[88vh] w-full max-w-lg flex-col overflow-hidden rounded-[32px] border border-white/10 bg-stone-950 shadow-2xl shadow-black/40">
        <div className="flex items-start justify-between gap-4 border-b border-white/10 px-6 py-4">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              Add Meal
            </p>
            <h3 className="mt-1 text-xl font-semibold text-white">添加记录</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/10"
          >
            关闭
          </button>
        </div>

        <div className="min-h-0 flex-1 space-y-5 overflow-y-auto px-6 py-5">
          <div>
            <label className="mb-2 block text-sm text-stone-300">餐次</label>
            <select
              value={mealType}
              onChange={(event) => setMealType(event.target.value as MealType)}
              className="w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-sm text-white outline-none focus:border-amber-300/60"
            >
              {mealTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-2 block text-sm text-stone-300">食物</label>
            {food ? (
              <div className="flex items-center justify-between gap-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
                <div>
                  <p className="font-medium text-white">{food.name}</p>
                  <p className="text-xs text-stone-400">
                    {food.caloriesKcal} 千卡 / 100g
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setFoodPickerOpen(true)}
                  className="rounded-full border border-white/10 px-3 py-1.5 text-xs text-stone-200 hover:bg-white/10"
                >
                  更换
                </button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => setFoodPickerOpen(true)}
                className="w-full rounded-2xl border border-dashed border-white/20 bg-white/5 px-4 py-4 text-sm text-amber-200 transition hover:bg-white/10"
              >
                选择食物
              </button>
            )}
          </div>

          {food ? (
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
              {gramsValid ? (
                <div className="mt-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-stone-200">
                  约 {Math.round((food.caloriesKcal * grams) / 100)} 千卡 · 蛋白{" "}
                  {Math.round((food.proteinG * grams) / 100)}g · 碳水{" "}
                  {Math.round((food.carbsG * grams) / 100)}g · 脂肪{" "}
                  {Math.round((food.fatG * grams) / 100)}g
                </div>
              ) : null}
            </div>
          ) : null}

          {error ? (
            <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
              {error}
            </div>
          ) : null}

          <button
            type="button"
            disabled={!food || !gramsValid || isSubmitting}
            onClick={() => void handleSubmit()}
            className="w-full rounded-2xl bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? "保存中..." : "保存记录"}
          </button>
        </div>
      </section>

      <FoodPickerDialog
        open={foodPickerOpen}
        onClose={() => setFoodPickerOpen(false)}
        onSelect={handleSelectFood}
      />
    </div>
  );
}
