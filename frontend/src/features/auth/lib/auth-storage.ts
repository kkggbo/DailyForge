import type { AuthUserSummary } from "../api/auth";

const AUTH_STORAGE_KEY = "dailyforge.auth.session";

export type StoredAuthSession = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  expiresAt: number;
  user: AuthUserSummary;
};

export function getStoredAuthSession(): StoredAuthSession | null {
  const rawValue = window.localStorage.getItem(AUTH_STORAGE_KEY);

  if (!rawValue) {
    return null;
  }

  try {
    const parsed = JSON.parse(rawValue) as Partial<StoredAuthSession>;

    if (
      typeof parsed.accessToken !== "string" ||
      typeof parsed.refreshToken !== "string" ||
      typeof parsed.user !== "object" ||
      parsed.user === null
    ) {
      throw new Error("invalid auth session");
    }

    return {
      accessToken: parsed.accessToken,
      refreshToken: parsed.refreshToken,
      expiresIn: typeof parsed.expiresIn === "number" ? parsed.expiresIn : 0,
      // Older sessions may not have expiresAt. Treat them as immediately refreshable.
      expiresAt: typeof parsed.expiresAt === "number" ? parsed.expiresAt : 0,
      user: parsed.user as AuthUserSummary
    };
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
