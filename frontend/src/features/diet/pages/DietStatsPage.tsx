import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import { useAuth } from "../../../app/providers/AuthProvider";
import { getDietStats, getDietTargets } from "../api/diet";
import { DietMissingFieldsNotice } from "../components/DietMissingFieldsNotice";
import { getDietErrorMessage } from "../lib/diet-formatters";
import type { DietStats } from "../types/diet";

type RangePreset = "7d" | "30d" | "90d" | "year" | "custom";
type Range = { preset: RangePreset; from?: string; to?: string };

const DAY_MS = 24 * 60 * 60 * 1000;
const rangeOptions: Array<{ value: RangePreset; label: string }> = [
  { value: "7d", label: "近 7 天" },
  { value: "30d", label: "近 30 天" },
  { value: "90d", label: "近 90 天" },
  { value: "year", label: "今年" },
  { value: "custom", label: "自定义" }
];

const macroColors = ["#38bdf8", "#a3e635", "#fbbf24"];

function toDateStr(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function buildQuery(range: Range): { from?: string; to?: string } {
  if (range.preset === "custom") {
    return {
      ...(range.from ? { from: range.from } : {}),
      ...(range.to ? { to: range.to } : {})
    };
  }
  const now = new Date();
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
  let start: Date;
  if (range.preset === "7d") {
    start = new Date(end.getTime() - 6 * DAY_MS);
  } else if (range.preset === "30d") {
    start = new Date(end.getTime() - 29 * DAY_MS);
  } else if (range.preset === "90d") {
    start = new Date(end.getTime() - 89 * DAY_MS);
  } else {
    start = new Date(now.getFullYear(), 0, 1);
  }
  return { from: toDateStr(start), to: toDateStr(end) };
}

export function DietStatsPage() {
  const { accessToken } = useAuth();
  const [range, setRange] = useState<Range>({ preset: "30d" });
  const [stats, setStats] = useState<DietStats | null>(null);
  const [targetMissing, setTargetMissing] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }
    const token = accessToken;
    let cancelled = false;
    async function load() {
      setIsLoading(true);
      setError(null);
      setTargetMissing([]);
      try {
        const nextStats = await getDietStats(token, buildQuery(range));
        if (!cancelled) {
          setStats(nextStats);
        }
        // 无目标（资料不足）：并行取精确缺失项，供「完善资料」提示展示
        if (!cancelled && !nextStats.goalAdherence) {
          try {
            const targetResponse = await getDietTargets(token);
            if (!cancelled) {
              setTargetMissing(targetResponse.missingFields ?? []);
            }
          } catch {
            // 拿不到缺失项时显示通用补齐文案
          }
        }
      } catch (error) {
        if (!cancelled) {
          setError(getDietErrorMessage(error, "加载摄入统计失败，请稍后再试。"));
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
  }, [accessToken, range]);

  const macroPieData = useMemo(() => {
    if (!stats) {
      return [];
    }
    return [
      { name: "蛋白质", value: stats.macroShare.proteinPct },
      { name: "碳水", value: stats.macroShare.carbsPct },
      { name: "脂肪", value: stats.macroShare.fatPct }
    ];
  }, [stats]);

  return (
    <section className="space-y-8">
      <header className="rounded-[36px] border border-white/10 bg-white/6 p-6 backdrop-blur">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              Diet Stats
            </p>
            <h1 className="mt-2 text-3xl font-semibold text-white sm:text-4xl">
              摄入统计
            </h1>
          </div>
          <Link
            to="/diet"
            className="rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 hover:bg-white/12"
          >
            返回日记
          </Link>
        </div>

        <div className="mt-5 flex flex-wrap items-center gap-2">
          {rangeOptions.map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => setRange({ preset: option.value })}
              className={[
                "rounded-full px-4 py-2 text-sm font-semibold transition",
                range.preset === option.value
                  ? "bg-amber-400 text-stone-950"
                  : "border border-white/10 bg-white/8 text-stone-200 hover:bg-white/12"
              ].join(" ")}
            >
              {option.label}
            </button>
          ))}
        </div>

        {range.preset === "custom" ? (
          <div className="mt-3 flex flex-wrap items-center gap-2 text-sm text-stone-300">
            <input
              type="date"
              value={range.from ?? ""}
              onChange={(event) =>
                setRange({ ...range, from: event.target.value || undefined })
              }
              className="rounded-full border border-white/10 bg-stone-950/70 px-3 py-2 text-sm text-white outline-none"
            />
            <span>至</span>
            <input
              type="date"
              value={range.to ?? ""}
              onChange={(event) =>
                setRange({ ...range, to: event.target.value || undefined })
              }
              className="rounded-full border border-white/10 bg-stone-950/70 px-3 py-2 text-sm text-white outline-none"
            />
          </div>
        ) : null}
      </header>

      {error ? (
        <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {error}
        </div>
      ) : isLoading ? (
        <div className="flex items-center justify-center py-10">
          <span className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
            正在加载统计...
          </span>
        </div>
      ) : stats ? (
        <>
          <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
            <h2 className="text-xl font-semibold text-white">每日热量</h2>
            {stats.dailyCalories.length === 0 ? (
              <p className="mt-4 py-8 text-center text-sm text-stone-400">
                该时间范围内暂无记录。
              </p>
            ) : (
              <div className="mt-4 h-64 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart
                    data={stats.dailyCalories}
                    margin={{ top: 8, right: 16, bottom: 8, left: 0 }}
                  >
                    <CartesianGrid stroke="rgba(255,255,255,0.08)" vertical={false} />
                    <XAxis
                      dataKey="date"
                      tick={{ fill: "#a8a29e", fontSize: 12 }}
                    />
                    <YAxis tick={{ fill: "#a8a29e", fontSize: 12 }} width={56} />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: "#1c1917",
                        border: "1px solid rgba(255,255,255,0.12)",
                        borderRadius: 12,
                        color: "#f5f5f4"
                      }}
                    />
                    <Line
                      type="monotone"
                      dataKey="caloriesKcal"
                      stroke="#fbbf24"
                      strokeWidth={2}
                      dot={{ r: 3 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            )}
          </section>

          <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
            <h2 className="text-xl font-semibold text-white">宏量占比</h2>
            <div className="mt-4 h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={macroPieData}
                    dataKey="value"
                    nameKey="name"
                    innerRadius={60}
                    outerRadius={90}
                    label
                  >
                    {macroPieData.map((_, index) => (
                      <Cell
                        key={index}
                        fill={macroColors[index % macroColors.length]}
                      />
                    ))}
                  </Pie>
                  <Legend />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#1c1917",
                      border: "1px solid rgba(255,255,255,0.12)",
                      borderRadius: 12,
                      color: "#f5f5f4"
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </section>

          <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
            <h2 className="text-xl font-semibold text-white">周均值</h2>
            {stats.weeklyAverage.length === 0 ? (
              <p className="mt-4 text-sm text-stone-400">暂无数据。</p>
            ) : (
              <div className="mt-4 space-y-2">
                {stats.weeklyAverage.map((week) => (
                  <div
                    key={week.weekStart}
                    className="rounded-2xl border border-white/10 bg-black/20 px-4 py-3 text-sm"
                  >
                    <p className="text-stone-300">周开始：{week.weekStart}</p>
                    <p className="mt-1 text-stone-200">
                      热量 {Math.round(week.caloriesKcal)} · 蛋白 {week.proteinG}g
                      · 碳水 {week.carbsG}g · 脂肪 {week.fatG}g
                    </p>
                  </div>
                ))}
              </div>
            )}
          </section>

          {stats.goalAdherence ? (
            <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
              <h2 className="text-xl font-semibold text-white">目标符合度</h2>
              <p className="mt-1 text-xs text-stone-500">
                每天总热量落在当日目标 ±10% 内即算达标；只统计有饮食记录的天数，符合率 = 目标内天数 ÷ 有记录天数。
              </p>
              <div className="mt-4 flex flex-wrap gap-3">
                <Stat label="目标内天数" value={`${stats.goalAdherence.daysWithinTarget}`} />
                <Stat label="有记录天数" value={`${stats.goalAdherence.daysLogged}`} />
                <Stat label="符合率" value={`${stats.goalAdherence.ratePct}%`} />
              </div>
            </section>
          ) : (
            <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
              <h2 className="text-xl font-semibold text-white">目标符合度</h2>
              <div className="mt-4">
                <DietMissingFieldsNotice
                  missingFields={targetMissing}
                  targetText="目标符合度"
                />
              </div>
            </section>
          )}
        </>
      ) : null}
    </section>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-black/20 px-4 py-3">
      <p className="text-xs text-stone-500">{label}</p>
      <p className="mt-1 font-semibold text-white">{value}</p>
    </div>
  );
}
