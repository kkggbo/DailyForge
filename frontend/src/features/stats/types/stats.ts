export type ExerciseType = "strength" | "cardio";

// 前端筛选：全部 / 仅力量 / 仅有氧
export type ExerciseFilter = "all" | ExerciseType;

export type OverallStats = {
  sessionCount: number;
  totalSets: number;
  totalReps: number;
  totalVolumeKg: number;
  totalDistanceKm: number;
  totalDurationMinutes: number;
  overviewCopy: string;
};

export type ExerciseStat = {
  exerciseId: number;
  name: string;
  exerciseType: ExerciseType;
  structureType: "set_based" | "single_segment";
  appearanceCount: number;
  setCount: number | null;
  repCount: number | null;
  totalVolumeKg: number | null;
  avgWeightKg: number | null;
  maxWeightKg: number | null;
  avgReps: number | null;
  totalDurationSeconds: number | null;
  totalDistanceKm: number | null;
  avgSpeedKmh: number | null;
  funCopy: string;
};

export type StatsSummary = {
  overall: OverallStats;
  exercises: ExerciseStat[];
};

export type ExerciseProgressionPoint = {
  date: string;
  maxWeightKg: number | null;
  maxReps: number | null;
  totalVolumeKg: number | null;
  totalDurationSeconds: number | null;
  totalDistanceKm: number | null;
};

export type ExerciseProgression = ExerciseStat & {
  progression: ExerciseProgressionPoint[];
};

export type BodyMetricKey =
  | "weight_kg"
  | "body_fat_percent"
  | "bmi"
  | "skeletal_muscle_percent"
  | "body_water_percent"
  | "basal_metabolic_rate_kcal"
  | "waist_cm"
  | "hip_cm"
  | "waist_hip_ratio"
  | "body_age";

export type BodyMetricPoint = {
  date: string;
  value: number;
};

export type BodyMetricsSeries = {
  metric: BodyMetricKey;
  unit: string;
  points: BodyMetricPoint[];
};

export type StatsTimeRangeQuery = {
  from?: string;
  to?: string;
};
