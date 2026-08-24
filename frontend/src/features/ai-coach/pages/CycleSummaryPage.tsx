import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { createCycleSummaryTask, getAiCoachCapabilities } from "../api/ai-coach";
import { AiCoachMissingFieldsNotice } from "../components/AiCoachMissingFieldsNotice";
import { AiCoachUnavailableState } from "../components/AiCoachUnavailableState";
import { CycleSummaryLaunchCard } from "../components/CycleSummaryLaunchCard";
import { getAiCoachErrorMessage } from "../lib/ai-coach-enums";
import { generateUuid } from "../../../shared/lib/uuid";
import type { AiCoachCapabilities } from "../types/ai-coach";

const backLinkClass =
  "inline-flex rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12";

export function CycleSummaryPage() {
  const navigate = useNavigate();
  const { accessToken } = useAuth();
  const [capabilities, setCapabilities] = useState<AiCoachCapabilities | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    const token = accessToken;
    let cancelled = false;

    async function loadPage() {
      setIsLoading(true);
      setPageError(null);

      try {
        const nextCapabilities = await getAiCoachCapabilities(token);
        if (!cancelled) {
          setCapabilities(nextCapabilities);
        }
      } catch (error) {
        if (!cancelled) {
          setPageError(
            getAiCoachErrorMessage(error, "加载周期总结状态失败，请稍后再试。")
          );
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadPage();

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  async function handleSubmit() {
    if (!accessToken || !capabilities?.cycleSummary.latestCompletedCycleRunId) {
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const acceptedTask = await createCycleSummaryTask(accessToken, {
        clientRequestId: generateUuid(),
        cycleRunId: capabilities.cycleSummary.latestCompletedCycleRunId
      });

      navigate(`/ai-coach/cycle-summary/tasks/${acceptedTask.taskId}`, {
        replace: true
      });
    } catch (error) {
      setSubmitError(
        getAiCoachErrorMessage(error, "提交周期总结任务失败，请稍后再试。")
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoading) {
    return <LoadingState label="正在准备周期总结页面..." />;
  }

  return (
    <section className="space-y-8">
      <div className="flex flex-wrap gap-3">
        <Link
          to="/ai-coach/history?tab=cycle-summaries"
          className={backLinkClass}
        >
          查看总结历史
        </Link>
        <Link to="/workout" className={backLinkClass}>
          返回训练工作台
        </Link>
      </div>

      <header className="rounded-[36px] border border-white/10 bg-white/6 p-8 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
          Cycle Summary
        </p>
        <h1 className="mt-4 text-4xl font-semibold text-white sm:text-5xl">
          AI 周期总结
        </h1>
        <p className="mt-4 max-w-3xl leading-8 text-stone-300">
          当前版本只围绕最近一轮已完成循环生成总结，并给出下一轮训练方向，不会自动修改模板。
        </p>
      </header>

      {pageError ? <Notice tone="error">{pageError}</Notice> : null}

      {capabilities && !capabilities.cycleSummary.available ? (
        <AiCoachUnavailableState
          title="当前账号暂时不能使用周期总结"
          description="入口仍然保留，但当前账号还没有发起 AI 周期总结任务的权限。"
          actionLabel="返回训练工作台"
          actionTo="/workout"
        />
      ) : null}

      {capabilities && !capabilities.cycleSummary.ready ? (
        <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
          <h2 className="text-2xl font-semibold text-white">暂时没有可分析的已完成循环</h2>
          <p className="mt-3 max-w-2xl leading-7 text-stone-300">
            当前至少需要存在一轮「已完成」状态的循环，AI 才能基于它输出总结。
          </p>
        </section>
      ) : null}

      {capabilities?.cycleSummary.recommendedMissingFields.length ? (
        <AiCoachMissingFieldsNotice
          fields={capabilities.cycleSummary.recommendedMissingFields}
          scene="ai-summary"
          redirectPath="/ai-coach/cycle-summary"
          title="补齐这些资料会让总结更准确"
          description="周期总结允许继续执行，但补齐这些资料后，AI 会更容易判断你的训练背景和恢复状态。"
          actionLabel="去完善资料"
        />
      ) : null}

      {capabilities?.cycleSummary.available && capabilities.cycleSummary.ready ? (
        <CycleSummaryLaunchCard
          capability={capabilities.cycleSummary}
          isSubmitting={isSubmitting}
          submitError={submitError}
          onSubmit={() => {
            void handleSubmit();
          }}
        />
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
