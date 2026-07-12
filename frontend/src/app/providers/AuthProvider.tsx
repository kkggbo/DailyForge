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
  redeemInviteCode as redeemInviteCodeRequest,
  register as registerRequest,
  type AuthTokenResponse,
  type CurrentUserResponse,
  type LoginPayload,
  type RedeemInviteCodePayload,
  type RegisterPayload
} from "../../features/auth/api/auth";
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
  redeemInviteCode: (payload: RedeemInviteCodePayload) => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function toStoredSession(response: AuthTokenResponse): StoredAuthSession {
  return {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    expiresIn: response.expiresIn,
    user: response.user
  };
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

  useEffect(() => {
    async function bootstrap() {
      if (!session?.accessToken) {
        setIsBootstrapping(false);
        return;
      }

      try {
        const me = await fetchCurrentUser(session.accessToken);
        setCurrentUser(me);
      } catch {
        clearStoredAuthSession();
        setSession(null);
        setCurrentUser(null);
      } finally {
        setIsBootstrapping(false);
      }
    }

    void bootstrap();
  }, [session?.accessToken]);

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
            accountTier: response.accountTier
          }
        };

        updateStoredSession(nextSession);
        setCurrentUser((previous) =>
          previous
            ? {
                ...previous,
                accountTier: response.accountTier
              }
            : previous
        );
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
