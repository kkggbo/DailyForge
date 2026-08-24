import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { ApiRequestError } from "../../../shared/api/http";
import { getLatestCycleSummaryTaskByCycleRun } from "../../ai-coach/api/ai-coach";
import {
  completeSession,
  getDay,
  getRecent,
  getWorkspace,
  initializeCurrentDay,
  restartCycle,
  saveSession
} from "../api/workout";
import {
  DayNavigator,
  RecentList,
  SessionEditor,
  SessionReadOnly
} from "../components/WorkoutPanel";
import { errorMessage, formatTime, metricLabel, metricUnitLabel } from "../lib/workout";
import type {
  DayDetail,
  RecentWorkouts,
  SavePayload,
  Workspace
} from "../types/workout";

const secondaryButton =
  "inline-flex rounded-full border border-white/10 bg-white/8 px-5 py-3 text-sm font-semibold text-stone-100 transition hover:bg-white/12";

export function WorkoutPage() {
  const navigate = useNavigate();
  const { accessToken } = useAuth();
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [detail, setDetail] = useState<DayDetail | null>(null);
  const [recent, setRecent] = useState<RecentWorkouts | null>(null);
  const [selectedDayIndex, setSelectedDayIndex] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingDay, setIsLoadingDay] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);
  const [isRestarting, setIsRestarting] = useState(false);
  const [isOpeningCycleSummary, setIsOpeningCycleSummary] = useState(false);
  const [pageError, setPageError] = useState<string | null>(null);
  const [recentError, setRecentError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [completedCycleOnCurrentDay, setCompletedCycleOnCurrentDay] =
    useState(false);
  const dayViewRequestSequence = useRef(0);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    const token = accessToken;
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setPageError(null);
      setActionError(null);
      setMessage(null);
      setCompletedCycleOnCurrentDay(false);

      try {
        const nextWorkspace = await getWorkspace(token);
        if (cancelled) {
          return;
        }

        setWorkspace(nextWorkspace);
        setDetail(null);
        setSelectedDayIndex(nextWorkspace.defaultDayIndex);

        if (
          nextWorkspace.workspaceState === "active" &&
          nextWorkspace.defaultDayIndex !== null
        ) {
          try {
            const initialized = await initializeCurrentDay(token);
            if (!cancelled) {
              setDetail(initialized.day);
            }
          } catch (error) {
            if (
              error instanceof ApiRequestError &&
              error.code === "WORKOUT_CYCLE_COMPLETED"
            ) {
              const refreshed = await getWorkspace(token);
              if (!cancelled) {
                setWorkspace(refreshed);
                setSelectedDayIndex(refreshed.defaultDayIndex);
              }
            } else {
              throw error;
            }
          }
        }

        void loadRecent(token, cancelled);
      } catch (error) {
        if (!cancelled) {
          setPageError(errorMessage(error, "加载训练工作台失败，请稍后重试。"));
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  async function loadRecent(token: string, cancelled = false) {
    setRecentError(null);

    try {
      const response = await getRecent(token);
      if (!cancelled) {
        setRecent(response);
      }
    } catch (error) {
      if (!cancelled) {
        setRecentError(errorMessage(error, "加载最近训练记录失败。"));
      }
    }
  }

  async function selectDay(dayIndex: number) {
    if (!accessToken || dayIndex === selectedDayIndex) {
      return;
    }

    const requestSequence = ++dayViewRequestSequence.current;
    setSelectedDayIndex(dayIndex);
    setIsLoadingDay(true);
    setPageError(null);
    setActionError(null);
    setMessage(null);
    setCompletedCycleOnCurrentDay(false);

    try {
      const nextDetail = await getDay(accessToken, dayIndex);
      if (requestSequence === dayViewRequestSequence.current) {
        setDetail(nextDetail);
      }
    } catch (error) {
      if (requestSequence === dayViewRequestSequence.current) {
        setPageError(errorMessage(error, "加载该训练日失败，请稍后重试。"));
      }
    } finally {
      if (requestSequence === dayViewRequestSequence.current) {
        setIsLoadingDay(false);
      }
    }
  }

  async function save(payload: SavePayload) {
    if (!accessToken || !detail?.session) {
      return;
    }

    setIsSaving(true);
    setActionError(null);
    setMessage(null);

    try {
      const response = await saveSession(
        accessToken,
        detail.session.sessionId,
        payload
      );
      setMessage(`已保存于 ${formatTime(response.savedAt)}。`);
    } catch (error) {
      setActionError(errorMessage(error, "保存训练记录失败，请稍后重试。"));
    } finally {
      setIsSaving(false);
    }
  }

  async function complete(payload: SavePayload) {
    if (!accessToken || !detail?.session) {
      return;
    }

    const requestSequence = ++dayViewRequestSequence.current;
    setIsCompleting(true);
    setActionError(null);
    setMessage(null);

    try {
      const response = await completeSession(
        accessToken,
        detail.session.sessionId,
        payload
      );
      if (requestSequence !== dayViewRequestSequence.current) {
        return;
      }

      setSelectedDayIndex(response.completedDayIndex);
      setDetail(response.completedDay);
      setCompletedCycleOnCurrentDay(response.cycleRunStatus === "completed");
      setMessage(
        response.cycleRunStatus === "completed"
          ? "本 Day 已完成，当前循环也已完成。刷新或重新进入后可选择下一步。"
          : "本 Day 已完成。当前页面保留在这条只读记录上。"
      );
      void loadRecent(accessToken);
    } catch (error) {
      if (requestSequence === dayViewRequestSequence.current) {
        setActionError(errorMessage(error, "完成训练打卡失败，请稍后重试。"));
      }
    } finally {
      setIsCompleting(false);
    }
  }

  async function restart() {
    if (!accessToken) {
      return;
    }

    setIsRestarting(true);
    setActionError(null);
    setMessage(null);

    try {
      await restartCycle(accessToken);
      const nextWorkspace = await getWorkspace(accessToken);
      setWorkspace(nextWorkspace);
      setSelectedDayIndex(nextWorkspace.defaultDayIndex);
      setCompletedCycleOnCurrentDay(false);

      if (
        nextWorkspace.workspaceState === "active" &&
        nextWorkspace.defaultDayIndex !== null
      ) {
        const initialized = await initializeCurrentDay(accessToken);
        setDetail(initialized.day);
      }
    } catch (error) {
      setActionError(errorMessage(error, "重新开始当前循环失败，请稍后重试。"));
    } finally {
      setIsRestarting(false);
    }
  }

  async function openCycleSummary() {
    if (!accessToken || !workspace?.cycleRunId) {
      return;
    }

    setIsOpeningCycleSummary(true);
    setActionError(null);
    setMessage(null);

    try {
      const task = await getLatestCycleSummaryTaskByCycleRun(
        accessToken,
        workspace.cycleRunId
      );
      navigate(`/ai-coach/cycle-summary/tasks/${task.taskId}`);
    } catch (error) {
      if (error instanceof ApiRequestError && error.code === "AI_TASK_NOT_FOUND") {
        navigate("/ai-coach/cycle-summary");
        return;
      }

      setActionError(errorMessage(error, "打开 AI 周期总结失败，请稍后重试。"));
    } finally {
      setIsOpeningCycleSummary(false);
    }
  }

  if (isLoading) {
    return <Loading label="正在加载训练工作台..." />;
  }

  if (!workspace) {
    return <ErrorPanel message={pageError ?? "训练工作台当前不可用。"} />;
  }

  return (
    <section className="space-y-8">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
            Workout
          </p>
          <h1 className="mt-3 text-4xl font-semibold text-white sm:text-5xl">
            训练工作台
          </h1>
          <p className="mt-3 max-w-3xl leading-7 text-stone-300">
            记录实际训练表现。仅默认当前 Day 会自动初始化；浏览历史和未来 Day
            不会创建训练记录。
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <Link to="/ai-coach/history?tab=cycle-summaries" className={secondaryButton}>
            周期总结历史
          </Link>
          <Link to="/cycle-templates" className={secondaryButton}>
            管理训练模板
          </Link>
        </div>
      </header>

      {workspace.workspaceState === "no_active_template" ? <NoTemplate /> : null}
      {workspace.workspaceState === "cycle_completed" ? (
        <CycleComplete
          isRestarting={isRestarting}
          isOpeningCycleSummary={isOpeningCycleSummary}
          message={message}
          error={actionError}
          onRestart={() => void restart()}
          onOpenCycleSummary={() => void openCycleSummary()}
        />
      ) : null}

      {workspace.workspaceState === "active" ? (
        <>
          <section className="rounded-[32px] border border-white/10 bg-white/6 p-6">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <p className="text-sm uppercase tracking-[0.22em] text-amber-300">
                  {workspace.templateName}
                </p>
                <h2 className="mt-2 text-2xl font-semibold text-white">
                  第 {workspace.runNo} 轮 · 当前进度 Day{" "}
                  {workspace.currentDayIndex}
                </h2>
              </div>
              <span className="rounded-full border border-white/10 bg-black/20 px-3 py-1 text-sm text-stone-300">
                共 {workspace.cycleLength} 天
              </span>
            </div>
            <div className="mt-5">
              <DayNavigator
                days={workspace.days}
                selectedDayIndex={selectedDayIndex}
                disabled={isLoadingDay || isCompleting}
                onSelect={(dayIndex) => void selectDay(dayIndex)}
              />
            </div>
          </section>

          {pageError ? <Notice tone="error">{pageError}</Notice> : null}
          {message ? <Notice>{message}</Notice> : null}
          {completedCycleOnCurrentDay ? (
            <Notice>
              当前循环已完成。刷新或重新进入后会显示循环结束选项；当前记录保留为只读。
            </Notice>
          ) : null}

          {isLoadingDay ? (
            <Loading label="正在加载训练日..." />
          ) : detail ? (
            <DayPanel
              detail={detail}
              isSaving={isSaving}
              isCompleting={isCompleting}
              error={actionError}
              onSave={save}
              onComplete={complete}
            />
          ) : (
            <Notice>当前训练日尚未加载完成。</Notice>
          )}
        </>
      ) : null}

      <RecentList data={recent} error={recentError} />
    </section>
  );
}

function DayPanel({
  detail,
  isSaving,
  isCompleting,
  error,
  onSave,
  onComplete
}: {
  detail: DayDetail;
  isSaving: boolean;
  isCompleting: boolean;
  error: string | null;
  onSave: (payload: SavePayload) => void;
  onComplete: (payload: SavePayload) => void;
}) {
  const editableSession = detail.viewMode === "editable" ? detail.session : null;

  return (
    <section className="space-y-5">
      <div className="rounded-[32px] border border-white/10 bg-black/20 p-6">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.22em] text-amber-300">
              Day {detail.dayIndex} · {detail.isRestDay ? "Rest Day" : "Training Day"}
            </p>
            <h2 className="mt-2 text-3xl font-semibold text-white">
              {detail.dayName}
            </h2>
            <p className="mt-2 text-stone-300">
              {detail.viewMode === "preview"
                ? "未来训练计划预览，不会创建 session。"
                : detail.viewMode === "readonly"
                  ? "已完成记录仅供查看，不能编辑。"
                  : "必要时记录与计划不一致的实际值后，可手动保存或完成打卡。"}
            </p>
          </div>
          <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-sm text-stone-300">
            {detail.viewMode === "editable"
              ? "可编辑"
              : detail.viewMode === "readonly"
                ? "只读"
                : "计划预览"}
          </span>
        </div>
      </div>

      {editableSession ? (
        <SessionEditor
          session={editableSession}
          isSaving={isSaving}
          isCompleting={isCompleting}
          error={error}
          onSave={onSave}
          onComplete={onComplete}
        />
      ) : null}

      {detail.viewMode === "readonly" && detail.session ? (
        <SessionReadOnly session={detail.session} />
      ) : null}

      {detail.viewMode === "preview" ? (
        <Preview exercises={detail.exercises ?? []} rest={detail.isRestDay} />
      ) : null}
    </section>
  );
}

function Preview({
  exercises,
  rest
}: {
  exercises: NonNullable<DayDetail["exercises"]>;
  rest: boolean;
}) {
  if (rest) {
    return (
      <Notice>这是未来休息日预览。到达该 Day 前不会创建休息日打卡记录。</Notice>
    );
  }

  return (
    <div className="space-y-4">
      {exercises.map((exercise) => (
        <article
          key={`${exercise.exerciseId}-${exercise.sortOrder}`}
          className="rounded-[28px] border border-white/10 bg-white/6 p-5"
        >
          <p className="text-xs uppercase tracking-[0.2em] text-amber-300">
            动作 {exercise.sortOrder}
          </p>
          <h3 className="mt-1 text-xl font-semibold text-white">
            {exercise.exerciseName}
          </h3>
          <div className="mt-4 space-y-2">
            {exercise.items.map((item) => (
              <div
                key={item.itemIndex}
                className="rounded-2xl border border-white/10 bg-black/20 p-4"
              >
                <p className="font-medium text-stone-100">
                  {item.itemName ?? `执行项 ${item.itemIndex}`}
                </p>
                <div className="mt-2 flex flex-wrap gap-2 text-sm text-stone-300">
                  {item.metrics.map((metric) => (
                    <span
                      key={metric.metricKey}
                      className="rounded-full bg-white/8 px-3 py-1"
                    >
                      {metricLabel(metric.metricKey)}：{metric.plannedValueNumber ?? "未设定"}{" "}
                      {metricUnitLabel(metric.metricUnit)}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </article>
      ))}
    </div>
  );
}

function NoTemplate() {
  return (
    <section className="rounded-[32px] border border-dashed border-white/15 bg-white/6 p-8 text-center">
      <p className="text-sm uppercase tracking-[0.28em] text-amber-300">Workout</p>
      <h2 className="mt-3 text-3xl font-semibold text-white">先启用一个训练模板</h2>
      <p className="mx-auto mt-3 max-w-xl leading-7 text-stone-300">
        训练工作台会按当前模板的循环进度创建和保存训练记录，不支持自由训练打卡。
      </p>
      <Link
        to="/cycle-templates"
        className="mt-6 inline-flex rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300"
      >
        前往训练模板
      </Link>
    </section>
  );
}

function CycleComplete({
  isRestarting,
  isOpeningCycleSummary,
  message,
  error,
  onRestart,
  onOpenCycleSummary
}: {
  isRestarting: boolean;
  isOpeningCycleSummary: boolean;
  message: string | null;
  error: string | null;
  onRestart: () => void;
  onOpenCycleSummary: () => void;
}) {
  return (
    <section className="rounded-[32px] border border-amber-300/25 bg-amber-300/10 p-8">
      <p className="text-sm uppercase tracking-[0.28em] text-amber-200">
        Cycle Complete
      </p>
      <h2 className="mt-3 text-3xl font-semibold text-white">这一轮训练已完成</h2>
      <p className="mt-3 max-w-2xl leading-7 text-stone-200">
        系统不会自动开启下一轮。你可以沿用当前模板重新开始、切换模板，或进入
        AI 周期总结。若当前循环已有历史总结，会直接打开对应结果。
      </p>
      <div className="mt-6 flex flex-wrap gap-3">
        <button
          type="button"
          disabled={isRestarting}
          onClick={onRestart}
          className="rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
        >
          {isRestarting ? "重启中..." : "再次使用当前模板"}
        </button>
        <Link to="/cycle-templates" className={secondaryButton}>
          选择其他模板
        </Link>
        <button
          type="button"
          disabled={isOpeningCycleSummary}
          onClick={onOpenCycleSummary}
          className={secondaryButton}
        >
          {isOpeningCycleSummary ? "检查总结中..." : "AI 周期总结"}
        </button>
      </div>
      {message ? <Notice>{message}</Notice> : null}
      {error ? <Notice tone="error">{error}</Notice> : null}
    </section>
  );
}

function Notice({
  children,
  tone = "info"
}: {
  children: string;
  tone?: "info" | "error";
}) {
  return (
    <div
      className={[
        "rounded-2xl border px-4 py-3 text-sm",
        tone === "error"
          ? "border-rose-400/20 bg-rose-400/10 text-rose-100"
          : "border-amber-300/20 bg-amber-300/10 text-amber-100"
      ].join(" ")}
    >
      {children}
    </div>
  );
}

function Loading({ label }: { label: string }) {
  return (
    <div className="flex min-h-[32vh] items-center justify-center">
      <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
        {label}
      </div>
    </div>
  );
}

function ErrorPanel({ message }: { message: string }) {
  return (
    <section className="space-y-5">
      <Notice tone="error">{message}</Notice>
      <button
        type="button"
        onClick={() => window.location.reload()}
        className={secondaryButton}
      >
        重新加载
      </button>
    </section>
  );
}
