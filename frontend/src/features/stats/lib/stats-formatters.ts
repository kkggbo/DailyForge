import { ApiRequestError } from "../../../shared/api/http";

export function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "--";
  }

  if (!Number.isFinite(value)) {
    return "--";
  }

  return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 1 }).format(
    value
  );
}

export function formatVolumeKg(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return "--";
  }
  return `${formatNumber(value)} kg`;
}

export function formatDistanceKm(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return "--";
  }
  return `${formatNumber(value)} km`;
}

export function formatDurationMinutes(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return "--";
  }
  return `${Math.round(value)} 分钟`;
}

export function formatDurationSeconds(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return "--";
  }
  const totalMinutes = Math.round(value / 60);
  return `${totalMinutes} 分钟`;
}

export function formatAvgSpeedKmh(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return "--";
  }
  return `${formatNumber(value)} km/h`;
}

export function formatDateShort(date: string | null | undefined): string {
  if (!date) {
    return "--";
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(date);
  if (match) {
    return `${match[2]}-${match[3]}`;
  }
  return date;
}

export function getStatsErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiRequestError)) {
    return error instanceof Error ? error.message : fallback;
  }

  switch (error.code) {
    case "UNAUTHORIZED":
      return "登录已失效，请重新登录。";
    case "RESOURCE_NOT_FOUND":
      return "没有找到对应的数据。";
    case "INVALID_ARGUMENT":
      return "请求参数不合法，请调整后重试。";
    default:
      return error.message || fallback;
  }
}
