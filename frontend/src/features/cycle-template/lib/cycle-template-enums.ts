import type {
  CycleTemplateStatus,
  ItemType,
  StructureType
} from "../types/cycle-template";

export const cycleTemplateStatusLabels: Record<CycleTemplateStatus, string> = {
  draft: "草稿",
  active: "已启用",
  inactive: "未启用",
  deleted: "已删除"
};

export const structureTypeLabels: Record<StructureType, string> = {
  set_based: "按组动作",
  single_segment: "单段动作"
};

export const itemTypeLabels: Record<ItemType, string> = {
  set: "组",
  segment: "段"
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
  CYCLE_TEMPLATE_STRUCTURE_TYPE_INVALID:
    "动作结构类型不合法，或与系统动作默认结构不一致。",
  CYCLE_TEMPLATE_ITEM_INVALID: "执行项结构不合法，请检查组或段的设置。",
  CYCLE_TEMPLATE_ITEM_COUNT_INVALID: "当前动作的执行项数量不符合结构要求。",
  CYCLE_TEMPLATE_METRIC_KEY_INVALID: "存在不支持的训练参数。",
  CYCLE_TEMPLATE_METRIC_DUPLICATE: "同一执行项下不能重复添加同一种参数。",
  CYCLE_TEMPLATE_METRIC_VALUE_INVALID: "训练参数的数值格式不合法。",
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
