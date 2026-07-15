export const exerciseErrorMessages: Record<string, string> = {
  EXERCISE_NOT_FOUND: "动作不存在或当前不可访问。",
  INVALID_ARGUMENT: "动作查询参数不合法，请检查后重试。",
  UNAUTHORIZED: "登录状态已失效，请重新登录。",
  TOKEN_INVALID: "登录状态已失效，请重新登录。",
  TOKEN_EXPIRED: "登录状态已过期，请重新登录。",
  FORBIDDEN: "当前账号没有权限查询动作。"
};

export function getExerciseErrorMessage(error: unknown, fallback: string) {
  if (
    error &&
    typeof error === "object" &&
    "code" in error &&
    typeof error.code === "string"
  ) {
    return exerciseErrorMessages[error.code] ?? getErrorMessage(error, fallback);
  }

  return getErrorMessage(error, fallback);
}

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}
