import { ApiRequestError } from "../../../shared/api/http";
import { goalTypeOptions } from "../../profile/lib/profile-enums";
import type {
  AiTaskProgressStage,
  AiTaskStatus,
  AiTaskToolCallStatus,
  IntensityBasisType,
  MissingFieldCode,
  SceneType
} from "../types/ai-coach";

export const sceneTypeOptions: Array<{ value: SceneType; label: string }> = [
  { value: "gym", label: "健身房" },
  { value: "home", label: "居家" }
];

export const missingFieldLabels: Record<MissingFieldCode, string> = {
  gender: "性别",
  birthDate: "出生日期",
  heightCm: "身高",
  goalType: "训练目标",
  trainingLevel: "训练经验",
  currentWeightKg: "当前体重"
};

export const aiTaskStatusLabels: Record<AiTaskStatus, string> = {
  pending: "等待处理",
  running: "AI 处理中",
  succeeded: "已完成",
  failed: "已失败"
};

export const aiTaskProgressStageLabels: Record<AiTaskProgressStage, string> = {
  queued: "排队中",
  generating_result: "生成结果中",
  calling_tool: "调用工具中",
  repairing_output: "修复输出中",
  completed: "已完成",
  failed: "已失败"
};

export const aiTaskProgressStageDescriptions: Record<
  AiTaskProgressStage,
  string
> = {
  queued: "任务已创建，正在等待 AI 执行器开始处理。",
  generating_result: "AI 正在整理结构化输出，结果页会继续自动刷新。",
  calling_tool: "AI 正在调用工具补充上下文，你会看到最近一次工具调用痕迹。",
  repairing_output: "AI 正在修复输出结构，确保结果能被系统正确解析。",
  completed: "任务已成功完成，可以查看最终结果。",
  failed: "任务执行失败，请根据错误信息重试。"
};

export const aiTaskToolCallStatusLabels: Record<AiTaskToolCallStatus, string> = {
  succeeded: "成功",
  failed: "失败"
};

export const intensityBasisTypeLabels: Record<IntensityBasisType, string> = {
  historical_performance: "基于历史训练表现",
  starting_recommendation: "基于起始建议"
};

export function getSceneTypeLabel(value: SceneType) {
  return sceneTypeOptions.find((option) => option.value === value)?.label ?? value;
}

export function getGoalTypeLabel(value: string) {
  return goalTypeOptions.find((option) => option.value === value)?.label ?? value;
}

export function getMissingFieldLabel(value: MissingFieldCode) {
  return missingFieldLabels[value] ?? value;
}

export function getAiCoachErrorMessage(error: unknown, fallback: string) {
  if (!(error instanceof ApiRequestError)) {
    return error instanceof Error ? error.message : fallback;
  }

  switch (error.code) {
    case "AI_FEATURE_NOT_AVAILABLE":
      return "当前账号暂未开通 AI Coach 功能。";
    case "AI_REQUIRED_PROFILE_MISSING":
      return "基础档案仍有缺失，暂时无法发起这项 AI 请求。";
    case "AI_REQUIRED_BODY_METRIC_MISSING":
      return "身体指标仍有缺失，暂时无法发起这项 AI 请求。";
    case "AI_CYCLE_RUN_NOT_COMPLETED":
      return "目标循环尚未完成，暂时不能生成周期总结。";
    case "AI_TASK_NOT_FOUND":
      return "没有找到这条 AI 任务，可能已失效或无权访问。";
    case "AI_OUTPUT_INVALID":
      return "AI 返回结果未通过系统校验，请稍后重试。";
    case "AI_SERVICE_TIMEOUT":
      return "AI 响应超时，请稍后再试。";
    case "AI_SERVICE_UNAVAILABLE":
      return "AI 服务暂时不可用，请稍后再试。";
    case "INVALID_ARGUMENT":
      return "提交参数不合法，请检查后重新提交。";
    default:
      return error.message || fallback;
  }
}
