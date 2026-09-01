import { ApiRequestError } from "../../../shared/api/http";

const USERNAME_PATTERN = /^[\u4e00-\u9fa5A-Za-z0-9_]+$/;

export function validateUserName(userName: string): string | null {
  const trimmed = userName.trim();
  if (!trimmed) {
    return "请输入用户名。";
  }
  if (trimmed.length < 2 || trimmed.length > 20) {
    return "用户名需为 2~20 个字符。";
  }
  if (!USERNAME_PATTERN.test(trimmed)) {
    return "用户名只能包含中文、字母、数字或下划线。";
  }
  return null;
}

export function validateNewPassword(password: string): string | null {
  if (password.length < 6 || password.length > 18) {
    return "密码长度需在 6 到 18 位之间。";
  }
  return null;
}

export function validateConfirmPassword(
  newPassword: string,
  confirmPassword: string
): string | null {
  if (newPassword !== confirmPassword) {
    return "两次输入的密码不一致。";
  }
  return null;
}

export function validateNewPasswordDiffersFromOld(
  oldPassword: string,
  newPassword: string
): string | null {
  if (oldPassword === newPassword) {
    return "新密码不能与旧密码相同。";
  }
  return null;
}

export function getAuthErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiRequestError)) {
    return error instanceof Error ? error.message : fallback;
  }

  switch (error.code) {
    case "USERNAME_ALREADY_EXISTS":
      return "该用户名已被占用，请换一个。";
    case "PASSWORD_INCORRECT":
      return "旧密码不正确。";
    case "PASSWORD_CONFIRM_MISMATCH":
      return "两次输入的密码不一致。";
    case "PASSWORD_SAME_AS_OLD":
      return "新密码不能与旧密码相同。";
    case "FORGOT_CODE_INVALID":
      return "验证码错误，请重新输入。";
    case "FORGOT_CODE_EXPIRED":
      return "验证码已过期，请重新发送。";
    case "FORGOT_CODE_ATTEMPTS_EXCEEDED":
      return "验证码错误次数过多，请重新发送。";
    case "FORGOT_CODE_TOO_FREQUENT":
      return "发送过于频繁，请稍后再试。";
    case "EMAIL_SEND_FAILED":
      return "验证码发送失败，请稍后再试。";
    case "UNAUTHORIZED":
      return "登录已失效，请重新登录。";
    case "INVALID_ARGUMENT":
      return "提交内容不合法，请检查后重试。";
    default:
      return error.message || fallback;
  }
}
