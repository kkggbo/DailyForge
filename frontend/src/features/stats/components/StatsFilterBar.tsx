import { exerciseFilterOptions, timeRangeOptions } from "../lib/stats-options";
import type { TimeRangeSelection } from "../lib/stats-options";
import type { ExerciseFilter } from "../types/stats";

type StatsFilterBarProps = {
  range: TimeRangeSelection;
  onRangeChange: (range: TimeRangeSelection) => void;
  filter: ExerciseFilter;
  onFilterChange: (filter: ExerciseFilter) => void;
  search: string;
  onSearchChange: (search: string) => void;
  resultCount?: number;
};

export function StatsFilterBar({
  range,
  onRangeChange,
  filter,
  onFilterChange,
  search,
  onSearchChange,
  resultCount
}: StatsFilterBarProps) {
  return (
    <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
      <div className="flex flex-wrap items-center gap-3">
        <label className="text-sm text-stone-300">时间范围</label>
        <select
          value={range.preset}
          onChange={(event) =>
            onRangeChange({
              preset: event.target.value as TimeRangeSelection["preset"]
            })
          }
          className="rounded-full border border-white/10 bg-stone-950/70 px-4 py-2 text-sm text-white outline-none focus:border-amber-300/60"
        >
          {timeRangeOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        {range.preset === "custom" ? (
          <>
            <input
              type="date"
              value={range.from ?? ""}
              onChange={(event) =>
                onRangeChange({ ...range, from: event.target.value || undefined })
              }
              aria-label="开始日期"
              className="rounded-full border border-white/10 bg-stone-950/70 px-3 py-2 text-sm text-white outline-none"
            />
            <span className="text-sm text-stone-400">至</span>
            <input
              type="date"
              value={range.to ?? ""}
              onChange={(event) =>
                onRangeChange({ ...range, to: event.target.value || undefined })
              }
              aria-label="结束日期"
              className="rounded-full border border-white/10 bg-stone-950/70 px-3 py-2 text-sm text-white outline-none"
            />
          </>
        ) : null}
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2">
        {exerciseFilterOptions.map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => onFilterChange(option.value)}
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

        <input
          type="search"
          value={search}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="搜索动作名称"
          aria-label="搜索动作"
          className="ml-auto w-full max-w-xs rounded-full border border-white/10 bg-stone-950/70 px-4 py-2 text-sm text-white outline-none placeholder:text-stone-500 focus:border-amber-300/60"
        />
      </div>

      {typeof resultCount === "number" ? (
        <p className="mt-4 text-xs text-stone-400">共 {resultCount} 个动作</p>
      ) : null}
    </section>
  );
}
