import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import {
  deleteMealLog,
  getDaySummary,
  getDietTargets,
  setDietTarget,
  updateMealLog
} from "../api/diet";
import { AddMealLogDialog } from "../components/AddMealLogDialog";
import { DietMissingFieldsNotice } from "../components/DietMissingFieldsNotice";
import { DietTargetCard } from "../components/DietTargetCard";
import { MealSection } from "../components/MealSection";
import { NutrientProgressBar } from "../components/NutrientProgressBar";
import { getDietErrorMessage } from "../lib/diet-formatters";
import { mealTypeOrder } from "../lib/diet-enums";
import type { DaySummary, MealType } from "../types/diet";

const DAY_MS = 24 * 60 * 60 * 1000;

function toDateStr(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function addDays(dateStr: string, delta: number): string {
  const date = new Date(`${dateStr}T00:00:00`);
  date.setTime(date.getTime() + delta * DAY_MS);
  return toDateStr(date);
}

export function DietDiaryPage() {
  const { accessToken } = useAuth();
  const [date, setDate] = useState<string>(() => toDateStr(new Date()));
  const [summary, setSummary] = useState<DaySummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeAdd, setActiveAdd] = useState<MealType | null>(null);
  const [targetMissing, setTargetMissing] = useState<string[]>([]);

  useEffect(() => {
    if (!accessToken) {
      return;
    }
    void load(accessToken, date);
  }, [accessToken, date]);

  async function load(token: string, targetDate: string) {
    setIsLoading(true);
    setError(null);
    setTargetMissing([]);
    try {
      const nextSummary = await getDaySummary(token, targetDate);
      setSummary(nextSummary);

      if (!nextSummary.target?.basis) {
        // 资料不足或目标不可用：取精确缺失项（含 activityLevel 等）；失败则退回通用文案
        try {
          const targetResponse = await getDietTargets(token);
          setTargetMissing(targetResponse.missingFields ?? []);
        } catch {
          setTargetMissing([]);
        }
      }
    } catch (error) {
      setError(getDietErrorMessage(error, "加载日记失败，请稍后再试。"));
    } finally {
      setIsLoading(false);
    }
  }

  async function reload() {
    if (accessToken) {
      await load(accessToken, date);
    }
  }

  async function handleDelete(logId: number) {
    if (!accessToken) {
      return;
    }
    await deleteMealLog(accessToken, logId);
    await reload();
  }

  async function handleUpdateGrams(mealType: MealType, logId: number, grams: number) {
    if (!accessToken) {
      return;
    }
    await updateMealLog(accessToken, logId, { grams, mealType, date });
    await reload();
  }

  async function handleSaveTarget(payload: {
    caloriesKcal: number;
    proteinG: number;
    carbsG: number;
    fatG: number;
  }) {
    if (!accessToken) {
      return;
    }
    await setDietTarget(accessToken, payload);
    await reload();
  }

  async function handleClearTarget() {
    if (!accessToken) {
      return;
    }
    await setDietTarget(accessToken, { clear: true });
    await reload();
  }

  // 只有 basis 有效（auto/custom）才算有可用目标；basis=null 视为无目标（显示补齐提示）
  const target = summary?.target?.basis ? summary.target : null;

  return (
    <section className="space-y-8">
      <header className="rounded-[36px] border border-white/10 bg-white/6 p-6 backdrop-blur">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              Diet Diary
            </p>
            <h1 className="mt-2 text-3xl font-semibold text-white sm:text-4xl">
              饮食日记
            </h1>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <button
              type="button"
              onClick={() => setDate(addDays(date, -1))}
              className="rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 hover:bg-white/12"
            >
              ‹ 前一天
            </button>
            <span className="min-w-[7rem] text-center text-sm font-medium text-white">
              {date}
            </span>
            <button
              type="button"
              onClick={() => setDate(addDays(date, 1))}
              className="rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 hover:bg-white/12"
            >
              后一天 ›
            </button>
            <button
              type="button"
              onClick={() => setDate(toDateStr(new Date()))}
              className="rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 hover:bg-amber-300"
            >
              回到今天
            </button>
          </div>
        </div>

        <nav className="mt-5 flex flex-wrap gap-2">
          <Link
            to="/diet/foods"
            className="rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 hover:bg-white/12"
          >
            食物库
          </Link>
          <Link
            to="/diet/stats"
            className="rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 hover:bg-white/12"
          >
            摄入统计
          </Link>
        </nav>
      </header>

      {error ? (
        <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {error}
        </div>
      ) : null}

      {isLoading && !summary ? (
        <div className="flex items-center justify-center py-10">
          <span className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
            正在加载日记...
          </span>
        </div>
      ) : summary ? (
        <>
          {target ? (
            <DietTargetCard
              target={target}
              onSave={handleSaveTarget}
              onClear={handleClearTarget}
            />
          ) : null}

          <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              Today Progress
            </p>
            <h2 className="mt-1 text-2xl font-semibold text-white">今日摄入</h2>

            {!target ? (
              <div className="mt-4">
                <DietMissingFieldsNotice missingFields={targetMissing} />
              </div>
            ) : (
              <div className="mt-4 grid gap-5 lg:grid-cols-2">
                <NutrientProgressBar
                  label="总热量"
                  current={summary.totals.caloriesKcal}
                  target={target.caloriesKcal}
                  pct={summary.progress?.caloriesPct ?? null}
                />
                <NutrientProgressBar
                  label="蛋白质"
                  current={summary.totals.proteinG}
                  target={target.proteinG}
                  pct={summary.progress?.proteinPct ?? null}
                  colorClass="bg-sky-400"
                />
                <NutrientProgressBar
                  label="碳水"
                  current={summary.totals.carbsG}
                  target={target.carbsG}
                  pct={summary.progress?.carbsPct ?? null}
                  colorClass="bg-lime-400"
                />
                <NutrientProgressBar
                  label="脂肪"
                  current={summary.totals.fatG}
                  target={target.fatG}
                  pct={summary.progress?.fatPct ?? null}
                  colorClass="bg-rose-400"
                />
              </div>
            )}
          </section>

          <div className="space-y-6">
            {mealTypeOrder.map((mealType) => (
              <MealSection
                key={mealType}
                mealType={mealType}
                items={summary.meals[mealType] ?? []}
                onAdd={(type) => setActiveAdd(type)}
                onUpdate={(logId, grams) => handleUpdateGrams(mealType, logId, grams)}
                onDelete={handleDelete}
              />
            ))}
          </div>
        </>
      ) : null}

      <AddMealLogDialog
        open={activeAdd !== null}
        date={date}
        defaultMealType={activeAdd ?? "breakfast"}
        onClose={() => setActiveAdd(null)}
        onSaved={async () => {
          if (accessToken) {
            await load(accessToken, date);
          }
        }}
      />
    </section>
  );
}
