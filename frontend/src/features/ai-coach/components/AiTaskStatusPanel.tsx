import { formatAiDateTime, formatAiTaskStatus } from "../lib/ai-coach-formatters";
import type { AiTaskBase } from "../types/ai-coach";

type AiTaskStatusPanelProps = {
  task: AiTaskBase;
};

export function AiTaskStatusPanel({ task }: AiTaskStatusPanelProps) {
  return (
    <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
            AI Task
          </p>
          <h2 className="mt-3 text-3xl font-semibold text-white">
            {formatAiTaskStatus(task.taskStatus)}
          </h2>
          <p className="mt-2 text-sm text-stone-300">
            任务编号 #{task.taskId} · {task.taskType}
          </p>
        </div>
        <span
          className={[
            "rounded-full px-3 py-1 text-xs",
            task.taskStatus === "failed"
              ? "bg-rose-400/15 text-rose-200"
              : "bg-amber-400/15 text-amber-200"
          ].join(" ")}
        >
          {formatAiTaskStatus(task.taskStatus)}
        </span>
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <InfoItem label="创建时间" value={formatAiDateTime(task.createdAt)} />
        <InfoItem label="开始时间" value={formatAiDateTime(task.startedAt)} />
        <InfoItem label="完成时间" value={formatAiDateTime(task.completedAt)} />
      </div>

      {task.errorCode || task.errorMessage ? (
        <div className="mt-5 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {task.errorCode ? `${task.errorCode} · ` : null}
          {task.errorMessage ?? "任务执行失败。"}
        </div>
      ) : null}
    </section>
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
