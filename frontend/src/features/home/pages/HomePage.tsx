import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { ApiRequestError } from "../../../shared/api/http";
import {
  completeSession,
  getWorkspace,
  initializeCurrentDay
} from "../../workout/api/workout";
import { errorMessage } from "../../workout/lib/workout";
import type { DayDetail, Workspace } from "../../workout/types/workout";

type QuickStartStep = {
  title: string;
  description: string;
  to?: string;
  actions?: Array<{ label: string; to: string }>;
};

const gettingStartedSteps: QuickStartStep[] = [
  {
    title: "完善个人资料",
    description: "补充身体指标与训练目标，让计划更贴合你。",
    to: "/profile"
  },
  {
    title: "创建并启用训练模板",
    description: "用循环模板定义你的训练分化节奏。",
    actions: [
      { label: "手动创建模板", to: "/cycle-templates/create" },
      { label: "AI 生成模板", to: "/ai-coach/template-generation" }
    ]
  },
  {
    title: "开始训练打卡",
    description: "按当天计划记录完成、跳过与原因。",
    to: "/workout"
  },
  {
    title: "用 AI 教练",
    description: "为已完成周期生成总结与下一轮调整建议。",
    to: "/ai-coach/cycle-summary"
  }
];

const aiEnabledTiers = new Set(["invited_ai", "premium"]);

