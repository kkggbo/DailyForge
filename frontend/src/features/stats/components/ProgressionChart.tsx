import { useMemo, useState } from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import { formatDateShort, formatNumber } from "../lib/stats-formatters";
import {
  getDefaultProgressionMetric,
  getProgressionOptions,
  getProgressionValue
} from "../lib/stats-mappers";
import type { ExerciseProgressionPoint, ExerciseType } from "../types/stats";

type ProgressionChartProps = {
  points: ExerciseProgressionPoint[];
  exerciseType: ExerciseType;
};

export function ProgressionChart({
  points,
  exerciseType
}: ProgressionChartProps) {
  const options = getProgressionOptions(exerciseType);
  const [metricKey, setMetricKey] = useState(() =>
    getDefaultProgressionMetric(exerciseType)
  );

  const selectedOption =
    options.find((option) => option.key === metricKey) ?? options[0];

  const data = useMemo(
    () =>
      points.map((point) => ({
        date: point.date,
        value: getProgressionValue(point, selectedOption?.key ?? "maxWeightKg")
      })),
    [points, selectedOption]
  );

  if (!selectedOption) {
    return (
      <p className="py-8 text-center text-sm text-stone-400">暂无进阶数据。</p>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        {options.map((option) => (
          <button
            key={option.key}
            type="button"
            onClick={() => setMetricKey(option.key)}
            className={[
              "rounded-full px-3 py-1.5 text-xs font-semibold transition",
              option.key === selectedOption.key
                ? "bg-amber-400 text-stone-950"
                : "border border-white/10 bg-white/8 text-stone-200 hover:bg-white/12"
            ].join(" ")}
          >
            {option.label}
          </button>
        ))}
      </div>

      <div className="h-64 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
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
              width={48}
            />
            <Tooltip
              labelFormatter={(date: string) => formatDateShort(date)}
              formatter={(value: number) => [
                `${formatNumber(value)} ${selectedOption.unit}`,
                selectedOption.label
              ]}
              contentStyle={{
                backgroundColor: "#1c1917",
                border: "1px solid rgba(255,255,255,0.12)",
                borderRadius: 12,
                color: "#f5f5f4"
              }}
            />
            <Line
              type="monotone"
              dataKey="value"
              stroke="#fbbf24"
              strokeWidth={2}
              connectNulls
              dot={{ r: 3, fill: "#fbbf24" }}
              activeDot={{ r: 5 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
