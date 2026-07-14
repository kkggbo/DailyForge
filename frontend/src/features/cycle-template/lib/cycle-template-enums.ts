import type { CycleTemplateStatus } from "../types/cycle-template";

export const cycleTemplateStatusLabels: Record<CycleTemplateStatus, string> = {
  draft: "草稿",
  active: "已启用",
  inactive: "未启用",
  deleted: "已删除"
};

export const goalTypeOptions = [
  { value: "", label: "未设置" },
  { value: "muscle_gain", label: "增肌" },
  { value: "fat_loss", label: "减脂" },
  { value: "strength", label: "力量提升" },
  { value: "endurance", label: "耐力提升" },
  { value: "health", label: "健康维持" }
];

export const cycleTemplateErrorMessages: Record<string, string> = {
  CYCLE_TEMPLATE_ACTIVE_NOT_FOUND: "当前没有启用中的训练模板。",
  CYCLE_TEMPLATE_SWITCH_CONFIRM_REQUIRED:
    "当前已有启用中的训练模板，请再次确认是否切换。",
  CYCLE_TEMPLATE_EDIT_FORBIDDEN: "当前模板或训练日不允许编辑。",
  CYCLE_TEMPLATE_DELETE_FORBIDDEN: "当前模板不允许删除。",
  CYCLE_TEMPLATE_ACTIVATE_INVALID: "模板还不满足启用条件，请先完善模板内容。",
  CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED: "AI 生成模板功能暂未开放。",
  CYCLE_TEMPLATE_SYSTEM_EXERCISE_REQUIRED: "训练动作必须从系统动作库中选择。",
  CYCLE_TEMPLATE_DAY_OUT_OF_RANGE: "训练日超出了当前周期长度。",
  CYCLE_TEMPLATE_CYCLE_LENGTH_INVALID: "周期长度必须在 1 到 7 天之间。",
  CYCLE_TEMPLATE_NOT_FOUND: "模板不存在或已被删除。",
  CYCLE_TEMPLATE_STATUS_INVALID: "当前模板状态不允许执行该操作。",
  CYCLE_TEMPLATE_EXERCISE_NOT_FOUND: "动作不存在或不可用。",
  INVALID_ARGUMENT: "提交参数不合法，请检查后再试。",
  UNAUTHORIZED: "登录状态已失效，请重新登录。",
  TOKEN_INVALID: "登录状态已失效，请重新登录。",
  TOKEN_EXPIRED: "登录状态已过期，请重新登录。",
  FORBIDDEN: "当前账号没有权限执行该操作。"
};

export function getCycleTemplateErrorMessage(error: unknown, fallback: string) {
  if (
    error &&
    typeof error === "object" &&
    "code" in error &&
    typeof error.code === "string"
  ) {
    return cycleTemplateErrorMessages[error.code] ?? getErrorMessage(error, fallback);
  }

  return getErrorMessage(error, fallback);
}

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}
