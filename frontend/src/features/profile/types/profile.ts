export type Gender = "male" | "female";

export type GoalType = "fat_loss" | "muscle_gain" | "health_maintenance";

export type TrainingLevel = "beginner" | "experienced";

export type ProfileTab = "basic" | "metrics";

export type AiCompletionScene = "ai-plan" | "ai-nutrition" | "ai-summary";

export type ProfileBasicResponse = {
  gender: Gender | null;
  birthDate: string | null;
  heightCm: number | null;
  goalType: GoalType | null;
  trainingLevel: TrainingLevel | null;
  injuryNotes: string | null;
  currentWeightKg: number | null;
  latestBodyMetricRecordDate: string | null;
};

export type UpdateProfileBasicPayload = {
  gender: Gender | null;
  birthDate: string | null;
  heightCm: number | null;
  goalType: GoalType | null;
  trainingLevel: TrainingLevel | null;
  injuryNotes: string | null;
};

export type BodyMetricSnapshotResponse = {
  currentWeightKg: number | null;
  currentBodyFatPercent: number | null;
  currentBmi: number | null;
  currentSkeletalMusclePercent: number | null;
  currentBodyWaterPercent: number | null;
  currentBasalMetabolicRateKcal: number | null;
  currentWaistCm: number | null;
  currentHipCm: number | null;
  currentWaistHipRatio: number | null;
  currentBodyAge: number | null;
  currentBodyType: string | null;
  updatedAt: string | null;
};

export type BodyMetricPageQuery = {
  page?: number;
  pageSize?: number;
};

export type CreateBodyMetricPayload = {
  recordDate: string;
  weightKg: number | null;
  bodyFatPercent: number | null;
  bmi: number | null;
  skeletalMusclePercent: number | null;
  bodyWaterPercent: number | null;
  basalMetabolicRateKcal: number | null;
  waistCm: number | null;
  hipCm: number | null;
  waistHipRatio: number | null;
  bodyAge: number | null;
  bodyType: string | null;
  note: string | null;
};

export type BodyMetricLogItemResponse = {
  id: number;
  recordDate: string;
  weightKg: number | null;
  bodyFatPercent: number | null;
  bmi: number | null;
  skeletalMusclePercent: number | null;
  bodyWaterPercent: number | null;
  basalMetabolicRateKcal: number | null;
  waistCm: number | null;
  hipCm: number | null;
  waistHipRatio: number | null;
  bodyAge: number | null;
  bodyType: string | null;
  note: string | null;
  isLatest: boolean;
};

export type BodyMetricsPageResponse = {
  page: number;
  pageSize: number;
  total: number;
  records: BodyMetricLogItemResponse[];
};

export type DeleteLatestBodyMetricResponse = {
  deletedId: number;
  deletedRecordDate: string;
  deletedWeightKg: number | null;
};

export type ProfileCompletionSummaryResponse = {
  basicProfileReady: boolean;
  hasWeightRecord: boolean;
  currentWeightKg: number | null;
  missingBasicProfileFields: string[];
  aiPlanReady: boolean;
  aiPlanMissingFields: string[];
  aiNutritionReady: boolean;
  aiNutritionMissingFields: string[];
  aiSummaryReady: boolean;
  aiSummaryMissingFields: string[];
};

export type BasicProfileFormValues = {
  gender: string;
  birthDate: string;
  heightCm: string;
  goalType: string;
  trainingLevel: string;
  injuryNotes: string;
};

export type BodyMetricFormValues = {
  weightKg: string;
  bodyFatPercent: string;
  bmi: string;
  skeletalMusclePercent: string;
  bodyWaterPercent: string;
  basalMetabolicRateKcal: string;
  waistCm: string;
  hipCm: string;
  waistHipRatio: string;
  bodyAge: string;
  bodyType: string;
  note: string;
};
