import { useEffect, useRef, useState } from "react";
import type { AiTaskResponse, AiTaskStatus, AiTaskType } from "../types/ai-coach";
import { getAiCoachErrorMessage } from "./ai-coach-enums";

const DEFAULT_POLL_AFTER_SECONDS = 2;

export function isAiTaskTerminal(status: AiTaskStatus) {
  return status === "succeeded" || status === "failed";
}

export function getAiTaskPollDelayMs(
  task: Pick<AiTaskResponse<AiTaskType, unknown>, "pollAfterSeconds">
) {
  const pollAfterSeconds =
    typeof task.pollAfterSeconds === "number" && task.pollAfterSeconds > 0
      ? task.pollAfterSeconds
      : DEFAULT_POLL_AFTER_SECONDS;

  return pollAfterSeconds * 1000;
}

export type UseAiTaskPollingOptions<TTaskType extends AiTaskType, TResult> = {
  accessToken: string | null;
  taskId: number;
  loadTask: (
    token: string,
    taskId: number
  ) => Promise<AiTaskResponse<TTaskType, TResult>>;
};

/**
 * 轮询一个异步 AI 任务直到终态。
 * - 进入即请求一次；非终态按 pollAfterSeconds 递归定时。
 * - 终态或组件卸载即停止；失败经 getAiCoachErrorMessage 归一化为页面文案。
 * - loadTask 通过 ref 读取，避免每次渲染触发 effect 重跑。
 */
export function useAiTaskPolling<TTaskType extends AiTaskType, TResult>({
  accessToken,
  taskId,
  loadTask
}: UseAiTaskPollingOptions<TTaskType, TResult>) {
  const [task, setTask] = useState<AiTaskResponse<TTaskType, TResult> | null>(
    null
  );
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const loadTaskRef = useRef(loadTask);

  useEffect(() => {
    loadTaskRef.current = loadTask;
  }, [loadTask]);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    if (!Number.isInteger(taskId) || taskId <= 0) {
      setPageError("无效的任务地址。");
      setIsLoading(false);
      return;
    }

    const token = accessToken;
    let cancelled = false;
    let timerId: number | null = null;

    async function load(initialLoad: boolean) {
      if (initialLoad) {
        setIsLoading(true);
      }

      try {
        const nextTask = await loadTaskRef.current(token, taskId);
        if (cancelled) {
          return;
        }

        setTask(nextTask);
        setPageError(null);

        if (!isAiTaskTerminal(nextTask.taskStatus)) {
          timerId = window.setTimeout(() => {
            void load(false);
          }, getAiTaskPollDelayMs(nextTask));
        }
      } catch (error) {
        if (!cancelled) {
          setPageError(
            getAiCoachErrorMessage(error, "加载 AI 任务失败，请稍后再试。")
          );
        }
      } finally {
        if (!cancelled && initialLoad) {
          setIsLoading(false);
        }
      }
    }

    void load(true);

    return () => {
      cancelled = true;
      if (timerId !== null) {
        window.clearTimeout(timerId);
      }
    };
  }, [accessToken, taskId]);

  return { task, isLoading, pageError };
}
