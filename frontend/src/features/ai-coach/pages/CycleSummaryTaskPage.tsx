import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { getCycleSummaryTask } from "../api/ai-coach";
import { AiTaskStatusPanel } from "../components/AiTaskStatusPanel";
import { CycleSummaryResult } from "../components/CycleSummaryResult";
import { getAiCoachErrorMessage } from "../lib/ai-coach-enums";
import {
  getAiTaskPollDelayMs,
  isAiTaskTerminal
} from "../lib/ai-coach-polling";
import type { CycleSummaryTaskResponse } from "../types/ai-coach";

const backLinkClass =
  "inline-flex rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12";

export function CycleSummaryTaskPage() {
  const { taskId: rawTaskId } = useParams();
  const { accessToken } = useAuth();
  const [task, setTask] = useState<CycleSummaryTaskResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    const token = accessToken;
    const taskId = Number(rawTaskId);
    if (!Number.isInteger(taskId) || taskId <= 0) {
      setPageError("任务编号无效。");
      setIsLoading(false);
      return;
    }

    let cancelled = false;
    let timerId: number | null = null;

    async function loadTask(initialLoad: boolean) {
      if (initialLoad) {
        setIsLoading(true);
      }

      try {
        const nextTask = await getCycleSummaryTask(token, taskId);
        if (cancelled) {
          return;
        }

        setTask(nextTask);
        setPageError(null);

        if (!isAiTaskTerminal(nextTask.taskStatus)) {
          timerId = window.setTimeout(() => {
            void loadTask(false);
          }, getAiTaskPollDelayMs(nextTask));
        }
      } catch (error) {
        if (!cancelled) {
          setPageError(
            getAiCoachErrorMessage(error, "加载周期总结任务失败，请稍后再试。")
          );
        }
      } finally {
        if (!cancelled && initialLoad) {
          setIsLoading(false);
        }
      }
    }

    void loadTask(true);

    return () => {
      cancelled = true;
      if (timerId !== null) {
        window.clearTimeout(timerId);
      }
    };
  }, [accessToken, rawTaskId]);

  if (isLoading && !task) {
    return <LoadingState label="正在加载周期总结任务..." />;
  }

  return (
    <section className="space-y-8">
      <div className="flex flex-wrap gap-3">
        <Link to="/ai-coach/cycle-summary" className={backLinkClass}>
          返回周期总结
        </Link>
        <Link
          to="/ai-coach/history?tab=cycle-summaries"
          className={backLinkClass}
        >
          查看总结历史
        </Link>
      </div>

      {pageError ? <Notice tone="error">{pageError}</Notice> : null}

      {task ? <AiTaskStatusPanel task={task} /> : null}

      {task?.taskStatus === "succeeded" && task.result ? (
        <>
          <CycleSummaryResult result={task.result} />

          <div className="flex flex-wrap gap-3">
            <Link
              to={`/cycle-templates/${task.result.templateId}`}
              className="rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300"
            >
              查看对应模板
            </Link>
            <Link to="/workout" className={backLinkClass}>
              返回训练工作台
            </Link>
          </div>
        </>
      ) : null}
    </section>
  );
}

function LoadingState({ label }: { label: string }) {
  return (
    <div className="flex min-h-[40vh] items-center justify-center">
      <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
        {label}
      </div>
    </div>
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
