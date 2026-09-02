import { useEffect, useState } from "react";
import { DietMissingFieldsNotice } from "./DietMissingFieldsNotice";
import { NutrientProgressBar } from "./NutrientProgressBar";
import type {
  DaySummary,
  DietTarget,
  NutrientValues
} from "../types/diet";

export type CustomTargetValues = {
  caloriesKcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
};

type IntakeSummaryCardProps = {
  totals: NutrientValues;
  target: DietTarget | null;
  progress: DaySummary["progress"] | null;
  missingFields: string[];
  onSaveTarget: (payload: CustomTargetValues) => Promise<void>;
  onClearTarget: () => Promise<void>;
};

export function IntakeSummaryCard({
  totals,
  target,
  progress,
  missingFields,
  onSaveTarget,
  onClearTarget
}: IntakeSummaryCardProps) {
  const hasTarget = target !== null && target.basis !== null;
  const [editing, setEditing] = useState(false);
  const [calories, setCalories] = useState(String(target?.caloriesKcal ?? ""));
  const [protein, setProtein] = useState(String(target?.proteinG ?? ""));
  const [carbs, setCarbs] = useState(String(target?.carbsG ?? ""));
  const [fat, setFat] = useState(String(target?.fatG ?? ""));
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // target（随日期/保存结果变化）更新时同步编辑表单初始值，避免残留旧值
  useEffect(() => {
    setCalories(target?.caloriesKcal != null ? String(target.caloriesKcal) : "");
    setProtein(target?.proteinG != null ? String(target.proteinG) : "");
    setCarbs(target?.carbsG != null ? String(target.carbsG) : "");
    setFat(target?.fatG != null ? String(target.fatG) : "");
  }, [target?.caloriesKcal, target?.proteinG, target?.carbsG, target?.fatG]);

  async function handleSave() {
    const num = (value: string) => Number(value);
    const nextCalories = num(calories);
    const nextProtein = num(protein);
    const nextCarbs = num(carbs);
    const nextFat = num(fat);
    if (
      !Number.isFinite(nextCalories) ||
      !Number.isFinite(nextProtein) ||
      !Number.isFinite(nextCarbs) ||
      !Number.isFinite(nextFat) ||
      nextCalories <= 0 ||
      nextProtein <= 0 ||
      nextCarbs <= 0 ||
      nextFat <= 0
    ) {
      setError("请输入大于 0 的有效数值。");
      return;
    }
    setIsSaving(true);
    setError(null);
    try {
      await onSaveTarget({
        caloriesKcal: nextCalories,
        proteinG: nextProtein,
        carbsG: nextCarbs,
        fatG: nextFat
      });
      setEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "保存目标失败。");
    } finally {
      setIsSaving(false);
    }
  }

  const isCustom = target?.basis === "custom";

  return (
    <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
            Today Intake
          </p>
          <h2 className="mt-1 text-2xl font-semibold text-white">今日摄入</h2>
        </div>
        {hasTarget ? (
          <span className="rounded-full border border-white/10 bg-white/8 px-3 py-1 text-xs text-stone-200">
            {isCustom ? "自定义目标" : "自动目标"}
          </span>
        ) : null}
      </div>

      {!editing ? (
        <>
          {/* 总热量大值突出 */}
          <div className="mt-6">
            <div className="flex items-baseline justify-between gap-3">
              <span className="text-sm text-stone-300">总热量</span>
              <span className="text-3xl font-bold text-white">
                {Math.round(totals.caloriesKcal)}
                {target?.caloriesKcal != null ? (
                  <span className="ml-2 text-base font-medium text-stone-400">
                    / {Math.round(target.caloriesKcal)} 千卡
                  </span>
                ) : null}
              </span>
            </div>
            {hasTarget ? (
              <div className="mt-2">
                <div className="h-4 w-full overflow-hidden rounded-full bg-white/8">
                  <div
                    className="h-full rounded-full bg-amber-400"
                    style={{
                      width: `${Math.min(
                        Math.max(progress?.caloriesPct ?? 0, 0),
                        100
                      )}%`
                    }}
                  />
                </div>
                {progress?.caloriesPct != null ? (
                  <p className="mt-1 text-xs text-stone-500">
                    {Math.round(progress.caloriesPct)}%
                  </p>
                ) : null}
              </div>
            ) : null}
          </div>

          {/* 蛋白 / 碳水 / 脂肪均分一行 */}
          <div className="mt-6 grid gap-4 sm:grid-cols-3">
            <NutrientProgressBar
              label="蛋白质"
              current={totals.proteinG}
              target={target?.proteinG ?? null}
              pct={progress?.proteinPct ?? null}
              colorClass="bg-sky-400"
            />
            <NutrientProgressBar
              label="碳水"
              current={totals.carbsG}
              target={target?.carbsG ?? null}
              pct={progress?.carbsPct ?? null}
              colorClass="bg-lime-400"
            />
            <NutrientProgressBar
              label="脂肪"
              current={totals.fatG}
              target={target?.fatG ?? null}
              pct={progress?.fatPct ?? null}
              colorClass="bg-rose-400"
            />
          </div>

          {!hasTarget ? (
            <div className="mt-5">
              <DietMissingFieldsNotice missingFields={missingFields} />
            </div>
          ) : null}

          <div className="mt-5 flex flex-wrap items-center gap-2">
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
                onClick={() => void onClearTarget()}
                className="rounded-full border border-white/10 px-4 py-2 text-sm font-semibold text-stone-100 hover:bg-white/10"
              >
                恢复自动
              </button>
            ) : null}
          </div>
        </>
      ) : (
        <div className="mt-6 space-y-4">
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
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              disabled={isSaving}
              onClick={() => void handleSave()}
              className="rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
            >
              {isSaving ? "保存中..." : "保存自定义目标"}
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
