import { getMetricMeta } from "./cycle-template-metric-config";
import type {
  CycleTemplateEditorForm,
  CycleTemplateFieldErrors,
  StructureType
} from "../types/cycle-template";

export function validateCycleTemplateEditor(
  form: CycleTemplateEditorForm,
  options: { allowEmptyCycleLength: boolean } = {
    allowEmptyCycleLength: false
  }
) {
  const fieldErrors: CycleTemplateFieldErrors = {};

  if (!form.templateName.trim()) {
    fieldErrors.templateName = "请输入模板名称。";
  } else if (form.templateName.trim().length > 128) {
    fieldErrors.templateName = "模板名称不能超过 128 个字符。";
  }

  if (!options.allowEmptyCycleLength || form.cycleLength.trim()) {
    const cycleLength = Number(form.cycleLength);
    if (!Number.isInteger(cycleLength) || cycleLength < 1 || cycleLength > 7) {
      fieldErrors.cycleLength = "周期长度必须是 1 到 7 之间的整数。";
    }
  }

  if (form.goalType.trim().length > 32) {
    fieldErrors.goalType = "训练目标不能超过 32 个字符。";
  }

  const maxDayIndex = Number(form.cycleLength);

  for (const [dayPosition, day] of form.days.entries()) {
    if (day.dayName.trim().length > 64) {
      fieldErrors[`day.${dayPosition}.dayName`] = "训练日名称不能超过 64 个字符。";
    }

    if (
      Number.isInteger(maxDayIndex) &&
      maxDayIndex >= 1 &&
      maxDayIndex <= 7 &&
      day.dayIndex > maxDayIndex
    ) {
      fieldErrors[`day.${dayPosition}.dayIndex`] = "训练日超出了当前周期长度。";
    }

    for (const [exercisePosition, exercise] of day.exercises.entries()) {
      const exercisePrefix = `day.${dayPosition}.exercise.${exercisePosition}`;

      if (!exercise.exerciseId) {
        fieldErrors[`${exercisePrefix}.exerciseId`] = "还没有选择系统动作。";
      }

      if (!exercise.structureType) {
        fieldErrors[`${exercisePrefix}.structureType`] = "动作结构类型缺失。";
      }

      if (exercise.note.trim().length > 500) {
        fieldErrors[`${exercisePrefix}.note`] = "动作备注不能超过 500 个字符。";
      }

      if (exercise.items.length === 0) {
        fieldErrors[`${exercisePrefix}.items`] = "当前动作至少需要 1 个执行项。";
        continue;
      }

      if (exercise.structureType === "single_segment" && exercise.items.length !== 1) {
        fieldErrors[`${exercisePrefix}.items`] = "单段动作只能保留 1 个执行项。";
      }

      for (const [itemPosition, item] of exercise.items.entries()) {
        const itemPrefix = `${exercisePrefix}.item.${itemPosition}`;
        validateItem(fieldErrors, itemPrefix, exercise.structureType, item);
      }
    }
  }

  return fieldErrors;
}

function validateItem(
  fieldErrors: CycleTemplateFieldErrors,
  itemPrefix: string,
  structureType: StructureType | null,
  item: CycleTemplateEditorForm["days"][number]["exercises"][number]["items"][number]
) {
  if (item.itemName.trim().length > 64) {
    fieldErrors[`${itemPrefix}.itemName`] = "执行项名称不能超过 64 个字符。";
  }

  if (item.note.trim().length > 500) {
    fieldErrors[`${itemPrefix}.note`] = "执行项备注不能超过 500 个字符。";
  }

  if (structureType === "set_based" && item.itemType !== "set") {
    fieldErrors[`${itemPrefix}.itemType`] = "按组动作只能使用“组”执行项。";
  }

  if (structureType === "single_segment" && item.itemType !== "segment") {
    fieldErrors[`${itemPrefix}.itemType`] = "单段动作只能使用“段”执行项。";
  }

  if (item.metrics.length === 0) {
    fieldErrors[`${itemPrefix}.metrics`] = "每个执行项至少需要 1 个指标。";
    return;
  }

  const metricKeys = new Set<string>();
  for (const [metricPosition, metric] of item.metrics.entries()) {
    const metricPrefix = `${itemPrefix}.metric.${metricPosition}`;

    if (!metric.metricKey) {
      fieldErrors[`${metricPrefix}.metricKey`] = "请选择指标类型。";
    } else {
      if (metricKeys.has(metric.metricKey)) {
        fieldErrors[`${metricPrefix}.metricKey`] =
          "同一执行项下不能重复添加同一种指标。";
      }
      metricKeys.add(metric.metricKey);

      const metricMeta = getMetricMeta(metric.metricKey);
      if (!metricMeta) {
        fieldErrors[`${metricPrefix}.metricKey`] = "存在不支持的训练指标。";
      } else if (
        structureType &&
        !metricMeta.allowedStructureTypes.includes(structureType)
      ) {
        fieldErrors[`${metricPrefix}.metricKey`] =
          "当前动作结构不允许使用这个指标。";
      }
    }

    if (!metric.metricValueNumberText.trim()) {
      fieldErrors[`${metricPrefix}.metricValueNumberText`] = "请输入指标数值。";
      continue;
    }

    const numericValue = Number(metric.metricValueNumberText);
    if (!Number.isFinite(numericValue)) {
      fieldErrors[`${metricPrefix}.metricValueNumberText`] = "指标数值必须是数字。";
      continue;
    }

    const metricMeta = getMetricMeta(metric.metricKey);
    if (metricMeta?.min !== undefined && numericValue < metricMeta.min) {
      fieldErrors[`${metricPrefix}.metricValueNumberText`] =
        `${metricMeta.label}不能小于 ${metricMeta.min}。`;
    }

    if (metricMeta?.max !== undefined && numericValue > metricMeta.max) {
      fieldErrors[`${metricPrefix}.metricValueNumberText`] =
        `${metricMeta.label}不能大于 ${metricMeta.max}。`;
    }
  }
}

