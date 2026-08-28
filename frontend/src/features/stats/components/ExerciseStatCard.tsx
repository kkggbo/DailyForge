import { ProgressionChart } from "./ProgressionChart";
import {
  formatAvgSpeedKmh,
  formatDurationSeconds,
  formatNumber,
  formatVolumeKg
} from "../lib/stats-formatters";
import { getExerciseTypeLabel } from "../lib/stats-mappers";
import type {
  ExerciseProgression,
  ExerciseStat
} from "../types/stats";

type ExerciseStatCardProps = {
  exercise: ExerciseStat;
  expanded: boolean;
  onToggleExpand: () => void;
  progression: ExerciseProgression | null;
  isProgressionLoading: boolean;
  progressionError: string | null;
  onRetryProgression: () => void;
};

export function ExerciseStatCard({
  exercise,
  expanded,
  onToggleExpand,
  progression,
  isProgressionLoading,
  progressionError,
  onRetryProgression
}: ExerciseStatCardProps) {
  const isStrength = exercise.structureType === "set_based";

  const badgeItems = [
    {
      label: "出现次数",
      value: formatNumber(exercise.appearanceCount),
      show: isNonZero(exercise.appearanceCount)
    },
    {
      label: "总组数",
      value: formatNumber(exercise.setCount),
      show: isNonZero(exercise.setCount)
    },
    {
      label: "总次数",
      value: formatNumber(exercise.repCount),
      show: isNonZero(exercise.repCount)
    }
  ].filter((item) => item.show);

  const metricItems = (
    isStrength
      ? [
          {
            label: "总容量",
            text: formatVolumeKg(exercise.totalVolumeKg),
            show: isNonZero(exercise.totalVolumeKg)
          },
          {
            label: "平均重量",
            text: formatVolumeKg(exercise.avgWeightKg),
            show: isNonZero(exercise.avgWeightKg)
          },
          {
            label: "最大重量",
            text: formatVolumeKg(exercise.maxWeightKg),
            show: isNonZero(exercise.maxWeightKg)
          },
          {
            label: "平均次数",
            text: formatNumber(exercise.avgReps),
            show: isNonZero(exercise.avgReps)
          }
        ]
      : [
          {
            label: "总时长",
            text: formatDurationSeconds(exercise.totalDurationSeconds),
            show: isNonZero(exercise.totalDurationSeconds)
          },
          {
            label: "平均配速",
            text: formatAvgSpeedKmh(exercise.avgSpeedKmh),
            show: isNonZero(exercise.avgSpeedKmh)
          }
        ]
  ).filter((item) => item.show);

  return (
    <article className="rounded-3xl border border-white/10 bg-black/20 p-5">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="text-lg font-semibold text-white">{exercise.name}</h3>
            <span className="rounded-full border border-white/10 bg-white/8 px-3 py-1 text-xs text-stone-300">
              {getExerciseTypeLabel(exercise.exerciseType)}
            </span>
          </div>
          {badgeItems.length > 0 ? (
            <div className="mt-3 flex flex-wrap gap-2 text-xs text-stone-200">
              {badgeItems.map((item) => (
                <Badge key={item.label} label={item.label} value={item.value} />
              ))}
            </div>
          ) : null}
        </div>

        <button
          type="button"
          onClick={onToggleExpand}
          className="inline-flex items-center self-start rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
        >
          {expanded ? "收起进阶" : "查看进阶"}
        </button>
      </div>

      {metricItems.length > 0 ? (
        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {metricItems.map((item) => (
            <Metric key={item.label} label={item.label} value={item.text} />
          ))}
        </div>
      ) : null}

      {exercise.funCopy ? (
        <p className="mt-4 rounded-2xl border border-amber-300/20 bg-amber-300/10 px-4 py-3 text-sm leading-6 text-amber-100">
          {exercise.funCopy}
        </p>
      ) : null}

      {expanded ? (
        <div className="mt-5 border-t border-white/10 pt-5">
          {isProgressionLoading ? (
            <div className="flex h-40 items-center justify-center text-sm text-stone-400">
              正在加载进阶数据...
            </div>
          ) : progressionError ? (
            <div className="flex flex-col items-center gap-3 py-6 text-center">
              <p className="text-sm text-rose-100">{progressionError}</p>
              <button
                type="button"
                onClick={onRetryProgression}
                className="rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
              >
                重试
              </button>
            </div>
          ) : progression ? (
            <ProgressionChart
              points={progression.progression}
              exerciseType={exercise.exerciseType}
            />
          ) : null}
        </div>
      ) : null}
    </article>
  );
}

function Badge({ label, value }: { label: string; value: string }) {
  return (
    <span className="rounded-full border border-white/10 bg-white/8 px-3 py-1">
      {label} {value}
    </span>
  );
}

function isNonZero(value: number | null | undefined): boolean {
  return value != null && value > 0;
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
      <p className="text-xs uppercase tracking-[0.18em] text-stone-500">{label}</p>
      <p className="mt-1 text-sm font-medium text-white">{value}</p>
    </div>
  );
}
