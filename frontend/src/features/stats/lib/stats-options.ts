import type {
  BodyMetricKey,
  ExerciseFilter,
  ExerciseType
} from "../types/stats";

// 本地 UI 类型（不参与网络契约）
export type TimeRangePreset = "30d" | "90d" | "year" | "all" | "custom";

export type TimeRangeSelection = {
  preset: TimeRangePreset;
  from?: string;
  to?: string;
};

export const defaultTrainingRange: TimeRangeSelection = { preset: "all" };
export const defaultBodyMetricRange: TimeRangeSelection = { preset: "all" };

export const timeRangeOptions: Array<{ value: TimeRangePreset; label: string }> = [
  { value: "30d", label: "近 30 天" },
  { value: "90d", label: "近 90 天" },
  { value: "year", label: "今年" },
  { value: "all", label: "全部" },
  { value: "custom", label: "自定义" }
];

export const exerciseFilterOptions: Array<{
  value: ExerciseFilter;
  label: string;
}> = [
  { value: "all", label: "全部动作" },
  { value: "strength", label: "仅力量" },
  { value: "cardio", label: "仅有氧" }
];

export const bodyMetricOptions: Array<{
  value: BodyMetricKey;
  label: string;
  unit: string;
}> = [
  { value: "weight_kg", label: "体重", unit: "kg" },
  { value: "body_fat_percent", label: "体脂率", unit: "%" },
  { value: "bmi", label: "BMI", unit: "" },
  { value: "skeletal_muscle_percent", label: "骨骼肌率", unit: "%" },
  { value: "body_water_percent", label: "体水分率", unit: "%" },
  { value: "basal_metabolic_rate_kcal", label: "基础代谢", unit: "kcal" },
  { value: "waist_cm", label: "腰围", unit: "cm" },
  { value: "hip_cm", label: "臀围", unit: "cm" },
  { value: "waist_hip_ratio", label: "腰臀比", unit: "" },
  { value: "body_age", label: "身体年龄", unit: "岁" }
];

export const exerciseTypeLabels: Record<ExerciseType, string> = {
  strength: "力量",
  cardio: "有氧"
};

const DAY_MS = 24 * 60 * 60 * 1000;

export function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

/**
 * 根据预设时间范围与自定义 from/to 生成查询参数。
 * "all" 返回空对象（不限时间）。
 */
export function buildTimeRangeQuery(
  range: TimeRangeSelection
): { from?: string; to?: string } {
  if (range.preset === "all") {
    return {};
  }

  if (range.preset === "custom") {
    return {
      ...(range.from ? { from: range.from } : {}),
      ...(range.to ? { to: range.to } : {})
    };
  }

  const now = new Date();
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);

  let start: Date;
  if (range.preset === "30d") {
    start = new Date(end.getTime() - 29 * DAY_MS);
  } else if (range.preset === "90d") {
    start = new Date(end.getTime() - 89 * DAY_MS);
  } else {
    start = new Date(now.getFullYear(), 0, 1);
  }

  return {
    from: toIsoDate(start),
    to: toIsoDate(end)
  };
}
