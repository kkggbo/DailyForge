import { ApiRequestError } from "../../../shared/api/http";
import { getDietMissingFieldLabel } from "./diet-enums";

export function formatGrams(grams: number | null | undefined): string {
  if (grams === null || grams === undefined) {
    return "--";
  }
  return `${grams} g`;
}

export function formatKcal(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return "--";
  }
  return `${Math.round(value)} 千卡`;
}

export function formatMacroGrams(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return "--";
  }
  return `${value} g`;
}

export function getDietErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiRequestError)) {
    return error instanceof Error ? error.message : fallback;
  }

  switch (error.code) {
    case "FOOD_NOT_FOUND":
      return "该食物不存在或已不可用。";
    case "FOOD_UPLOAD_INVALID":
      return "上传食物信息不合法（名称或每 100g 营养）。";
    case "DIET_LOG_INVALID":
      return "记录参数不合法，请检查克数与餐次。";
    case "DIET_TARGET_INVALID":
      return "目标值不合法，请检查后重试。";
    case "RESOURCE_NOT_FOUND":
      return "记录不存在或无权访问。";
    case "UNAUTHORIZED":
      return "登录已失效，请重新登录。";
    case "INVALID_ARGUMENT":
      return "提交内容不合法，请检查后重试。";
    default:
      return error.message || fallback;
  }
}

/**
 * 把后端返回的缺失资料字段映射为中文（含 activityLevel），用于「去补齐资料」提示。
 */
export function mapDietMissingFields(fields: string[]): string[] {
  return fields.map((field) => getDietMissingFieldLabel(field));
}
