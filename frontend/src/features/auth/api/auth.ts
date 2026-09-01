import { request } from "../../../shared/api/http";

export type RegisterPayload = {
  email: string;
  password: string;
  confirmPassword: string;
  userName: string;
  inviteCode?: string;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type RedeemInviteCodePayload = {
  code: string;
};

export type RefreshTokenPayload = {
  refreshToken: string;
};

export type AuthUserSummary = {
  userId: number;
  email: string;
  userName: string;
  platformRole: string;
  accountTier: string;
  accountTierExpiresAt: string | null;
};

export type AuthTokenResponse = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: AuthUserSummary;
};

export type RegisterResponse = {
  userId: number;
  email: string;
  userName: string;
  platformRole: string;
  accountTier: string;
  accountTierExpiresAt: string | null;
  inviteCodeApplied: boolean;
};

export type CurrentUserResponse = {
  userId: number;
  email: string;
  userName: string;
  platformRole: string;
  accountTier: string;
  accountTierExpiresAt: string | null;
  status: string;
};

export type RedeemInviteCodeResponse = {
  userId: number;
  accountTier: string;
  accountTierExpiresAt: string | null;
  inviteCode: string;
};

export type UpdateUserNameRequest = {
  userName: string;
};

export type ChangePasswordRequest = {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
};

export type ForgotPasswordCodeRequest = {
  email: string;
};

export type ResetPasswordRequest = {
  email: string;
  code: string;
  newPassword: string;
  confirmPassword: string;
};

export async function register(payload: RegisterPayload) {
  return request<RegisterResponse>("/auth/register", {
    method: "POST",
    body: payload
  });
}

export async function login(payload: LoginPayload) {
  return request<AuthTokenResponse>("/auth/login", {
    method: "POST",
    body: payload
  });
}

export async function refreshAccessToken(payload: RefreshTokenPayload) {
  return request<AuthTokenResponse>("/auth/refresh-token", {
    method: "POST",
    body: payload
  });
}

export async function fetchCurrentUser(accessToken: string) {
  return request<CurrentUserResponse>("/auth/me", {
    method: "GET",
    accessToken
  });
}

export async function logout(accessToken: string, refreshToken?: string | null) {
  return request<void>("/auth/logout", {
    method: "POST",
    accessToken,
    body: {
      refreshToken: refreshToken ?? null
    }
  });
}

export async function redeemInviteCode(
  accessToken: string,
  payload: RedeemInviteCodePayload
) {
  return request<RedeemInviteCodeResponse>("/auth/redeem-invite-code", {
    method: "POST",
    accessToken,
    body: payload
  });
}

export function updateUserName(
  accessToken: string,
  payload: UpdateUserNameRequest
) {
  return request<CurrentUserResponse>("/auth/username", {
    method: "PUT",
    accessToken,
    body: payload
  });
}

export function changePassword(
  accessToken: string,
  payload: ChangePasswordRequest
) {
  return request<void>("/auth/password/change", {
    method: "POST",
    accessToken,
    body: payload
  });
}

export function sendForgotPasswordCode(payload: ForgotPasswordCodeRequest) {
  return request<void>("/auth/password/forgot-code", {
    method: "POST",
    body: payload
  });
}

export function resetPassword(payload: ResetPasswordRequest) {
  return request<void>("/auth/password/reset", {
    method: "POST",
    body: payload
  });
}
