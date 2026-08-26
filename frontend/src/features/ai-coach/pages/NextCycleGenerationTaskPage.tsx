import { useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { getNextCycleGenerationTask } from "../api/ai-coach";
import { AiTaskStatusPanel } from "../components/AiTaskStatusPanel";
import { TemplateGenerationResult } from "../components/TemplateGenerationResult";
import { useAiTaskPolling } from "../lib/ai-coach-polling";
import type { TemplateGenerationTaskResult } from "../types/ai-coach";

const backLinkClass =
  "inline-flex rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12";

export function NextCycleGenerationTaskPage() {
  const { taskId: rawTaskId } = useParams();
  const { accessToken } = useAuth();

  const taskId = useMemo(() => {
    const parsed = Number(rawTaskId);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : NaN;
  }, [rawTaskId]);

  const { task, isLoading, pageError } = useAiTaskPolling<
    "next_cycle_generation",
    TemplateGenerationTaskResult
  >({
    accessToken,
    taskId,
    loadTask: getNextCycleGenerationTask
  });

  const additionalRequirements = useMemo(() => {
    const snapshotValue = task?.requestSnapshot?.additionalRequirements?.trim();
    if (snapshotValue) {
      return snapshotValue;
    }

    return null;
  }, [task]);

  if (isLoading && !task) {
    return <LoadingState label="正在加载下一周期模板生成任务..." />;
  }

  return (
    <section className="space-y-8">
      <div className="flex flex-wrap gap-3">
        <Link
          to="/ai-coach/history?tab=cycle-summaries"
          className={backLinkClass}
        >
          查看周期总结历史
        </Link>
        <Link to="/ai-coach/template-generation" className={backLinkClass}>
          进入模板生成
        </Link>
      </div>

      {pageError ? <Notice tone="error">{pageError}</Notice> : null}

      {task ? <AiTaskStatusPanel task={task} /> : null}

      {additionalRequirements ? (
        <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
          <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
            Additional Requirements
          </p>
          <h2 className="mt-3 text-2xl font-semibold text-white">补充要求</h2>
          <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-stone-300">
            {additionalRequirements}
          </p>
        </section>
      ) : null}

      {task?.taskStatus === "succeeded" && task.result ? (
        <>
          <TemplateGenerationResult result={task.result} />

          <div className="flex flex-wrap gap-3">
            <Link
              to={`/cycle-templates/${task.result.draftTemplate.templateId}/edit`}
              className="rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300"
            >
              去编辑草稿
            </Link>
            <Link to="/cycle-templates" className={backLinkClass}>
              返回模板列表
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
