import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren
} from "react";
import {
  fetchCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  refreshAccessToken as refreshAccessTokenRequest,
  redeemInviteCode as redeemInviteCodeRequest,
  register as registerRequest,
  updateUserName as updateUserNameRequest,
  type AuthTokenResponse,
  type CurrentUserResponse,
  type LoginPayload,
  type RedeemInviteCodePayload,
  type RegisterPayload
} from "../../features/auth/api/auth";
import { ApiRequestError } from "../../shared/api/http";
import {
  clearStoredAuthSession,
  getStoredAuthSession,
  setStoredAuthSession,
  type StoredAuthSession
} from "../../features/auth/lib/auth-storage";

type AuthContextValue = {
  currentUser: CurrentUserResponse | null;
  isAuthenticated: boolean;
  isBootstrapping: boolean;
  accessToken: string | null;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
  redeemInviteCode: (payload: RedeemInviteCodePayload) => Promise<string>;
  updateUserName: (userName: string) => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);
const SESSION_REFRESH_SKEW_MS = 60_000;

function toStoredSession(response: AuthTokenResponse): StoredAuthSession {
  return {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    expiresIn: response.expiresIn,
    expiresAt: Date.now() + response.expiresIn * 1000,
    user: response.user
  };
}

function shouldRefreshSession(session: StoredAuthSession) {
  return session.expiresAt - Date.now() <= SESSION_REFRESH_SKEW_MS;
}

function isRefreshableAuthError(error: unknown) {
  if (!(error instanceof ApiRequestError)) {
    return false;
  }

  return (
    error.status === 401 ||
    error.code === "UNAUTHORIZED" ||
    error.code === "TOKEN_INVALID" ||
    error.code === "TOKEN_EXPIRED" ||
    error.code === "TOKEN_TYPE_MISMATCH"
  );
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [currentUser, setCurrentUser] = useState<CurrentUserResponse | null>(null);
  const [session, setSession] = useState<StoredAuthSession | null>(() =>
    getStoredAuthSession()
  );
  const [isBootstrapping, setIsBootstrapping] = useState(true);

  function updateStoredSession(nextSession: StoredAuthSession | null) {
    if (nextSession) {
      setStoredAuthSession(nextSession);
    } else {
      clearStoredAuthSession();
    }
    setSession(nextSession);
  }

  async function refreshSession(currentSession: StoredAuthSession) {
    const response = await refreshAccessTokenRequest({
      refreshToken: currentSession.refreshToken
    });
    const nextSession = toStoredSession(response);
    updateStoredSession(nextSession);
    return nextSession;
  }

  async function loadCurrentUser(currentSession: StoredAuthSession) {
    try {
      return await fetchCurrentUser(currentSession.accessToken);
    } catch (error) {
      if (!isRefreshableAuthError(error)) {
        throw error;
      }

      const nextSession = await refreshSession(currentSession);
      return fetchCurrentUser(nextSession.accessToken);
    }
  }

  useEffect(() => {
    async function bootstrap() {
      if (!session?.accessToken) {
        setIsBootstrapping(false);
        return;
      }

      try {
        const effectiveSession = shouldRefreshSession(session)
          ? await refreshSession(session)
          : session;
        const me = await loadCurrentUser(effectiveSession);
        setCurrentUser(me);
      } catch {
        updateStoredSession(null);
        setCurrentUser(null);
      } finally {
        setIsBootstrapping(false);
      }
    }

    void bootstrap();
  }, [session?.accessToken, session?.expiresAt, session?.refreshToken]);

  useEffect(() => {
    if (!session?.refreshToken || !currentUser) {
      return;
    }

    const timeoutMs = Math.max(session.expiresAt - Date.now() - SESSION_REFRESH_SKEW_MS, 0);
    let cancelled = false;

    const timerId = window.setTimeout(() => {
      void (async () => {
        try {
          const nextSession = await refreshSession(session);
          const me = await fetchCurrentUser(nextSession.accessToken);
          if (!cancelled) {
            setCurrentUser(me);
          }
        } catch {
          if (!cancelled) {
            updateStoredSession(null);
            setCurrentUser(null);
          }
        }
      })();
    }, timeoutMs);

    return () => {
      cancelled = true;
      window.clearTimeout(timerId);
    };
  }, [currentUser, session?.expiresAt, session?.refreshToken]);

  const value = useMemo<AuthContextValue>(
    () => ({
      currentUser,
      isAuthenticated: Boolean(session?.accessToken && currentUser),
      isBootstrapping,
      accessToken: session?.accessToken ?? null,
      async login(payload) {
        const response = await loginRequest(payload);
        const nextSession = toStoredSession(response);
        updateStoredSession(nextSession);

        const me = await fetchCurrentUser(response.accessToken);
        setCurrentUser(me);
      },
      async register(payload) {
        await registerRequest(payload);
      },
      async logout() {
        try {
          if (session?.accessToken) {
            await logoutRequest(session.accessToken, session.refreshToken);
          }
        } finally {
          updateStoredSession(null);
          setCurrentUser(null);
        }
      },
      async redeemInviteCode(payload) {
        if (!session?.accessToken) {
          throw new Error("当前未登录，无法兑换邀请码");
        }

        const response = await redeemInviteCodeRequest(session.accessToken, payload);
        const nextSession = {
          ...session,
          user: {
            ...session.user,
            accountTier: response.accountTier,
            accountTierExpiresAt: response.accountTierExpiresAt
          }
        };

        updateStoredSession(nextSession);
        setCurrentUser((previous) =>
          previous
            ? {
                ...previous,
                accountTier: response.accountTier,
                accountTierExpiresAt: response.accountTierExpiresAt
              }
            : previous
        );

        return response.accountTier;
      },
      async updateUserName(userName) {
        if (!session?.accessToken) {
          throw new Error("当前未登录，无法修改用户名");
        }

        const me = await updateUserNameRequest(session.accessToken, {
          userName
        });

        updateStoredSession({
          ...session,
          user: { ...session.user, userName: me.userName }
        });
        setCurrentUser(me);
      }
    }),
    [currentUser, isBootstrapping, session]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
