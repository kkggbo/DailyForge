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
import { AddMealFlowDialog } from "../components/AddMealFlowDialog";
import { IntakeSummaryCard } from "../components/IntakeSummaryCard";
import { MealDetailPanel } from "../components/MealDetailPanel";
import { UploadFoodDialog } from "../components/UploadFoodDialog";
import { getDietErrorMessage } from "../lib/diet-formatters";
import { getMealTypeLabel, mealTypeOrder } from "../lib/diet-enums";
import type {
  CustomTargetValues
} from "../components/IntakeSummaryCard";
import type {
  DaySummary,
  MealLogItem,
  MealType,
  NutrientValues
} from "../types/diet";

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

function sumMeal(items: MealLogItem[]): NutrientValues {
  return items.reduce(
    (acc, item) => ({
      caloriesKcal: acc.caloriesKcal + item.caloriesKcal,
      proteinG: acc.proteinG + item.proteinG,
      carbsG: acc.carbsG + item.carbsG,
      fatG: acc.fatG + item.fatG
    }),
    { caloriesKcal: 0, proteinG: 0, carbsG: 0, fatG: 0 }
  );
}

export function DietDiaryPage() {
  const { accessToken } = useAuth();
  const today = toDateStr(new Date());
  const [date, setDate] = useState<string>(today);
  const [summary, setSummary] = useState<DaySummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [targetMissing, setTargetMissing] = useState<string[]>([]);
  const [expandedMeal, setExpandedMeal] = useState<MealType | null>(null);
  const [addMeal, setAddMeal] = useState<MealType | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);

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

  async function handleUpdateGrams(
    mealType: MealType,
    logId: number,
    grams: number
  ) {
    if (!accessToken) {
      return;
    }
    await updateMealLog(accessToken, logId, { grams, mealType, date });
    await reload();
  }

  async function handleSaveTarget(payload: CustomTargetValues) {
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

  // 只有 basis 有效（auto/custom）才算有可用目标；basis=null 视为无目标
  const target = summary?.target?.basis ? summary.target : null;
  const isToday = date === today;

  function handleMealCardClick(mealType: MealType) {
    setExpandedMeal((previous) =>
      previous === mealType ? null : mealType
    );
  }

  function openAdd(mealType: MealType) {
    setAddMeal(mealType);
  }

  return (
    <section className="space-y-6">
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
            {!isToday ? (
              <button
                type="button"
                onClick={() => setDate(addDays(date, 1))}
                className="rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 hover:bg-white/12"
              >
                后一天 ›
              </button>
            ) : null}
            <button
              type="button"
              onClick={() => setDate(today)}
              className="rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 hover:bg-amber-300"
            >
              回到今天
            </button>
          </div>
        </div>

        <div className="mt-5 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setUploadOpen(true)}
            className="rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 hover:bg-amber-300"
          >
            上传食物
          </button>
          <Link
            to="/diet/stats"
            className="rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 hover:bg-white/12"
          >
            摄入统计
          </Link>
        </div>
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
          <IntakeSummaryCard
            totals={summary.totals}
            target={target}
            progress={summary.progress}
            missingFields={targetMissing}
            onSaveTarget={handleSaveTarget}
            onClearTarget={handleClearTarget}
          />

          {/* 四餐卡片：任意屏幕宽度都保持一行均分（手机也同行），窄屏压缩内边距与字号 */}
          <div className="grid grid-cols-4 gap-1.5 sm:gap-3">
            {mealTypeOrder.map((mealType) => {
              const items = summary.meals[mealType] ?? [];
              const mealTotals = sumMeal(items);
              return (
                <MealCard
                  key={mealType}
                  mealType={mealType}
                  totals={mealTotals}
                  active={expandedMeal === mealType}
                  onCardClick={() => handleMealCardClick(mealType)}
                  onAdd={() => openAdd(mealType)}
                />
              );
            })}
          </div>

          {expandedMeal ? (
            <MealDetailPanel
              mealType={expandedMeal}
              items={summary.meals[expandedMeal] ?? []}
              onUpdate={handleUpdateGrams}
              onDelete={handleDelete}
            />
          ) : null}
        </>
      ) : null}

      <AddMealFlowDialog
        open={addMeal !== null}
        date={date}
        mealType={addMeal ?? "breakfast"}
        onClose={() => setAddMeal(null)}
        onSaved={reload}
      />

      <UploadFoodDialog
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        onSaved={reload}
      />
    </section>
  );
}

function MealCard({
  mealType,
  totals,
  active,
  onCardClick,
  onAdd
}: {
  mealType: MealType;
  totals: NutrientValues;
  active: boolean;
  onCardClick: () => void;
  onAdd: () => void;
}) {
  return (
    <div
      onClick={onCardClick}
      className={[
        "relative cursor-pointer overflow-hidden rounded-3xl border p-2 transition sm:p-5",
        active
          ? "border-amber-300/40 bg-white/10"
          : "border-white/10 bg-white/6 hover:bg-white/10"
      ].join(" ")}
    >
      <div className="flex items-center justify-between gap-1 sm:gap-2">
        <h3 className="min-w-0 truncate text-xs font-semibold text-white sm:text-base">
          {getMealTypeLabel(mealType)}
        </h3>
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onAdd();
          }}
          aria-label={`向${getMealTypeLabel(mealType)}添加食物`}
          className="df-round-btn flex aspect-square h-6 w-6 shrink-0 items-center justify-center rounded-full bg-amber-400 p-0 text-base font-bold leading-none text-stone-950 transition hover:bg-amber-300 sm:h-7 sm:w-7"
        >
          <span aria-hidden="true" className="block leading-none">
            +
          </span>
        </button>
      </div>
      <p className="mt-1.5 whitespace-nowrap text-sm font-bold text-white sm:mt-3 sm:text-2xl">
        {Math.round(totals.caloriesKcal)}
        <span className="ml-0.5 text-[10px] font-normal text-stone-400 sm:ml-1 sm:text-xs">
          千卡
        </span>
      </p>
      {/* 窄屏（四卡一行）下宏量改为纵向小字；宽屏单行展示 */}
      <div className="mt-1 flex flex-col gap-0.5 text-[10px] leading-4 text-stone-400 sm:mt-2 sm:flex-row sm:flex-wrap sm:gap-x-2 sm:text-xs sm:leading-5">
        <span className="truncate">蛋白 {Math.round(totals.proteinG)}g</span>
        <span className="truncate">碳水 {Math.round(totals.carbsG)}g</span>
        <span className="truncate">脂肪 {Math.round(totals.fatG)}g</span>
      </div>
    </div>
  );
}
