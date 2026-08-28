import { useMemo } from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import {
  bodyMetricOptions,
  timeRangeOptions,
  type TimeRangeSelection
} from "../lib/stats-options";
import { formatDateShort, formatNumber } from "../lib/stats-formatters";
import { getBodyMetricLabelWithUnit } from "../lib/stats-mappers";
import type { BodyMetricKey, BodyMetricsSeries } from "../types/stats";

const metricColors = [
  "#fbbf24",
  "#38bdf8",
  "#a3e635",
  "#f472b6",
  "#c084fc",
  "#34d399",
  "#fb923c",
  "#22d3ee",
  "#e879f9",
  "#facc15"
];

type BodyMetricsChartProps = {
  seriesMap: Partial<Record<BodyMetricKey, BodyMetricsSeries>>;
  isLoading: boolean;
  error: string | null;
  selectedMetrics: Set<BodyMetricKey>;
  range: TimeRangeSelection;
  onToggleMetric: (metric: BodyMetricKey) => void;
  onSelectAll: () => void;
  onClearAll: () => void;
  onRangeChange: (range: TimeRangeSelection) => void;
};

export function BodyMetricsChart({
  seriesMap,
  isLoading,
  error,
  selectedMetrics,
  range,
  onToggleMetric,
  onSelectAll,
  onClearAll,
  onRangeChange
}: BodyMetricsChartProps) {
  const renderedMetrics = bodyMetricOptions.filter(
    (option) =>
      selectedMetrics.has(option.value) &&
      (seriesMap[option.value]?.points.length ?? 0) > 0
  );

  const chartData = useMemo(() => {
    const byDate: Record<string, Record<string, number>> = {};
    const dateSet = new Set<string>();

    for (const option of bodyMetricOptions) {
      if (!selectedMetrics.has(option.value)) {
        continue;
      }

      const series = seriesMap[option.value];
      if (!series) {
        continue;
      }

      for (const point of series.points) {
        dateSet.add(point.date);
        byDate[point.date] = byDate[point.date] ?? {};
        byDate[point.date]![option.value] = point.value;
      }
    }

    const rows: Array<Record<string, string | number>> = [];
    for (const date of Array.from(dateSet).sort()) {
      rows.push({ date, ...(byDate[date] ?? {}) });
    }
    return rows;
  }, [seriesMap, selectedMetrics]);

  return (
    <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
            Body Metrics
          </p>
          <h2 className="mt-2 text-2xl font-semibold text-white">身体指标趋势</h2>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={onSelectAll}
            className="rounded-full border border-white/10 bg-white/8 px-3 py-2 text-xs font-semibold text-stone-100 transition hover:bg-white/12"
          >
            勾选全部
          </button>
          <button
            type="button"
            onClick={onClearAll}
            className="rounded-full border border-white/10 bg-white/8 px-3 py-2 text-xs font-semibold text-stone-100 transition hover:bg-white/12"
          >
            取消全部
          </button>
          <RangePresetSelect value={range} onChange={onRangeChange} />
        </div>
      </div>

      <div className="mt-4 flex flex-wrap gap-x-5 gap-y-2">
        {bodyMetricOptions.map((option, index) => (
          <label
            key={option.value}
            className="inline-flex cursor-pointer items-center gap-2 text-sm text-stone-200"
          >
            <input
              type="checkbox"
              checked={selectedMetrics.has(option.value)}
              onChange={() => onToggleMetric(option.value)}
              className="h-4 w-4 accent-amber-400"
            />
            <span
              className="inline-block h-2 w-2 rounded-full"
              style={{ backgroundColor: metricColors[index % metricColors.length] }}
            />
            {getBodyMetricLabelWithUnit(option.value)}
          </label>
        ))}
      </div>

      {range.preset === "custom" ? (
        <CustomRangeInputs range={range} onChange={onRangeChange} />
      ) : null}

      <div className="mt-6">
        {isLoading ? (
          <div className="flex h-64 items-center justify-center">
            <span className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
              正在加载身体指标...
            </span>
          </div>
        ) : error ? (
          <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
            {error}
          </div>
        ) : selectedMetrics.size === 0 ? (
          <div className="flex h-64 items-center justify-center rounded-2xl border border-dashed border-white/10 bg-black/20 text-sm text-stone-400">
            还没有选中任何身体指标，请勾选上方指标。
          </div>
        ) : renderedMetrics.length === 0 ? (
          <div className="flex h-64 items-center justify-center rounded-2xl border border-dashed border-white/10 bg-black/20 text-sm text-stone-400">
            该时间范围内暂无身体指标记录。
          </div>
        ) : (
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart
                data={chartData}
                margin={{ top: 8, right: 16, bottom: 8, left: 0 }}
              >
                <CartesianGrid stroke="rgba(255,255,255,0.08)" vertical={false} />
                <XAxis
                  dataKey="date"
                  tickFormatter={(date: string) => formatDateShort(date)}
                  stroke="#a8a29e"
                  tick={{ fill: "#a8a29e", fontSize: 12 }}
                />
                <YAxis
                  stroke="#a8a29e"
                  tick={{ fill: "#a8a29e", fontSize: 12 }}
                  width={56}
                />
                <Tooltip
                  labelFormatter={(date: string) => formatDateShort(date)}
                  formatter={(value: number, name: string) => [
                    `${formatNumber(value)}`,
                    name
                  ]}
                  contentStyle={{
                    backgroundColor: "#1c1917",
                    border: "1px solid rgba(255,255,255,0.12)",
                    borderRadius: 12,
                    color: "#f5f5f4"
                  }}
                />
                {renderedMetrics.map((option) => {
                  const colorIndex = bodyMetricOptions.findIndex(
                    (o) => o.value === option.value
                  );
                  const color =
                    metricColors[colorIndex % metricColors.length] ?? "#fbbf24";
                  return (
                    <Line
                      key={option.value}
                      type="monotone"
                      dataKey={option.value}
                      name={getBodyMetricLabelWithUnit(option.value)}
                      stroke={color}
                      strokeWidth={2}
                      connectNulls={false}
                      dot={{ r: 3, fill: color }}
                      activeDot={{ r: 5 }}
                    />
                  );
                })}
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </section>
  );
}

function RangePresetSelect({
  value,
  onChange
}: {
  value: TimeRangeSelection;
  onChange: (range: TimeRangeSelection) => void;
}) {
  return (
    <select
      value={value.preset}
      onChange={(event) =>
        onChange({ preset: event.target.value as TimeRangeSelection["preset"] })
      }
      className="rounded-full border border-white/10 bg-stone-950/70 px-4 py-2 text-sm text-white outline-none focus:border-amber-300/60"
    >
      {timeRangeOptions.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  );
}

function CustomRangeInputs({
  range,
  onChange
}: {
  range: TimeRangeSelection;
  onChange: (range: TimeRangeSelection) => void;
}) {
  return (
    <div className="mt-4 flex flex-wrap items-center gap-3">
      <label className="flex items-center gap-2 text-sm text-stone-300">
        从
        <input
          type="date"
          value={range.from ?? ""}
          onChange={(event) =>
            onChange({ ...range, from: event.target.value || undefined })
          }
          className="rounded-full border border-white/10 bg-stone-950/70 px-3 py-2 text-sm text-white outline-none"
        />
      </label>
      <label className="flex items-center gap-2 text-sm text-stone-300">
        到
        <input
          type="date"
          value={range.to ?? ""}
          onChange={(event) =>
            onChange({ ...range, to: event.target.value || undefined })
          }
          className="rounded-full border border-white/10 bg-stone-950/70 px-3 py-2 text-sm text-white outline-none"
        />
      </label>
    </div>
  );
}
