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

export type AuthUserSummary = {
  userId: number;
  email: string;
  userName: string;
  platformRole: string;
  accountTier: string;
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
  inviteCodeApplied: boolean;
};

export type CurrentUserResponse = {
  userId: number;
  email: string;
  userName: string;
  platformRole: string;
  accountTier: string;
  status: string;
};

export type RedeemInviteCodeResponse = {
  userId: number;
  accountTier: string;
  inviteCode: string;
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
