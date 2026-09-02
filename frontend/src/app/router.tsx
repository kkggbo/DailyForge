import { useEffect, useState } from "react";
import { createBrowserRouter, Navigate, Outlet } from "react-router-dom";
import { AppShell } from "./layout/AppShell";
import { useAuth } from "./providers/AuthProvider";
import { AiCoachHistoryPage } from "../features/ai-coach/pages/AiCoachHistoryPage";
import { CycleSummaryPage } from "../features/ai-coach/pages/CycleSummaryPage";
import { CycleSummaryTaskPage } from "../features/ai-coach/pages/CycleSummaryTaskPage";
import { NextCycleGenerationTaskPage } from "../features/ai-coach/pages/NextCycleGenerationTaskPage";
import { TemplateGenerationPage } from "../features/ai-coach/pages/TemplateGenerationPage";
import { TemplateGenerationTaskPage } from "../features/ai-coach/pages/TemplateGenerationTaskPage";
import { LoginPage } from "../features/auth/pages/LoginPage";
import { RedeemInviteCodePage } from "../features/auth/pages/RedeemInviteCodePage";
import { RegisterPage } from "../features/auth/pages/RegisterPage";
import { AccountPage } from "../features/auth/pages/AccountPage";
import { ForgotPasswordPage } from "../features/auth/pages/ForgotPasswordPage";
import { CycleTemplateCreatePage } from "../features/cycle-template/pages/CycleTemplateCreatePage";
import { CycleTemplateDetailPage } from "../features/cycle-template/pages/CycleTemplateDetailPage";
import { CycleTemplateEditPage } from "../features/cycle-template/pages/CycleTemplateEditPage";
import { CycleTemplatePage } from "../features/cycle-template/pages/CycleTemplatePage";
import { DietDiaryPage } from "../features/diet/pages/DietDiaryPage";
import { DietFoodsPage } from "../features/diet/pages/DietFoodsPage";
import { DietStatsPage } from "../features/diet/pages/DietStatsPage";
import { HomePage } from "../features/home/pages/HomePage";
import { getProfileCompletionSummary } from "../features/profile/api/profile";
import {
  hasCompletedProfileOnboarding,
  markProfileOnboardingCompleted
} from "../features/profile/lib/onboarding-storage";
import { BodyMetricHistoryPage } from "../features/profile/pages/BodyMetricHistoryPage";
import { ProfileAiCompletionPage } from "../features/profile/pages/ProfileAiCompletionPage";
import { ProfileEditPage } from "../features/profile/pages/ProfileEditPage";
import { ProfileOnboardingPage } from "../features/profile/pages/ProfileOnboardingPage";
import { ProfilePage } from "../features/profile/pages/ProfilePage";
import { StatsPage } from "../features/stats/pages/StatsPage";
import { WorkoutHistoryDetailPage } from "../features/workout/pages/WorkoutHistoryDetailPage";
import { WorkoutPage } from "../features/workout/pages/WorkoutPage";

function ProtectedOutlet() {
  const { isAuthenticated, isBootstrapping } = useAuth();

  if (isBootstrapping) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
          正在同步账号状态...
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

function GuestOnlyOutlet() {
  const { isAuthenticated, isBootstrapping } = useAuth();

  if (isBootstrapping) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
          正在同步账号状态...
        </div>
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/app" replace />;
  }

  return <Outlet />;
}

