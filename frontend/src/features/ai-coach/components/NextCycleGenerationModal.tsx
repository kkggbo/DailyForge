import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { getCycleTemplateDetail } from "../../cycle-template/api/cycle-template";
import { generateUuid } from "../../../shared/lib/uuid";
import {
  createNextCycleGenerationTask,
  getAiCoachCapabilities
} from "../api/ai-coach";
import { getAiCoachErrorMessage } from "../lib/ai-coach-enums";
import { normalizeOptionalText } from "../lib/ai-coach-mappers";
import {
  buildNextCycleInitialValues,
  buildNextCyclePrefill,
  type NextCyclePrefill
} from "../lib/next-cycle-generation";
import { TemplateGenerationForm } from "./TemplateGenerationForm";
import type {
  TemplateGenerationCapability,
  TemplateGenerationForm as TemplateGenerationFormValues
} from "../types/ai-coach";

type NextCycleGenerationModalProps = {
  open: boolean;
  onClose: () => void;
  sourceCycleRunId: number;
  sourceSummaryTaskId: number | null;
  prefillTemplateId: number | null;
};

export function NextCycleGenerationModal({
  open,
  onClose,
  sourceCycleRunId,
  sourceSummaryTaskId,
  prefillTemplateId
}: NextCycleGenerationModalProps) {
  const navigate = useNavigate();
  const { accessToken } = useAuth();
  const [capability, setCapability] =
    useState<TemplateGenerationCapability | null>(null);
  const [prefill, setPrefill] = useState<NextCyclePrefill | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [pageError, setPageError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !accessToken) {
      return;
    }

    const token = accessToken;
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setPageError(null);

      try {
        const [nextCapabilities, templateDetail] = await Promise.all([
          getAiCoachCapabilities(token),
          prefillTemplateId != null
            ? getCycleTemplateDetail(token, prefillTemplateId)
            : null
        ]);

        if (cancelled) {
          return;
        }

        setCapability(nextCapabilities.templateGeneration);
        setPrefill(
          templateDetail ? buildNextCyclePrefill(templateDetail) : null
        );
      } catch (error) {
        if (!cancelled) {
          setPageError(
            getAiCoachErrorMessage(error, "加载生成条件失败，请稍后再试。")
          );
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
  }, [open, accessToken, prefillTemplateId]);

  const initialValues = useMemo(() => {
    if (!prefill) {
      return undefined;
    }

    return buildNextCycleInitialValues(prefill);
  }, [prefill]);

  async function handleSubmit(form: TemplateGenerationFormValues) {
    if (!accessToken || !capability) {
      return;
    }
    if (!sourceCycleRunId || sourceCycleRunId <= 0) {
      setSubmitError("无法定位上一周期，请返回后重试。");
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const acceptedTask = await createNextCycleGenerationTask(accessToken, {
        clientRequestId: generateUuid(),
        sourceCycleRunId,
        sourceSummaryTaskId,
        sceneType: form.sceneType,
        goalType: form.goalType,
        cycleLength: Number(form.cycleLengthText),
        includeCardio: form.includeCardio,
        additionalRequirements: normalizeOptionalText(
          form.additionalRequirements
        )
      });

      onClose();
      navigate(
        `/ai-coach/next-cycle-generation/tasks/${acceptedTask.taskId}`,
        { replace: true }
      );
    } catch (error) {
      setSubmitError(
        getAiCoachErrorMessage(error, "提交下一周期模板生成任务失败，请稍后再试。")
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 px-4 py-8 backdrop-blur">
      <section className="flex h-[min(88vh,840px)] w-full max-w-3xl flex-col overflow-hidden rounded-[32px] border border-white/10 bg-stone-950 shadow-2xl shadow-black/40">
        <div className="flex items-start justify-between gap-4 border-b border-white/10 px-6 py-5">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              Next Cycle Generation
            </p>
            <h2 className="mt-2 text-2xl font-semibold text-white">
              生成下一周期模板
            </h2>
            <p className="mt-2 text-sm leading-6 text-stone-400">
              基于你上一轮的训练表现和周期总结，为你生成下一轮训练计划草稿。已按上一轮计划预填，可直接修改。
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/10"
          >
            关闭
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-6 py-5">
          {isLoading ? (
            <div className="flex items-center justify-center py-20">
              <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
                正在加载生成条件...
              </div>
            </div>
          ) : pageError ? (
            <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
              {pageError}
            </div>
          ) : !capability ? (
            <div className="rounded-2xl border border-amber-300/20 bg-amber-300/10 px-4 py-3 text-sm text-amber-100">
              当前无法发起生成，请返回后重试。
            </div>
          ) : (
            <TemplateGenerationForm
              capability={capability}
              initialValues={initialValues}
              isSubmitting={isSubmitting}
              submitError={submitError}
              onSubmit={handleSubmit}
            />
          )}
        </div>
      </section>
    </div>
  );
}
