import { useState } from "react";
import type { DietTarget } from "../types/diet";

export type CustomTargetValues = {
  caloriesKcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
};

type DietTargetCardProps = {
  target: DietTarget;
  onSave: (payload: CustomTargetValues) => Promise<void>;
  onClear: () => Promise<void>;
};

export function DietTargetCard({
  target,
  onSave,
  onClear
}: DietTargetCardProps) {
  const [editing, setEditing] = useState(false);
  const [calories, setCalories] = useState(String(target.caloriesKcal ?? ""));
  const [protein, setProtein] = useState(String(target.proteinG ?? ""));
  const [carbs, setCarbs] = useState(String(target.carbsG ?? ""));
  const [fat, setFat] = useState(String(target.fatG ?? ""));
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSave() {
    const num = (value: string) => Number(value);
    if (
      !Number.isFinite(num(calories)) ||
      !Number.isFinite(num(protein)) ||
      !Number.isFinite(num(carbs)) ||
      !Number.isFinite(num(fat))
    ) {
      setError("请输入有效数值。");
      return;
    }
    setIsSaving(true);
    setError(null);
    try {
      await onSave({
        caloriesKcal: num(calories),
        proteinG: num(protein),
        carbsG: num(carbs),
        fatG: num(fat)
      });
      setEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "保存目标失败。");
    } finally {
      setIsSaving(false);
    }
  }

  const isCustom = target.basis === "custom";

  return (
    <section className="rounded-[32px] border border-amber-300/20 bg-white/6 p-6 backdrop-blur">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
            Daily Target
          </p>
          <h3 className="mt-1 text-xl font-semibold text-white">每日目标</h3>
        </div>
        {isCustom ? (
          <span className="rounded-full border border-white/10 bg-white/8 px-3 py-1 text-xs text-stone-200">
            自定义
          </span>
        ) : (
          <span className="rounded-full border border-white/10 bg-white/8 px-3 py-1 text-xs text-stone-200">
            自动
          </span>
        )}
      </div>

      {!editing ? (
        <>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <TargetStat label="热量" value={`${Math.round(target.caloriesKcal ?? 0)} 千卡`} />
            <TargetStat label="蛋白质" value={`${Math.round(target.proteinG ?? 0)} g`} />
            <TargetStat label="碳水" value={`${Math.round(target.carbsG ?? 0)} g`} />
            <TargetStat label="脂肪" value={`${Math.round(target.fatG ?? 0)} g`} />
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setEditing(true)}
              className="rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 transition hover:bg-amber-300"
            >
              自定义目标
            </button>
            {isCustom ? (
              <button
                type="button"
                onClick={() => void onClear()}
                className="rounded-full border border-white/10 px-4 py-2 text-sm font-semibold text-stone-100 hover:bg-white/10"
              >
                恢复自动
              </button>
            ) : null}
          </div>
        </>
      ) : (
        <div className="mt-4 space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <NumberInput label="热量(千卡)" value={calories} onChange={setCalories} />
            <NumberInput label="蛋白质(g)" value={protein} onChange={setProtein} />
            <NumberInput label="碳水(g)" value={carbs} onChange={setCarbs} />
            <NumberInput label="脂肪(g)" value={fat} onChange={setFat} />
          </div>
          {error ? (
            <p className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-2 text-sm text-rose-100">
              {error}
            </p>
          ) : null}
          <div className="flex gap-2">
            <button
              type="button"
              disabled={isSaving}
              onClick={() => void handleSave()}
              className="rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
            >
              {isSaving ? "保存中..." : "保存自定义"}
            </button>
            <button
              type="button"
              onClick={() => setEditing(false)}
              className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200"
            >
              取消
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

function TargetStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
      <p className="text-xs text-stone-500">{label}</p>
      <p className="mt-1 font-semibold text-white">{value}</p>
    </div>
  );
}

function NumberInput({
  label,
  value,
  onChange
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div>
      <label className="mb-1 block text-sm text-stone-300">{label}</label>
      <input
        type="number"
        min="1"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="w-full rounded-2xl border border-white/10 bg-stone-950/70 px-3 py-2 text-sm text-white outline-none"
      />
    </div>
  );
}
