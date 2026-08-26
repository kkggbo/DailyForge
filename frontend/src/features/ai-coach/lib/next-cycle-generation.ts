import type { CycleTemplateDetailResponse } from "../../cycle-template/types/cycle-template";
import type { GoalType } from "../../profile/types/profile";
import type {
  SceneType,
  TemplateGenerationForm
} from "../types/ai-coach";

/**
 * 下一周期模板生成的预填数据，全部来自上轮模板（CycleTemplateDetailResponse）。
 * 仅作为内部 UI 预填模型，不参与网络契约。
 */
export type NextCyclePrefill = {
  templateName: string | null;
  goalType: GoalType | null;
  cycleLength: number | null;
  sceneType: SceneType | null;
  includeCardio: boolean | null;
};

export function isSceneType(value: string | null | undefined): value is SceneType {
  return value === "gym" || value === "home";
}

export function isGoalType(value: string | null | undefined): value is GoalType {
  return (
    value === "fat_loss" ||
    value === "muscle_gain" ||
    value === "health_maintenance"
  );
}

export function buildNextCyclePrefill(
  detail: CycleTemplateDetailResponse
): NextCyclePrefill {
  return {
    templateName: detail.templateName,
    goalType: isGoalType(detail.goalType) ? detail.goalType : null,
    cycleLength: detail.cycleLength,
    sceneType: isSceneType(detail.sceneType) ? detail.sceneType : null,
    includeCardio: detail.includeCardio ?? null
  };
}

/**
 * 把预填值转换为 TemplateGenerationForm 的 initialValues。
 * - cycleLength 由 number 字符串化为 cycleLengthText。
 * - additionalRequirements 初始为空（用户当下意图留空）。
 */
export function buildNextCycleInitialValues(
  prefill: NextCyclePrefill
): Partial<TemplateGenerationForm> {
  return {
    sceneType: prefill.sceneType ?? undefined,
    goalType: prefill.goalType ?? undefined,
    cycleLengthText:
      prefill.cycleLength != null ? String(prefill.cycleLength) : undefined,
    includeCardio: prefill.includeCardio ?? undefined,
    additionalRequirements: ""
  };
}
