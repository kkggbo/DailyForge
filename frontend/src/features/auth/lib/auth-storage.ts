import type { AuthUserSummary } from "../api/auth";

const AUTH_STORAGE_KEY = "dailyforge.auth.session";

export type StoredAuthSession = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: AuthUserSummary;
};

export function getStoredAuthSession(): StoredAuthSession | null {
  const rawValue = window.localStorage.getItem(AUTH_STORAGE_KEY);

  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue) as StoredAuthSession;
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function setStoredAuthSession(session: StoredAuthSession) {
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

export function clearStoredAuthSession() {
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
}
