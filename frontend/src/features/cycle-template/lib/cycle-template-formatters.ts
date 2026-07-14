import { cycleTemplateStatusLabels, goalTypeOptions } from "./cycle-template-enums";
import type { CycleTemplateStatus } from "../types/cycle-template";

export function formatDateTime(value: string | null | undefined) {
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

export function formatCycleLength(value: number | null | undefined) {
  return value ? `${value} 天循环` : "未设置周期";
}

export function formatGoalType(value: string | null | undefined) {
  const option = goalTypeOptions.find((item) => item.value === (value ?? ""));
  return option?.label ?? value ?? "未设置";
}

export function formatStatus(value: CycleTemplateStatus) {
  return cycleTemplateStatusLabels[value] ?? value;
}

export function formatNullableNumber(value: number | null | undefined, suffix = "") {
  if (value === null || value === undefined) {
    return "-";
  }

  return `${value}${suffix}`;
}