export function hasFieldErrors(fieldErrors: CycleTemplateFieldErrors) {
  return Object.keys(fieldErrors).length > 0;
}

export function getCycleTemplateFieldErrorSummaries(
  fieldErrors: CycleTemplateFieldErrors
) {
  const summaries: string[] = [];
  const seen = new Set<string>();

  for (const [key, message] of Object.entries(fieldErrors)) {
    const summary = `${getFieldLabel(key)}：${message}`;
    if (seen.has(summary)) {
      continue;
    }

    seen.add(summary);
    summaries.push(summary);
  }

  return summaries;
}

function getFieldLabel(key: string) {
  if (key === "templateName") {
    return "模板名称";
  }

  if (key === "cycleLength") {
    return "周期长度";
  }

  if (key === "goalType") {
    return "训练目标";
  }

  const dayNameMatch = key.match(/^day\.(\d+)\.dayName$/);
  if (dayNameMatch?.[1]) {
    return `Day ${Number(dayNameMatch[1]) + 1} 名称`;
  }

  const dayIndexMatch = key.match(/^day\.(\d+)\.dayIndex$/);
  if (dayIndexMatch?.[1]) {
    return `Day ${Number(dayIndexMatch[1]) + 1}`;
  }

  const exerciseMatch = key.match(/^day\.(\d+)\.exercise\.(\d+)\.(.+)$/);
  if (exerciseMatch?.[1] && exerciseMatch[2] && exerciseMatch[3]) {
    const dayLabel = `Day ${Number(exerciseMatch[1]) + 1}`;
    const exerciseLabel = `动作 ${Number(exerciseMatch[2]) + 1}`;
    const suffix = exerciseMatch[3];

    if (suffix === "exerciseId") {
      return `${dayLabel} · ${exerciseLabel} · 动作选择`;
    }

    if (suffix === "structureType") {
      return `${dayLabel} · ${exerciseLabel} · 结构类型`;
    }

    if (suffix === "note") {
      return `${dayLabel} · ${exerciseLabel} · 动作备注`;
    }

    if (suffix === "items") {
      return `${dayLabel} · ${exerciseLabel} · 执行项`;
    }

    const itemMatch = suffix.match(/^item\.(\d+)\.(.+)$/);
    if (itemMatch?.[1] && itemMatch[2]) {
      const itemLabel = `执行项 ${Number(itemMatch[1]) + 1}`;
      const itemSuffix = itemMatch[2];

      if (itemSuffix === "itemName") {
        return `${dayLabel} · ${exerciseLabel} · ${itemLabel} · 名称`;
      }

      if (itemSuffix === "itemType") {
        return `${dayLabel} · ${exerciseLabel} · ${itemLabel} · 类型`;
      }

      if (itemSuffix === "note") {
        return `${dayLabel} · ${exerciseLabel} · ${itemLabel} · 备注`;
      }

      if (itemSuffix === "metrics") {
        return `${dayLabel} · ${exerciseLabel} · ${itemLabel} · 指标`;
      }

      const metricMatch = itemSuffix.match(/^metric\.(\d+)\.(.+)$/);
      if (metricMatch?.[1] && metricMatch[2]) {
        const metricLabel = `指标 ${Number(metricMatch[1]) + 1}`;
        const metricSuffix = metricMatch[2];

        if (metricSuffix === "metricKey") {
          return `${dayLabel} · ${exerciseLabel} · ${itemLabel} · ${metricLabel} · 类型`;
        }

        if (metricSuffix === "metricValueNumberText") {
          return `${dayLabel} · ${exerciseLabel} · ${itemLabel} · ${metricLabel} · 数值`;
        }
      }
    }
  }

  return "表单字段";
}
