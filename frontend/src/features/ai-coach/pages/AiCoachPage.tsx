import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { getAiCoachCapabilities } from "../api/ai-coach";
import { AiCoachCapabilityCard } from "../components/AiCoachCapabilityCard";
import { AiCoachMissingFieldsNotice } from "../components/AiCoachMissingFieldsNotice";
import { AiCoachUnavailableState } from "../components/AiCoachUnavailableState";
import { getAiCoachErrorMessage } from "../lib/ai-coach-enums";
import {
  formatAiDateTime,
  formatCycleLengthRange
} from "../lib/ai-coach-formatters";
import type { AiCoachCapabilities } from "../types/ai-coach";

export function AiCoachPage() {
  const { accessToken } = useAuth();
  const [capabilities, setCapabilities] = useState<AiCoachCapabilities | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);

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
            getAiCoachErrorMessage(
              error,
              "加载 AI Coach 状态失败，请稍后再试。"
            )
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

  if (isLoading) {
    return <LoadingState label="正在加载 AI Coach..." />;
  }

  return (
    <section className="space-y-8">
      <header className="rounded-[36px] border border-white/10 bg-white/6 p-8 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
          AI Coach
        </p>
        <h1 className="mt-4 text-4xl font-semibold text-white sm:text-5xl">
          结构化 AI 训练助手
        </h1>
        <p className="mt-4 max-w-3xl leading-8 text-stone-300">
          这里不是开放式聊天，而是两个明确的 AI 入口：生成训练模板草稿，
          或为最近一轮已完成循环输出周期总结。你也可以随时回看历史任务。
        </p>
        {capabilities ? (
          <div className="mt-6 flex flex-wrap gap-2 text-xs text-stone-200">
            <InfoTag>{capabilities.accountTier}</InfoTag>
            <InfoTag>{capabilities.platformRole}</InfoTag>
            <InfoTag>{capabilities.aiEnabled ? "AI 已开通" : "AI 未开通"}</InfoTag>
          </div>
        ) : null}
      </header>

      {pageError ? <Notice tone="error">{pageError}</Notice> : null}

      {capabilities && !capabilities.aiEnabled ? (
        <AiCoachUnavailableState
          title="当前账号暂未开通 AI Coach"
          description="你仍然可以查看入口和能力范围，但提交 AI 任务前需要先开通对应权限。"
          actionLabel="返回控制台"
          actionTo="/app"
        />
      ) : null}

      {capabilities ? (
        <div className="grid gap-6 xl:grid-cols-2">
          <AiCoachCapabilityCard
            title="AI 生成训练模板"
            description="系统会读取你的资料和本次目标，生成一份可继续编辑的训练模板草稿。"
            available={capabilities.templateGeneration.available}
            ready={capabilities.templateGeneration.ready}
            ctaLabel="进入模板生成"
            to="/ai-coach/template-generation"
            meta={
              <div className="flex flex-wrap gap-2 text-xs text-stone-200">
                <InfoTag>
                  周期范围{" "}
                  {formatCycleLengthRange(
                    capabilities.templateGeneration.minCycleLength,
                    capabilities.templateGeneration.maxCycleLength
                  )}
                </InfoTag>
                <InfoTag>
                  缺失资料 {capabilities.templateGeneration.missingRequiredFields.length} 项
                </InfoTag>
              </div>
            }
          />

          <AiCoachCapabilityCard
            title="AI 周期总结"
            description="系统会围绕最近一轮已完成循环，输出亮点、问题、原因分析和下一轮调整方向。"
            available={capabilities.cycleSummary.available}
            ready={capabilities.cycleSummary.ready}
            ctaLabel="进入周期总结"
            to="/ai-coach/cycle-summary"
            meta={
              <div className="flex flex-wrap gap-2 text-xs text-stone-200">
                <InfoTag>
                  最近循环 ID #{capabilities.cycleSummary.latestCompletedCycleRunId ?? "--"}
                </InfoTag>
                <InfoTag>
                  完成时间 {formatAiDateTime(capabilities.cycleSummary.latestCompletedAt)}
                </InfoTag>
              </div>
            }
          />
        </div>
      ) : null}

      <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              History
            </p>
            <h2 className="mt-3 text-3xl font-semibold text-white">
              查看已提交任务
            </h2>
            <p className="mt-3 max-w-2xl leading-7 text-stone-300">
              模板生成和周期总结都会保留历史记录。你可以回看已完成结果，也能追踪仍在进行中的任务。
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <HistoryLink
              to="/ai-coach/history?tab=template-generations"
              label="模板生成历史"
            />
            <HistoryLink
              to="/ai-coach/history?tab=cycle-summaries"
              label="周期总结历史"
            />
          </div>
        </div>
      </section>

      {capabilities?.templateGeneration.missingRequiredFields.length ? (
        <AiCoachMissingFieldsNotice
          fields={capabilities.templateGeneration.missingRequiredFields}
          scene="ai-plan"
          redirectPath="/ai-coach/template-generation"
        />
      ) : null}

      {capabilities?.cycleSummary.recommendedMissingFields.length ? (
        <AiCoachMissingFieldsNotice
          fields={capabilities.cycleSummary.recommendedMissingFields}
          scene="ai-summary"
          redirectPath="/ai-coach/cycle-summary"
          title="补充更多资料可以提升总结准确度"
          description="周期总结在资料不完整时仍可继续，但补齐这些信息后，AI 会更容易理解你的训练背景。"
          actionLabel="去完善资料"
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

function InfoTag({ children }: { children: ReactNode }) {
  return (
    <span className="rounded-full border border-white/10 bg-white/8 px-3 py-1">
      {children}
    </span>
  );
}

function HistoryLink({ to, label }: { to: string; label: string }) {
  return (
    <Link
      to={to}
      className="inline-flex rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
    >
      {label}
    </Link>
  );
}
