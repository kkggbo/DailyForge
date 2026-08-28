import { bodyMetricOptions, exerciseTypeLabels } from "./stats-options";
import type {
  BodyMetricKey,
  ExerciseFilter,
  ExerciseProgressionPoint,
  ExerciseStat,
  ExerciseType
} from "../types/stats";

/**
 * 按动作类型筛选 + 名称搜索过滤动作列表。
 * 时间范围过滤由后端负责；这里只做前端本地过滤。
 */
export function filterExercises(
  exercises: ExerciseStat[],
  filter: ExerciseFilter,
  search: string
): ExerciseStat[] {
  const keyword = search.trim().toLowerCase();

  return exercises.filter((exercise) => {
    if (filter !== "all" && exercise.exerciseType !== filter) {
      return false;
    }

    if (
      keyword &&
      !(exercise.name ?? "").toLowerCase().includes(keyword)
    ) {
      return false;
    }

    return true;
  });
}

export function getExerciseTypeLabel(type: ExerciseType): string {
  return exerciseTypeLabels[type] ?? type;
}

export function getBodyMetricLabel(key: BodyMetricKey): string {
  return (
    bodyMetricOptions.find((option) => option.value === key)?.label ?? key
  );
}

export function getBodyMetricUnit(key: BodyMetricKey): string {
  return (
    bodyMetricOptions.find((option) => option.value === key)?.unit ?? ""
  );
}

/**
 * 指标展示名，单位非空时追加「（单位）」，如「体重（kg）」「BMI」。
 */
export function getBodyMetricLabelWithUnit(key: BodyMetricKey): string {
  const option = bodyMetricOptions.find((item) => item.value === key);
  if (!option) {
    return key;
  }
  return option.unit ? `${option.label}（${option.unit}）` : option.label;
}

export type ProgressionMetricKind =
  | "maxWeightKg"
  | "maxReps"
  | "totalVolumeKg"
  | "totalDurationSeconds"
  | "totalDistanceKm";

export type ProgressionMetricOption = {
  key: ProgressionMetricKind;
  label: string;
  unit: string;
};

const strengthProgressionOptions: ProgressionMetricOption[] = [
  { key: "maxWeightKg", label: "最大重量", unit: "kg" },
  { key: "maxReps", label: "最大次数", unit: "次" },
  { key: "totalVolumeKg", label: "总容量", unit: "kg" }
];

const cardioProgressionOptions: ProgressionMetricOption[] = [
  { key: "totalDistanceKm", label: "总距离", unit: "km" },
  { key: "totalDurationSeconds", label: "总时长", unit: "分" }
];

export function getProgressionOptions(
  exerciseType: ExerciseType
): ProgressionMetricOption[] {
  return exerciseType === "strength"
    ? strengthProgressionOptions
    : cardioProgressionOptions;
}

export function getDefaultProgressionMetric(
  exerciseType: ExerciseType
): ProgressionMetricKind {
  return exerciseType === "strength" ? "maxWeightKg" : "totalDistanceKm";
}

export function getProgressionValue(
  point: ExerciseProgressionPoint,
  key: ProgressionMetricKind
): number | null {
  return point[key] ?? null;
}
