import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import {
  createTemplateGenerationTask,
  getAiCoachCapabilities
} from "../api/ai-coach";
import { AiCoachMissingFieldsNotice } from "../components/AiCoachMissingFieldsNotice";
import { AiCoachUnavailableState } from "../components/AiCoachUnavailableState";
import { TemplateGenerationForm } from "../components/TemplateGenerationForm";
import { getAiCoachErrorMessage } from "../lib/ai-coach-enums";
import {
  formatCycleLengthRange
} from "../lib/ai-coach-formatters";
import { normalizeOptionalText } from "../lib/ai-coach-mappers";
import type {
  AiCoachCapabilities,
  TemplateGenerationForm as TemplateGenerationFormValues
} from "../types/ai-coach";

const secondaryLinkClass =
  "inline-flex rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12";

export function TemplateGenerationPage() {
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
            getAiCoachErrorMessage(
              error,
              "加载模板生成状态失败，请稍后再试。"
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

  async function handleSubmit(form: TemplateGenerationFormValues) {
    if (!accessToken || !capabilities) {
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const acceptedTask = await createTemplateGenerationTask(accessToken, {
        clientRequestId: crypto.randomUUID(),
        sceneType: form.sceneType,
        goalType: form.goalType,
        cycleLength: Number(form.cycleLengthText),
        includeCardio: form.includeCardio,
        additionalRequirements: normalizeOptionalText(form.additionalRequirements)
      });

      navigate(`/ai-coach/template-generation/tasks/${acceptedTask.taskId}`, {
        replace: true
      });
    } catch (error) {
      setSubmitError(
        getAiCoachErrorMessage(
          error,
          "提交模板生成任务失败，请稍后再试。"
        )
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoading) {
    return <LoadingState label="正在准备模板生成页面..." />;
  }

  return (
    <section className="space-y-8">
      <div className="flex flex-wrap gap-3">
        <Link to="/ai-coach" className={secondaryLinkClass}>
          返回 AI Coach
        </Link>
        <Link
          to="/ai-coach/history?tab=template-generations"
          className={secondaryLinkClass}
        >
          查看生成历史
        </Link>
      </div>

      <header className="rounded-[36px] border border-white/10 bg-white/6 p-8 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
          Template Generation
        </p>
        <h1 className="mt-4 text-4xl font-semibold text-white sm:text-5xl">
          AI 生成训练模板草稿
        </h1>
        <p className="mt-4 max-w-3xl leading-8 text-stone-300">
          提交后会创建一条异步任务。任务成功后，你会同时看到草稿模板和只读的
          AI 设计说明。
        </p>
      </header>

      {pageError ? <Notice tone="error">{pageError}</Notice> : null}

      {capabilities && !capabilities.templateGeneration.available ? (
        <AiCoachUnavailableState
          title="当前账号暂时不能使用模板生成"
          description="入口仍然可见，但当前账号还没有调用 AI 模板生成的权限。"
          actionLabel="返回 AI Coach"
          actionTo="/ai-coach"
        />
      ) : null}

      {capabilities ? (
        <section className="rounded-[28px] border border-white/10 bg-black/20 p-5">
          <div className="flex flex-wrap gap-2 text-xs text-stone-200">
            <Tag>
              周期范围{" "}
              {formatCycleLengthRange(
                capabilities.templateGeneration.minCycleLength,
                capabilities.templateGeneration.maxCycleLength
              )}
            </Tag>
            <Tag>
              当前状态{" "}
              {capabilities.templateGeneration.ready ? "资料已满足要求" : "仍需补资料"}
            </Tag>
          </div>
        </section>
      ) : null}

      {capabilities && !capabilities.templateGeneration.ready ? (
        <AiCoachMissingFieldsNotice
          fields={capabilities.templateGeneration.missingRequiredFields}
          scene="ai-plan"
          redirectPath="/ai-coach/template-generation"
          description="模板生成会严格检查必要资料。先补齐这些信息，再回来发起任务。"
        />
      ) : null}

      {capabilities?.templateGeneration.available &&
      capabilities.templateGeneration.ready ? (
        <TemplateGenerationForm
          capability={capabilities.templateGeneration}
          isSubmitting={isSubmitting}
          submitError={submitError}
          onSubmit={(form) => {
            void handleSubmit(form);
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

function Tag({ children }: { children: ReactNode }) {
  return (
    <span className="rounded-full border border-white/10 bg-white/8 px-3 py-1">
      {children}
    </span>
  );
}
