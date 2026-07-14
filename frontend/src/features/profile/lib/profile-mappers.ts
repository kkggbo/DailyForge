import { getProfileFieldLabel } from "./profile-enums";
import type {
  AiCompletionScene,
  BasicProfileFormValues,
  BodyMetricSnapshotResponse,
  BodyMetricFormValues,
  ProfileBasicResponse,
  ProfileCompletionSummaryResponse,
  UpdateProfileBasicPayload
} from "../types/profile";

export const DEFAULT_BIRTH_DATE = "2000-01-01";

export function toBasicProfileFormValues(
  profile?: ProfileBasicResponse | UpdateProfileBasicPayload | null
): BasicProfileFormValues {
  return {
    gender: profile?.gender ?? "",
    birthDate: profile?.birthDate ?? DEFAULT_BIRTH_DATE,
    heightCm:
      profile?.heightCm === null || profile?.heightCm === undefined
        ? ""
        : String(profile.heightCm),
    goalType: profile?.goalType ?? "",
    trainingLevel: profile?.trainingLevel ?? "",
    injuryNotes: profile?.injuryNotes ?? ""
  };
}

export function createDefaultBodyMetricFormValues(): BodyMetricFormValues {
  return {
    weightKg: "",
    bodyFatPercent: "",
    bmi: "",
    skeletalMusclePercent: "",
    bodyWaterPercent: "",
    basalMetabolicRateKcal: "",
    waistCm: "",
    hipCm: "",
    waistHipRatio: "",
    bodyAge: "",
    bodyType: "",
    note: ""
  };
}

export function toBodyMetricFormValues(
  snapshot?: BodyMetricSnapshotResponse | null
): BodyMetricFormValues {
  if (!snapshot) {
    return createDefaultBodyMetricFormValues();
  }

  return {
    weightKg: toOptionalNumberString(snapshot.currentWeightKg),
    bodyFatPercent: toOptionalNumberString(snapshot.currentBodyFatPercent),
    bmi: toOptionalNumberString(snapshot.currentBmi),
    skeletalMusclePercent: toOptionalNumberString(snapshot.currentSkeletalMusclePercent),
    bodyWaterPercent: toOptionalNumberString(snapshot.currentBodyWaterPercent),
    basalMetabolicRateKcal: toOptionalNumberString(snapshot.currentBasalMetabolicRateKcal),
    waistCm: toOptionalNumberString(snapshot.currentWaistCm),
    hipCm: toOptionalNumberString(snapshot.currentHipCm),
    waistHipRatio: toOptionalNumberString(snapshot.currentWaistHipRatio),
    bodyAge:
      snapshot.currentBodyAge === null || snapshot.currentBodyAge === undefined
        ? ""
        : String(snapshot.currentBodyAge),
    bodyType: snapshot.currentBodyType ?? "",
    note: ""
  };
}

export function getMissingFieldsForScene(
  summary: ProfileCompletionSummaryResponse,
  scene: AiCompletionScene
) {
  switch (scene) {
    case "ai-plan":
      return summary.aiPlanMissingFields;
    case "ai-nutrition":
      return summary.aiNutritionMissingFields;
    case "ai-summary":
      return summary.aiSummaryMissingFields;
    default:
      return summary.aiPlanMissingFields;
  }
}

export function getSceneReady(
  summary: ProfileCompletionSummaryResponse,
  scene: AiCompletionScene
) {
  switch (scene) {
    case "ai-plan":
      return summary.aiPlanReady;
    case "ai-nutrition":
      return summary.aiNutritionReady;
    case "ai-summary":
      return summary.aiSummaryReady;
    default:
      return summary.aiPlanReady;
  }
}

export function mapMissingFieldsToLabels(fields: string[]) {
  if (fields.length === 0) {
    return [];
  }

  return fields.map((field) => getProfileFieldLabel(field));
}

export function shouldStartFromMetricStep(fields: string[]) {
  if (fields.length === 0) {
    return false;
  }

  return fields.every((field) => field === "weightKg");
}

export function normalizeRedirectPath(redirect: string | null) {
  if (!redirect || !redirect.startsWith("/")) {
    return "/profile";
  }

  if (redirect.startsWith("//")) {
    return "/profile";
  }

  return redirect;
}

export function getLocalTodayDateString() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function toOptionalNumberString(value: number | null | undefined) {
  return value === null || value === undefined ? "" : String(value);
}
