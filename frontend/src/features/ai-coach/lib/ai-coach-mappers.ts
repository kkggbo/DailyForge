import type { AiCompletionScene } from "../../profile/types/profile";
import type { CycleTemplateDetailResponse } from "../../cycle-template/types/cycle-template";
import type {
  GeneratedDraftTemplate,
  TemplateGenerationCapability,
  TemplateGenerationForm
} from "../types/ai-coach";

export function createDefaultTemplateGenerationForm(
  capability: TemplateGenerationCapability
): TemplateGenerationForm | null {
  const defaultSceneType = capability.allowedSceneTypes[0];
  const defaultGoalType = capability.allowedGoalTypes[0];

  if (!defaultSceneType || !defaultGoalType) {
    return null;
  }

  const defaultCycleLength = clampCycleLength(
    4,
    capability.minCycleLength,
    capability.maxCycleLength
  );

  return {
    sceneType: defaultSceneType,
    goalType: defaultGoalType,
    cycleLengthText: String(defaultCycleLength),
    includeCardio: true,
    additionalRequirements: ""
  };
}

export function mapGeneratedDraftTemplateToDetail(
  draftTemplate: GeneratedDraftTemplate
): CycleTemplateDetailResponse {
  return {
    templateId: draftTemplate.templateId,
    templateName: draftTemplate.templateName,
    goalType: null,
    status: draftTemplate.templateStatus,
    sourceType: "ai_generated",
    cycleLength: draftTemplate.cycleLength,
    isActive: false,
    currentDayIndex: null,
    editableFromDayIndex: 1,
    canActivate: false,
    canDelete: false,
    createdAt: null,
    updatedAt: null,
    days: draftTemplate.days.map((day) => ({
      ...day,
      isLocked: false
    }))
  };
}

export function buildProfileAiCompletionPath(
  scene: AiCompletionScene,
  redirectPath: string
) {
  const params = new URLSearchParams({
    scene,
    redirect: redirectPath
  });

  return `/profile/ai-completion?${params.toString()}`;
}

export function normalizeOptionalText(value: string) {
  const normalized = value.trim();
  return normalized.length > 0 ? normalized : null;
}

function clampCycleLength(
  value: number,
  minCycleLength: number,
  maxCycleLength: number
) {
  return Math.min(Math.max(value, minCycleLength), maxCycleLength);
}
