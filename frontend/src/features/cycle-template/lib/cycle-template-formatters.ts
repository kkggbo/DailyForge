import {
  cycleTemplateStatusLabels,
  goalTypeOptions,
  itemTypeLabels,
  structureTypeLabels
} from "./cycle-template-enums";
import { getMetricMeta } from "./cycle-template-metric-config";
import type {
  CycleTemplateMetricResponse,
  CycleTemplateStatus,
  ItemType,
  MetricKey,
  StructureType
} from "../types/cycle-template";

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

export function formatStructureType(value: StructureType) {
  return structureTypeLabels[value] ?? value;
}

export function formatItemType(value: ItemType) {
  return itemTypeLabels[value] ?? value;
}

export function formatMetricKey(metricKey: MetricKey) {
  return getMetricMeta(metricKey)?.label ?? metricKey;
}

export function formatMetricValue(metric: CycleTemplateMetricResponse) {
  const meta = getMetricMeta(metric.metricKey);
  const unitLabel = metric.metricUnit ?? meta?.unitLabel;

  if (unitLabel) {
    return `${metric.metricValueNumber} ${unitLabel}`;
  }

  return String(metric.metricValueNumber);
}
