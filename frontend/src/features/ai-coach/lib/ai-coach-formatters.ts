import {
  aiTaskProgressStageDescriptions,
  aiTaskProgressStageLabels,
  aiTaskStatusLabels,
  aiTaskToolCallStatusLabels,
  getGoalTypeLabel,
  getSceneTypeLabel,
  intensityBasisTypeLabels
} from "./ai-coach-enums";
import type {
  AiTaskProgressStage,
  AiTaskStatus,
  AiTaskToolCallStatus,
  IntensityBasisType,
  SceneType
} from "../types/ai-coach";

export function formatAiDateTime(value: string | null | undefined) {
  if (!value) {
    return "未记录";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

export function formatSceneType(value: SceneType) {
  return getSceneTypeLabel(value);
}

export function formatGoalType(value: string) {
  return getGoalTypeLabel(value);
}

export function formatAiTaskStatus(value: AiTaskStatus) {
  return aiTaskStatusLabels[value] ?? value;
}

export function formatAiTaskProgressStage(value: AiTaskProgressStage) {
  return aiTaskProgressStageLabels[value] ?? value;
}

export function formatAiTaskProgressDescription(value: AiTaskProgressStage) {
  return aiTaskProgressStageDescriptions[value] ?? value;
}

export function formatAiToolCallStatus(value: AiTaskToolCallStatus) {
  return aiTaskToolCallStatusLabels[value] ?? value;
}

export function formatIntensityBasisType(value: IntensityBasisType) {
  return intensityBasisTypeLabels[value] ?? value;
}

export function formatCycleLengthRange(
  minCycleLength: number,
  maxCycleLength: number
) {
  return `${minCycleLength} - ${maxCycleLength} 天`;
}