export function HomePage() {
  const { currentUser, accessToken } = useAuth();
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [day, setDay] = useState<DayDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [isCheckingIn, setIsCheckingIn] = useState(false);
  const [checkinMessage, setCheckinMessage] = useState<string | null>(null);

  async function load() {
    if (!accessToken) {
      return;
    }

    const token = accessToken;
    setIsLoading(true);
    setPageError(null);
    setCheckinMessage(null);

    try {
      const nextWorkspace = await getWorkspace(token);
      setWorkspace(nextWorkspace);
      setDay(null);

      if (nextWorkspace.workspaceState === "active") {
        try {
          const initialized = await initializeCurrentDay(token);
          setDay(initialized.day);
        } catch (error) {
          if (
            error instanceof ApiRequestError &&
            error.code === "WORKOUT_CYCLE_COMPLETED"
          ) {
            const refreshed = await getWorkspace(token);
            setWorkspace(refreshed);
          } else {
            setPageError(errorMessage(error, "加载今日训练信息失败，请稍后重试。"));
          }
        }
      }
    } catch (error) {
      setPageError(errorMessage(error, "加载控制台失败，请稍后重试。"));
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [accessToken]);

  async function handleRestDayCheckIn() {
    if (!accessToken || !day?.session) {
      return;
    }

    setIsCheckingIn(true);
    setCheckinMessage(null);

    try {
      await completeSession(accessToken, day.session.sessionId, {
        notes: null,
        exercises: []
      });
      setCheckinMessage("今日休息日打卡完成。");
      await load();
    } catch (error) {
      setPageError(errorMessage(error, "打卡失败，请稍后重试。"));
    } finally {
      setIsCheckingIn(false);
    }
  }

  const showAiUnlock =
    currentUser !== null && !aiEnabledTiers.has(currentUser.accountTier);

  return (
    <section className="space-y-8">
      <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        <WelcomeCard userName={currentUser?.userName} />
        {showAiUnlock ? <AiUnlockCard /> : null}
      </div>

      {isLoading ? (
        <Loading />
      ) : pageError ? (
        <ErrorPanel message={pageError} onRetry={() => void load()} />
      ) : workspace?.workspaceState === "active" ? (
        <TodayCard
          workspace={workspace}
          day={day}
          isCheckingIn={isCheckingIn}
          checkinMessage={checkinMessage}
          onCheckIn={() => void handleRestDayCheckIn()}
        />
      ) : workspace?.workspaceState === "cycle_completed" ? (
        <CycleCompleteCard templateName={workspace.templateName} />
      ) : (
        <QuickStart />
      )}
    </section>
  );
}

function WelcomeCard({ userName }: { userName?: string }) {
  return (
    <div className="rounded-[36px] border border-white/10 bg-white/6 p-8 backdrop-blur">
      <p className="text-sm uppercase tracking-[0.28em] text-amber-300">控制台</p>
      <h1 className="mt-4 text-4xl font-semibold leading-tight text-white sm:text-5xl">
        你好，{userName ?? "训练者"}。
      </h1>
      <p className="mt-4 max-w-2xl leading-8 text-stone-300">
        开始今天的训练，或先完善你的训练计划。
      </p>
    </div>
  );
}

function AiUnlockCard() {
  return (
    <div className="rounded-[36px] border border-amber-300/20 bg-stone-950/70 p-8 backdrop-blur">
      <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
        解锁 AI 权限
      </p>
      <p className="mt-4 leading-7 text-stone-300">
        AI 教练功能需要邀请码解锁。兑换后即可使用 AI 生成模板与周期总结。
      </p>
      <Link
        to="/invite-code"
        className="mt-6 inline-flex rounded-full bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300"
      >
        去兑换邀请码
      </Link>
    </div>
  );
}

function QuickStart() {
  return (
    <div>
      <p className="text-sm uppercase tracking-[0.24em] text-amber-300">快速入门</p>
      <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {gettingStartedSteps.map((step, index) => {
          const stepLabel = (
            <>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-amber-300">
                第 {index + 1} 步
              </p>
              <p className="mt-2 text-base font-medium text-white">{step.title}</p>
              <p className="mt-1 text-sm leading-6 text-stone-300">{step.description}</p>
            </>
          );
          const cardClass =
            "rounded-[28px] border border-white/10 bg-white/5 p-5 transition hover:bg-white/8";

          if (step.actions) {
            return (
              <div key={step.title} className={cardClass}>
                {stepLabel}
                <div className="mt-3 flex flex-wrap gap-2">
                  {step.actions.map((action) => (
                    <Link
                      key={action.label}
                      to={action.to}
                      className="rounded-full border border-white/10 bg-white/8 px-3 py-2 text-xs font-semibold text-stone-100 transition hover:bg-white/12"
                    >
                      {action.label}
                    </Link>
                  ))}
                </div>
              </div>
            );
          }

          return (
            <Link key={step.title} to={step.to ?? "/app"} className={cardClass}>
              {stepLabel}
            </Link>
          );
        })}
      </div>
    </div>
  );
}

function TodayCard({
  workspace,
  day,
  isCheckingIn,
  checkinMessage,
  onCheckIn
}: {
  workspace: Workspace;
  day: DayDetail | null;
  isCheckingIn: boolean;
  checkinMessage: string | null;
  onCheckIn: () => void;
}) {
  const currentDay = workspace.days.find((item) => item.dayState === "current");
  const dayName = day?.dayName ?? currentDay?.dayName ?? "";
  const isRestDay = day?.isRestDay ?? currentDay?.isRestDay ?? false;
  const exerciseNames = (day?.session?.exercises ?? []).map(
    (exercise) => exercise.exerciseName
  );

  return (
    <div className="rounded-[32px] border border-white/10 bg-white/6 p-8 backdrop-blur">
      <p className="text-sm uppercase tracking-[0.28em] text-amber-300">今天训练</p>
      <div className="mt-4 flex flex-wrap items-center gap-3">
        <h2 className="text-3xl font-semibold text-white">
          {workspace.templateName ?? "当前模板"}
        </h2>
        {workspace.runNo !== null ? (
          <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-sm text-stone-300">
            第 {workspace.runNo} 轮
          </span>
        ) : null}
      </div>
      <p className="mt-2 text-stone-300">
        Day {workspace.currentDayIndex ?? "-"} · {dayName}
        {isRestDay ? " · 休息日" : ""}
      </p>

      {isRestDay ? (
        <div className="mt-6 rounded-2xl border border-sky-300/20 bg-sky-300/10 p-5">
          <p className="text-stone-200">今天休息，没有计划动作。</p>
          <div className="mt-4 flex flex-wrap gap-3">
            <button
              type="button"
              disabled={isCheckingIn}
              onClick={onCheckIn}
              className="rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
            >
              {isCheckingIn ? "打卡中..." : "完成今日打卡"}
            </button>
            <Link
              to="/workout"
              className="rounded-full border border-white/10 px-5 py-3 text-sm font-semibold text-stone-100 transition hover:bg-white/10"
            >
              进入训练工作台
            </Link>
          </div>
        </div>
      ) : (
        <>
          <div className="mt-6">
            <p className="text-sm uppercase tracking-[0.2em] text-amber-300">今日动作</p>
            {exerciseNames.length > 0 ? (
              <ul className="mt-3 grid gap-2 sm:grid-cols-2">
                {exerciseNames.map((name, index) => (
                  <li
                    key={`${name}-${index}`}
                    className="rounded-2xl border border-white/10 bg-black/20 px-4 py-3 text-white"
                  >
                    {name}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-3 text-stone-400">今日暂无计划动作。</p>
            )}
          </div>
          <Link
            to="/workout"
            className="mt-6 inline-flex rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300"
          >
            进入训练工作台
          </Link>
        </>
      )}

      {checkinMessage ? (
        <p className="mt-4 text-sm text-emerald-200">{checkinMessage}</p>
      ) : null}
    </div>
  );
}

function CycleCompleteCard({ templateName }: { templateName: string | null }) {
  return (
    <div className="rounded-[32px] border border-amber-300/25 bg-amber-300/10 p-8">
      <p className="text-sm uppercase tracking-[0.28em] text-amber-200">
        Cycle Complete
      </p>
      <h2 className="mt-3 text-3xl font-semibold text-white">这一轮训练已完成</h2>
      <p className="mt-3 max-w-2xl leading-7 text-stone-200">
        {templateName ? `「${templateName}」的当前循环已结束。` : "当前循环已结束。"}
        你可以到训练工作台沿用当前模板重新开始、切换其他模板，或进行 AI 周期总结。
      </p>
      <Link
        to="/workout"
        className="mt-6 inline-flex rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300"
      >
        去训练工作台
      </Link>
    </div>
  );
}

function Loading() {
  return (
    <div className="flex min-h-[32vh] items-center justify-center">
      <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
        正在加载控制台...
      </div>
    </div>
  );
}

function ErrorPanel({
  message,
  onRetry
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <div className="rounded-[32px] border border-rose-400/20 bg-rose-400/10 p-8">
      <p className="text-rose-100">{message}</p>
      <button
        type="button"
        onClick={onRetry}
        className="mt-5 rounded-full border border-white/10 bg-white/8 px-5 py-3 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
      >
        重新加载
      </button>
    </div>
  );
}
