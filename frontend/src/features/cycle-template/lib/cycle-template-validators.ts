import type {
  CycleTemplateEditorForm,
  CycleTemplateFieldErrors
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
    fieldErrors.goalType = "目标类型不能超过 32 个字符。";
  }

  for (const day of form.days) {
    if (day.dayName.trim().length > 64) {
      fieldErrors[`day.${day.dayIndex}.dayName`] = "训练日名称不能超过 64 个字符。";
    }

    for (const [exerciseIndex, exercise] of day.exercises.entries()) {
      const prefix = `day.${day.dayIndex}.exercise.${exercise.localId}`;

      if (!exercise.exerciseId) {
        fieldErrors[`${prefix}.exerciseId`] = `Day ${day.dayIndex} 的第 ${
          exerciseIndex + 1
        } 个动作还没有选择系统动作。`;
      }

      validateIntegerRange(
        fieldErrors,
        `${prefix}.targetSets`,
        exercise.targetSets,
        1,
        100,
        "目标组数"
      );
      validateIntegerRange(
        fieldErrors,
        `${prefix}.targetRepsMin`,
        exercise.targetRepsMin,
        1,
        1000,
        "最小次数"
      );
      validateIntegerRange(
        fieldErrors,
        `${prefix}.targetRepsMax`,
        exercise.targetRepsMax,
        1,
        1000,
        "最大次数"
      );
      validateNumberRange(
        fieldErrors,
        `${prefix}.targetWeightKg`,
        exercise.targetWeightKg,
        0,
        9999.99,
        "目标重量"
      );
      validateIntegerRange(
        fieldErrors,
        `${prefix}.targetDurationSeconds`,
        exercise.targetDurationSeconds,
        1,
        86400,
        "目标时长"
      );
      validateIntegerRange(
        fieldErrors,
        `${prefix}.restSeconds`,
        exercise.restSeconds,
        0,
        86400,
        "休息时间"
      );
      validateNumberRange(
        fieldErrors,
        `${prefix}.targetRpe`,
        exercise.targetRpe,
        0,
        10,
        "目标 RPE"
      );

      if (exercise.note.trim().length > 500) {
        fieldErrors[`${prefix}.note`] = "动作备注不能超过 500 个字符。";
      }
    }
  }

  return fieldErrors;
}

export function hasFieldErrors(fieldErrors: CycleTemplateFieldErrors) {
  return Object.keys(fieldErrors).length > 0;
}

export function getCycleTemplateFieldErrorSummaries(
  fieldErrors: CycleTemplateFieldErrors
) {
  const entries = Object.entries(fieldErrors);
  const seen = new Set<string>();
  const summaries: string[] = [];

  for (const [key, message] of entries) {
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
  if (dayNameMatch) {
    return `Day ${dayNameMatch[1]} 名称`;
  }

  const exerciseMatch = key.match(/^day\.(\d+)\.exercise\.[^.]+\.(.+)$/);
  if (exerciseMatch) {
    const [, dayIndex, fieldName] = exerciseMatch;
    if (!fieldName) {
      return `Day ${dayIndex} · 字段错误`;
    }

    const fieldLabels: Record<string, string> = {
      exerciseId: "动作选择",
      targetSets: "目标组数",
      targetRepsMin: "最小次数",
      targetRepsMax: "最大次数",
      targetWeightKg: "目标重量",
      targetDurationSeconds: "目标时长",
      restSeconds: "休息时间",
      targetRpe: "目标 RPE",
      note: "动作备注"
    };

    return `Day ${dayIndex} · ${fieldLabels[fieldName] ?? fieldName}`;
  }

  return "表单字段";
}

function validateIntegerRange(
  fieldErrors: CycleTemplateFieldErrors,
  key: string,
  value: string,
  min: number,
  max: number,
  label: string
) {
  if (!value.trim()) {
    return;
  }

  const numberValue = Number(value);
  if (!Number.isInteger(numberValue) || numberValue < min || numberValue > max) {
    fieldErrors[key] = `${label}必须是 ${min} 到 ${max} 之间的整数。`;
  }
}

function validateNumberRange(
  fieldErrors: CycleTemplateFieldErrors,
  key: string,
  value: string,
  min: number,
  max: number,
  label: string
) {
  if (!value.trim()) {
    return;
  }

  const numberValue = Number(value);
  if (!Number.isFinite(numberValue) || numberValue < min || numberValue > max) {
    fieldErrors[key] = `${label}必须在 ${min} 到 ${max} 之间。`;
  }
}
