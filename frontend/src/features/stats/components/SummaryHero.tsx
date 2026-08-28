import {
  formatDurationMinutes,
  formatDistanceKm,
  formatNumber,
  formatVolumeKg
} from "../lib/stats-formatters";
import type { OverallStats } from "../types/stats";

type SummaryHeroProps = {
  overall: OverallStats | null;
  isLoading: boolean;
  error: string | null;
};

export function SummaryHero({ overall, isLoading, error }: SummaryHeroProps) {
  return (
    <section className="rounded-[36px] border border-white/10 bg-white/6 p-8 backdrop-blur">
      <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
        Statistics
      </p>
      <h1 className="mt-4 text-4xl font-semibold leading-tight text-white sm:text-5xl">
        训练统计
      </h1>

      {isLoading ? (
        <div className="mt-6 flex items-center justify-center py-8">
          <span className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
            正在加载统计...
          </span>
        </div>
      ) : error ? (
        <div className="mt-6 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {error}
        </div>
      ) : !overall || overall.sessionCount === 0 ? (
        <div className="mt-6 rounded-3xl border border-dashed border-white/10 bg-black/20 px-5 py-8 text-center text-sm text-stone-400">
          还没有训练记录，去开始你的第一次训练打卡吧。
        </div>
      ) : (
        <div className="mt-6">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
            <HeroStat label="训练场数" value={formatNumber(overall.sessionCount)} />
            <HeroStat label="总组数" value={formatNumber(overall.totalSets)} />
            <HeroStat label="总次数" value={formatNumber(overall.totalReps)} />
            <HeroStat label="总容量" value={formatVolumeKg(overall.totalVolumeKg)} />
            <HeroStat label="总时长" value={formatDurationMinutes(overall.totalDurationMinutes)} />
          </div>
          <div className="mt-6 rounded-2xl border border-amber-300/20 bg-amber-300/10 px-5 py-4">
            <p className="text-sm font-medium text-white">总里程</p>
            <p className="mt-1 text-sm leading-6 text-stone-200">
              {formatDistanceKm(overall.totalDistanceKm)}
            </p>
          </div>
          {overall.overviewCopy ? (
            <p className="mt-6 rounded-2xl border border-white/10 bg-white/5 px-5 py-4 text-sm leading-7 text-stone-200">
              {overall.overviewCopy}
            </p>
          ) : null}
        </div>
      )}
    </section>
  );
}

function HeroStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
      <p className="text-xs uppercase tracking-[0.18em] text-stone-500">{label}</p>
      <p className="mt-2 text-lg font-semibold text-white">{value}</p>
    </div>
  );
}
