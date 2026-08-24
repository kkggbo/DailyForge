import {
  formatAiDateTime,
  formatAiTaskProgressDescription,
  formatAiTaskProgressStage,
  formatAiTaskStatus,
  formatAiToolCallStatus
} from "../lib/ai-coach-formatters";
import type { AiTaskBase } from "../types/ai-coach";

type AiTaskStatusPanelProps = {
  task: AiTaskBase;
};

export function AiTaskStatusPanel({ task }: AiTaskStatusPanelProps) {
  const isActive = task.taskStatus === "pending" || task.taskStatus === "running";
  const isSucceeded = task.taskStatus === "succeeded";
  const latestToolCallName =
    task.latestToolCall?.toolDisplayName?.trim() || task.latestToolCall?.toolName;

  return (
    <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
            AI Task
          </p>
          <h2 className="mt-3 text-3xl font-semibold text-white">
            {formatAiTaskStatus(task.taskStatus)}
          </h2>
          <p className="mt-2 text-sm text-stone-300">
            {task.taskType === "cycle_summary" ? "周期总结任务" : "模板生成任务"}
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <StatusPill tone={task.taskStatus === "failed" ? "danger" : "default"}>
            {formatAiTaskStatus(task.taskStatus)}
          </StatusPill>
          {isSucceeded ? null : (
            <StatusPill
              tone={task.progressStage === "failed" ? "danger" : "default"}
            >
              {formatAiTaskProgressStage(task.progressStage)}
            </StatusPill>
          )}
        </div>
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-4">
        <InfoItem label="创建时间" value={formatAiDateTime(task.createdAt)} />
        <InfoItem label="开始时间" value={formatAiDateTime(task.startedAt)} />
        <InfoItem label="最近更新" value={formatAiDateTime(task.updatedAt)} />
        <InfoItem label="完成时间" value={formatAiDateTime(task.completedAt)} />
      </div>

      {isSucceeded ? null : (
        <div className="mt-4 rounded-3xl border border-white/10 bg-black/20 p-4">
          <p className="text-xs uppercase tracking-[0.18em] text-stone-500">
            当前阶段
          </p>
          <p className="mt-2 text-lg font-semibold text-white">
            {formatAiTaskProgressStage(task.progressStage)}
          </p>
          <p className="mt-2 text-sm leading-6 text-stone-300">
            {formatAiTaskProgressDescription(task.progressStage)}
          </p>
          {isActive ? (
            <p className="mt-3 text-xs text-amber-200">
              页面会自动刷新，直到任务进入完成或失败状态。
            </p>
          ) : null}
        </div>
      )}

      {isSucceeded ? null : (
        <div className="mt-4 rounded-3xl border border-white/10 bg-black/20 p-4">
          <p className="text-xs uppercase tracking-[0.18em] text-stone-500">
            最近工具调用
          </p>
          {task.latestToolCall && latestToolCallName ? (
            <div className="mt-3 space-y-3">
              <div className="flex flex-wrap items-center gap-2">
                <code className="rounded-full border border-white/10 bg-white/8 px-3 py-1 text-xs text-stone-100">
                  {latestToolCallName}
                </code>
                <span className="text-xs text-stone-400">
                  第 {task.latestToolCall.roundNo} 轮
                </span>
                <StatusPill
                  tone={
                    task.latestToolCall.status === "failed" ? "danger" : "default"
                  }
                >
                  {formatAiToolCallStatus(task.latestToolCall.status)}
                </StatusPill>
              </div>
              <p className="text-sm text-stone-300">
                调用时间：{formatAiDateTime(task.latestToolCall.createdAt)}
              </p>
            </div>
          ) : (
            <p className="mt-3 text-sm text-stone-300">
              {isActive
                ? "当前还没有可展示的工具调用痕迹。"
                : "本次任务没有记录到可展示的工具调用信息。"}
            </p>
          )}
        </div>
      )}

      {task.errorCode || task.errorMessage ? (
        <div className="mt-5 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {task.errorCode ? `${task.errorCode} · ` : null}
          {task.errorMessage ?? "任务执行失败。"}
        </div>
      ) : null}
    </section>
  );
}

function StatusPill({
  children,
  tone
}: {
  children: string;
  tone: "default" | "danger";
}) {
  return (
    <span
      className={[
        "rounded-full px-3 py-1 text-xs",
        tone === "danger"
          ? "bg-rose-400/15 text-rose-200"
          : "bg-amber-400/15 text-amber-200"
      ].join(" ")}
    >
      {children}
    </span>
  );
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-3xl border border-white/10 bg-black/20 p-4">
      <p className="text-xs uppercase tracking-[0.18em] text-stone-500">{label}</p>
      <p className="mt-3 text-sm text-stone-100">{value}</p>
    </div>
  );
}
