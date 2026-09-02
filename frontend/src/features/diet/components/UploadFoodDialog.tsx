import { useState } from "react";
import { useAuth } from "../../../app/providers/AuthProvider";
import { uploadFood } from "../api/diet";
import { foodCategoryOptions } from "../lib/diet-enums";
import { getDietErrorMessage } from "../lib/diet-formatters";
import type { FoodCategory } from "../types/diet";

type UploadFoodDialogProps = {
  open: boolean;
  onClose: () => void;
  onSaved: () => Promise<void>;
};

export function UploadFoodDialog({
  open,
  onClose,
  onSaved
}: UploadFoodDialogProps) {
  const { accessToken } = useAuth();
  const [name, setName] = useState("");
  const [category, setCategory] = useState<FoodCategory | "">("");
  const [calories, setCalories] = useState("");
  const [protein, setProtein] = useState("");
  const [carbs, setCarbs] = useState("");
  const [fat, setFat] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open) {
    return null;
  }

  function reset() {
    setName("");
    setCategory("");
    setCalories("");
    setProtein("");
    setCarbs("");
    setFat("");
    setError(null);
  }

  async function handleSubmit() {
    if (!accessToken) {
      return;
    }
    const trimmed = name.trim();
    const num = (value: string) => Number(value);
    const c = num(calories);
    const p = num(protein);
    const cb = num(carbs);
    const f = num(fat);

    if (!trimmed) {
      setError("请输入食物名称。");
      return;
    }
    if (trimmed.length > 64) {
      setError("食物名称不能超过 64 个字符。");
      return;
    }
    if (![c, p, cb, f].every((value) => Number.isFinite(value) && value >= 0)) {
      setError("四项营养（每 100g）必须为不小于 0 的数值。");
      return;
    }
    if (c + p + cb + f === 0) {
      setError("四项营养不能全为 0。");
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      await uploadFood(accessToken, {
        name: trimmed,
        category: category || null,
        caloriesKcal: c,
        proteinG: p,
        carbsG: cb,
        fatG: f
      });
      reset();
      await onSaved();
      onClose();
    } catch (error) {
      setError(getDietErrorMessage(error, "上传食物失败，请稍后再试。"));
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
              Upload Food
            </p>
            <h3 className="mt-1 text-xl font-semibold text-white">上传食物</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/10"
          >
            关闭
          </button>
        </div>

        <div className="space-y-4 overflow-y-auto px-6 py-5">
          <InputField label="名称 *">
            <input
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="例如 自制燕麦饼干"
              className={inputClass}
            />
          </InputField>

          <InputField label="分类（可选）">
            <select
              value={category}
              onChange={(event) => setCategory(event.target.value as FoodCategory | "")}
              className={inputClass}
            >
              <option value="">不分类</option>
              {foodCategoryOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </InputField>

          <p className="text-xs text-stone-400">
            以下为每 100g 可食部分的营养素：
          </p>
          <div className="grid grid-cols-2 gap-3">
            <InputField label="热量(kcal) *">
              <input type="number" min="0" value={calories} onChange={(event) => setCalories(event.target.value)} className={inputClass} />
            </InputField>
            <InputField label="蛋白质(g) *">
              <input type="number" min="0" value={protein} onChange={(event) => setProtein(event.target.value)} className={inputClass} />
            </InputField>
            <InputField label="碳水(g) *">
              <input type="number" min="0" value={carbs} onChange={(event) => setCarbs(event.target.value)} className={inputClass} />
            </InputField>
            <InputField label="脂肪(g) *">
              <input type="number" min="0" value={fat} onChange={(event) => setFat(event.target.value)} className={inputClass} />
            </InputField>
          </div>

          {error ? (
            <p className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
              {error}
            </p>
          ) : null}

          <button
            type="button"
            disabled={isSubmitting}
            onClick={() => void handleSubmit()}
            className="w-full rounded-2xl bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
          >
            {isSubmitting ? "上传中..." : "上传食物"}
          </button>
        </div>
      </section>
    </div>
  );
}

const inputClass =
  "w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-sm text-white outline-none focus:border-amber-300/60";

function InputField({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block text-sm text-stone-300">
      {label}
      <span className="mt-1 block">{children}</span>
    </label>
  );
}
