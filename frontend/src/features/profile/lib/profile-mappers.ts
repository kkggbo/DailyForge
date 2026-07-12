import { getProfileFieldLabel } from "./profile-enums";
import type {
  AiCompletionScene,
  BasicProfileFormValues,
  BodyMetricFormValues,
  ProfileBasicResponse,
  ProfileCompletionSummaryResponse,
  UpdateProfileBasicPayload
} from "../types/profile";

export function toBasicProfileFormValues(
  profile?: ProfileBasicResponse | UpdateProfileBasicPayload | null
): BasicProfileFormValues {
  return {
    gender: profile?.gender ?? "",
    birthDate: profile?.birthDate ?? "",
    heightCm:
      profile?.heightCm === null || profile?.heightCm === undefined
        ? ""
        : String(profile.heightCm),
    goalType: profile?.goalType ?? "",
    trainingLevel: profile?.trainingLevel ?? "",
    injuryNotes: profile?.injuryNotes ?? ""
  };
}

export function createDefaultBodyMetricFormValues(
  recordDate = getTodayDateString()
): BodyMetricFormValues {
  return {
    recordDate,
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

function getTodayDateString() {
  return new Date().toISOString().slice(0, 10);
}
