import { ApiRequestError } from "../../../shared/api/http";
import type { ExerciseStatus, FailureReason, MetricKey, SavePayload, SessionStatus, SessionType, WorkoutSession } from "../types/workout";

export const exerciseStatuses: Array<{ value: ExerciseStatus; label: string }> = [
  { value: "completed", label: "已完成" },
  { value: "partial_completed", label: "部分完成" },
  { value: "skipped", label: "已跳过" },
  { value: "failed", label: "未完成" }
];

export const failureReasons: Array<{ value: FailureReason; label: string }> = [
  { value: "too_tired", label: "状态不佳 / 力竭" },
  { value: "equipment_unavailable", label: "器械不可用" },
  { value: "pain_or_discomfort", label: "疼痛或不适" },
  { value: "time_not_enough", label: "时间不足" },
  { value: "plan_too_hard", label: "计划强度过高" },
  { value: "other", label: "其他" }
];

const metricLabels: Record<MetricKey, string> = {
  weight_kg: "重量",
  reps: "次数",
  duration_seconds: "时长",
  duration_minutes: "时长",
  distance_km: "距离",
  speed_kmh: "速度",
  pace_seconds_per_km: "配速",
  incline_percent: "坡度",
  rest_seconds: "休息",
  rpe: "RPE",
  intensity_level: "强度等级"
};

const sessionLabels: Record<SessionStatus, string> = {
  in_progress: "进行中",
  completed: "已完成",
  cancelled: "已取消"
};

const integerMetricKeys = new Set<MetricKey>([
  "reps",
  "duration_seconds",
  "duration_minutes",
  "rest_seconds",
  "intensity_level"
]);

export function formatTime(value: string | null) {
  if (!value) return "未记录";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat("zh-CN", { dateStyle: "medium", timeStyle: "short" }).format(date);
}

export function metricLabel(key: MetricKey) {
  return metricLabels[key];
}

export function metricUnitLabel(unit: string | null) {
  if (unit === null) {
    return "";
  }

  switch (unit) {
    case "seconds":
      return "秒";
    case "minutes":
      return "分钟";
    case "count":
      return "次";
    case "sec/km":
      return "秒/公里";
    case "percent":
      return "%";
    case "km":
      return "km";
    case "km/h":
      return "km/h";
    case "rpe":
      return "";
    default:
      return unit;
  }
}

export function sessionLabel(status: SessionStatus) {
  return sessionLabels[status];
}

export function sessionTypeLabel(type: SessionType) {
  return type === "rest_day" ? "休息日" : "训练日";
}

export function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiRequestError) {
    if (error.code === "WORKOUT_AI_NOT_IMPLEMENTED") return "AI 循环分析能力暂未开放。";
    if (error.code === "WORKOUT_EXERCISE_STATUS_REQUIRED") return "请先为每个动作选择完成状态。";
    if (error.code === "WORKOUT_CYCLE_COMPLETED") return "当前循环已完成，请选择下一步。";
    if (error.message) return error.message;
  }
  return error instanceof Error && error.message ? error.message : fallback;
}

export type SessionForm = {
  notes: string;
  exercises: Array<{
    sessionExerciseId: number;
    exerciseStatus: string;
    failureReason: string;
    note: string;
    items: Array<{
      itemIndex: number;
      metrics: Array<{
        metricKey: MetricKey;
        actual: string;
      }>;
    }>;
  }>;
};

export function toForm(session: WorkoutSession): SessionForm {
  return {
    notes: session.notes ?? "",
    exercises: session.exercises.map((exercise) => ({
      sessionExerciseId: exercise.sessionExerciseId ?? 0,
      exerciseStatus: exercise.exerciseStatus ?? "",
      failureReason: exercise.failureReason ?? "",
      note: exercise.feedback ?? "",
      items: exercise.items.map((item) => ({
        itemIndex: item.itemIndex,
        metrics: item.metrics.map((metric) => ({
          metricKey: metric.metricKey,
          actual: metric.actualValueNumber === null ? "" : String(metric.actualValueNumber)
        }))
      }))
    }))
  };
}

export function toPayload(form: SessionForm): SavePayload {
  const text = (value: string) => value.trim() || null;
  const number = (value: string) => value.trim() === "" ? null : Number.isFinite(Number(value)) ? Number(value) : null;

  return {
    notes: text(form.notes),
    exercises: form.exercises.map((exercise) => ({
      sessionExerciseId: exercise.sessionExerciseId,
      exerciseStatus: exercise.exerciseStatus ? exercise.exerciseStatus as ExerciseStatus : null,
      failureReason: exercise.failureReason ? exercise.failureReason as FailureReason : null,
      feedback: text(exercise.note),
      items: exercise.items.map((item) => ({
        itemIndex: item.itemIndex,
        metrics: item.metrics.map((metric) => ({
          metricKey: metric.metricKey,
          actualValueNumber: number(metric.actual)
        }))
      }))
    }))
  };
}


export function metricInputRule(metricKey: MetricKey) {
  const integerOnly = integerMetricKeys.has(metricKey);
  return {
    integerOnly,
    inputMode: integerOnly ? "numeric" as const : "decimal" as const,
    step: integerOnly ? "1" : "0.01"
  };
}

export function metricActualError(metricKey: MetricKey, value: string) {
  const normalized = value.trim();
  if (!normalized) return null;
  if (!/^\d+(?:\.\d+)?$/.test(normalized)) return "请输入大于或等于 0 的数字。";
  const rule = metricInputRule(metricKey);
  if (rule.integerOnly && normalized.includes(".")) return "该指标只允许输入整数。";
  const decimalPart = normalized.split(".")[1];
  if (!rule.integerOnly && decimalPart && decimalPart.length > 2) return "最多保留 2 位小数。";
  return null;
}

export function firstMetricValidationError(form: SessionForm) {
  for (const exercise of form.exercises) {
    for (const item of exercise.items) {
      for (const metric of item.metrics) {
        const error = metricActualError(metric.metricKey, metric.actual);
        if (error) return `${metricLabel(metric.metricKey)}：${error}`;
      }
    }
  }
  return null;
}