function AppEntryPage() {
  const { currentUser, accessToken } = useAuth();
  const [isCheckingOnboarding, setIsCheckingOnboarding] = useState(true);
  const [shouldOnboard, setShouldOnboard] = useState(false);

  useEffect(() => {
    if (!currentUser || !accessToken) {
      setShouldOnboard(false);
      setIsCheckingOnboarding(false);
      return;
    }

    if (hasCompletedProfileOnboarding(currentUser.userId)) {
      setShouldOnboard(false);
      setIsCheckingOnboarding(false);
      return;
    }

    const token = accessToken;
    const userId = currentUser.userId;
    let cancelled = false;

    async function checkCompleteness() {
      try {
        const summary = await getProfileCompletionSummary(token);
        if (cancelled) {
          return;
        }
        if (summary.basicProfileReady && summary.hasWeightRecord) {
          markProfileOnboardingCompleted(userId);
          setShouldOnboard(false);
        } else {
          setShouldOnboard(true);
        }
      } catch {
        // 接口异常：本次宽松进应用，不强制引导首次 onboarding
        if (!cancelled) {
          setShouldOnboard(false);
        }
      } finally {
        if (!cancelled) {
          setIsCheckingOnboarding(false);
        }
      }
    }

    void checkCompleteness();

    return () => {
      cancelled = true;
    };
  }, [currentUser, accessToken]);

  if (isCheckingOnboarding) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
          正在同步账号状态...
        </div>
      </div>
    );
  }

  if (shouldOnboard) {
    return <Navigate to="/profile/onboarding" replace />;
  }

  return <HomePage />;
}

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      {
        element: <GuestOnlyOutlet />,
        children: [
          {
            path: "/",
            element: <LoginPage />
          },
          {
            path: "/register",
            element: <RegisterPage />
          },
          {
            path: "/forgot-password",
            element: <ForgotPasswordPage />
          }
        ]
      },
      {
        path: "/login",
        element: <Navigate to="/" replace />
      },
      {
        element: <ProtectedOutlet />,
        children: [
          {
            path: "/app",
            element: <AppEntryPage />
          },
          {
            path: "/invite-code",
            element: <RedeemInviteCodePage />
          },
          {
            path: "/account",
            element: <AccountPage />
          },
          {
            path: "/ai-coach",
            element: <Navigate to="/ai-coach/template-generation" replace />
          },
          {
            path: "/ai-coach/history",
            element: <AiCoachHistoryPage />
          },
          {
            path: "/ai-coach/template-generation",
            element: <TemplateGenerationPage />
          },
          {
            path: "/ai-coach/template-generation/tasks/:taskId",
            element: <TemplateGenerationTaskPage />
          },
          {
            path: "/ai-coach/cycle-summary",
            element: <CycleSummaryPage />
          },
          {
            path: "/ai-coach/cycle-summary/tasks/:taskId",
            element: <CycleSummaryTaskPage />
          },
          {
            path: "/ai-coach/next-cycle-generation/tasks/:taskId",
            element: <NextCycleGenerationTaskPage />
          },
          {
            path: "/stats",
            element: <StatsPage />
          },
          {
            path: "/diet",
            element: <DietDiaryPage />
          },
          {
            path: "/diet/foods",
            element: <DietFoodsPage />
          },
          {
            path: "/diet/stats",
            element: <DietStatsPage />
          },
          {
            path: "/profile",
            element: <ProfilePage />
          },
          {
            path: "/profile/edit",
            element: <ProfileEditPage />
          },
          {
            path: "/profile/metrics/history",
            element: <BodyMetricHistoryPage />
          },
          {
            path: "/profile/onboarding",
            element: <ProfileOnboardingPage />
          },
          {
            path: "/profile/ai-completion",
            element: <ProfileAiCompletionPage />
          },
          {
            path: "/cycle-templates",
            element: <CycleTemplatePage />
          },
          {
            path: "/cycle-templates/create",
            element: <CycleTemplateCreatePage />
          },
          {
            path: "/cycle-templates/:templateId",
            element: <CycleTemplateDetailPage />
          },
          {
            path: "/cycle-templates/:templateId/edit",
            element: <CycleTemplateEditPage />
          },
          {
            path: "/workout",
            element: <WorkoutPage />
          },
          {
            path: "/workout/history/:sessionId",
            element: <WorkoutHistoryDetailPage />
          }
        ]
      }
    ]
  }
]);
