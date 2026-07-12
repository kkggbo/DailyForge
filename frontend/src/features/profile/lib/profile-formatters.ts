export function formatNullableNumber(
  value: number | null | undefined,
  options?: { digits?: number; unit?: string }
) {
  if (value === null || value === undefined) {
    return "--";
  }

  const digits = options?.digits ?? 2;
  const suffix = options?.unit ? ` ${options.unit}` : "";
  return `${value.toFixed(digits)}${suffix}`;
}

export function formatNullableInteger(
  value: number | null | undefined,
  unit?: string
) {
  if (value === null || value === undefined) {
    return "--";
  }

  return unit ? `${value}${unit}` : String(value);
}

export function formatNullableText(value: string | null | undefined) {
  if (!value) {
    return "--";
  }

  return value;
}

export function formatDate(value: string | null | undefined) {
  if (!value) {
    return "--";
  }

  return value;
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "--";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}
