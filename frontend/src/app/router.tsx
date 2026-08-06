import { createBrowserRouter, Navigate, Outlet } from "react-router-dom";
import { AppShell } from "./layout/AppShell";
import { useAuth } from "./providers/AuthProvider";
import { AiCoachPage } from "../features/ai-coach/pages/AiCoachPage";
import { AiCoachHistoryPage } from "../features/ai-coach/pages/AiCoachHistoryPage";
import { CycleSummaryPage } from "../features/ai-coach/pages/CycleSummaryPage";
import { CycleSummaryTaskPage } from "../features/ai-coach/pages/CycleSummaryTaskPage";
import { TemplateGenerationPage } from "../features/ai-coach/pages/TemplateGenerationPage";
import { TemplateGenerationTaskPage } from "../features/ai-coach/pages/TemplateGenerationTaskPage";
import { LoginPage } from "../features/auth/pages/LoginPage";
import { RedeemInviteCodePage } from "../features/auth/pages/RedeemInviteCodePage";
import { RegisterPage } from "../features/auth/pages/RegisterPage";
import { CycleTemplateCreatePage } from "../features/cycle-template/pages/CycleTemplateCreatePage";
import { CycleTemplateDetailPage } from "../features/cycle-template/pages/CycleTemplateDetailPage";
import { CycleTemplateEditPage } from "../features/cycle-template/pages/CycleTemplateEditPage";
import { CycleTemplatePage } from "../features/cycle-template/pages/CycleTemplatePage";
import { HomePage } from "../features/home/pages/HomePage";
import { LandingPage } from "../features/home/pages/LandingPage";
import { hasCompletedProfileOnboarding } from "../features/profile/lib/onboarding-storage";
import { ProfileAiCompletionPage } from "../features/profile/pages/ProfileAiCompletionPage";
import { ProfileOnboardingPage } from "../features/profile/pages/ProfileOnboardingPage";
import { ProfilePage } from "../features/profile/pages/ProfilePage";
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
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

function AppEntryPage() {
  const { currentUser } = useAuth();

  if (currentUser && !hasCompletedProfileOnboarding(currentUser.userId)) {
    return <Navigate to="/profile/onboarding" replace />;
  }

  return <HomePage />;
}

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      {
        path: "/",
        element: <LandingPage />
      },
      {
        path: "/login",
        element: <LoginPage />
      },
      {
        path: "/register",
        element: <RegisterPage />
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
            path: "/ai-coach",
            element: <AiCoachPage />
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
            path: "/profile",
            element: <ProfilePage />
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
