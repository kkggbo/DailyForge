import type { AiTaskStatus, AiTaskBase } from "../types/ai-coach";

const DEFAULT_POLL_AFTER_SECONDS = 2;

export function isAiTaskTerminal(status: AiTaskStatus) {
  return status === "succeeded" || status === "failed";
}

export function getAiTaskPollDelayMs(task: Pick<AiTaskBase, "pollAfterSeconds">) {
  const pollAfterSeconds =
    typeof task.pollAfterSeconds === "number" && task.pollAfterSeconds > 0
      ? task.pollAfterSeconds
      : DEFAULT_POLL_AFTER_SECONDS;

  return pollAfterSeconds * 1000;
}
