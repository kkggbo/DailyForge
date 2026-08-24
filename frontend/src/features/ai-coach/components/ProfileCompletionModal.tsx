import { useEffect, useState } from "react";
import { useAuth } from "../../../app/providers/AuthProvider";
import {
  createBodyMetric,
  getBasicProfile,
  getProfileCompletionSummary,
  updateBasicProfile
} from "../../profile/api/profile";
import { BasicProfileForm } from "../../profile/components/BasicProfileForm";
import { BodyMetricForm } from "../../profile/components/BodyMetricForm";
import { aiSceneMetaMap } from "../../profile/lib/profile-enums";
import {
  getMissingFieldsForScene,
  getSceneReady,
  mapMissingFieldsToLabels,
  shouldStartFromMetricStep
} from "../../profile/lib/profile-mappers";
import type {
  AiCompletionScene,
  CreateBodyMetricPayload,
  ProfileBasicResponse,
  ProfileCompletionSummaryResponse,
  UpdateProfileBasicPayload
} from "../../profile/types/profile";

type ProfileCompletionModalProps = {
  open: boolean;
  scene: AiCompletionScene;
  onClose: () => void;
  onCompleted: () => Promise<void>;
};

export function ProfileCompletionModal({
  open,
  scene,
  onClose,
  onCompleted
}: ProfileCompletionModalProps) {
  const { accessToken } = useAuth();
  const [step, setStep] = useState<1 | 2>(1);
  const [basicProfile, setBasicProfile] = useState<ProfileBasicResponse | null>(null);
  const [summary, setSummary] = useState<ProfileCompletionSummaryResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [isSavingBasic, setIsSavingBasic] = useState(false);
  const [isSubmittingMetric, setIsSubmittingMetric] = useState(false);

  const sceneMeta = aiSceneMetaMap[scene];
  const missingFieldLabels = summary
    ? mapMissingFieldsToLabels(getMissingFieldsForScene(summary, scene))
    : [];

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
        const [nextBasic, nextSummary] = await Promise.all([
          getBasicProfile(token),
          getProfileCompletionSummary(token)
        ]);

        if (cancelled) {
          return;
        }

        setBasicProfile(nextBasic);
        setSummary(nextSummary);
        setStep(
          shouldStartFromMetricStep(getMissingFieldsForScene(nextSummary, scene))
            ? 2
            : 1
        );
      } catch (error) {
        if (!cancelled) {
          setPageError(error instanceof Error ? error.message : "加载资料补录信息失败");
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
  }, [open, accessToken, scene]);

  async function refreshBasicAndSummary() {
    if (!accessToken) {
      throw new Error("当前未登录，请重新登录后再试");
    }

    const [nextBasic, nextSummary] = await Promise.all([
      getBasicProfile(accessToken),
      getProfileCompletionSummary(accessToken)
    ]);

    setBasicProfile(nextBasic);
    setSummary(nextSummary);
    return nextSummary;
  }

  async function handleSaveBasicProfile(payload: UpdateProfileBasicPayload) {
    if (!accessToken) {
      throw new Error("当前未登录，请重新登录后再试");
    }

    setIsSavingBasic(true);

    try {
      await updateBasicProfile(accessToken, payload);
      const nextSummary = await refreshBasicAndSummary();

      if (getSceneReady(nextSummary, scene)) {
        onClose();
        void onCompleted();
        return;
      }

      setStep(2);
    } finally {
      setIsSavingBasic(false);
    }
  }

  async function handleCreateBodyMetric(payload: CreateBodyMetricPayload) {
    if (!accessToken) {
      throw new Error("当前未登录，请重新登录后再试");
    }

    setIsSubmittingMetric(true);

    try {
      await createBodyMetric(accessToken, payload);
      await refreshBasicAndSummary();
      onClose();
      void onCompleted();
    } finally {
      setIsSubmittingMetric(false);
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
              AI Completion
            </p>
            <h2 className="mt-2 text-2xl font-semibold text-white">{sceneMeta.title}</h2>
            <p className="mt-2 text-sm leading-6 text-stone-400">
              {sceneMeta.description}
            </p>
            <div className="mt-4 flex flex-wrap items-center gap-2">
              <StepChip active={step === 1} label="第 1 步：基础档案" />
              <StepChip active={step === 2} label="第 2 步：身体指标" />
            </div>
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
                正在准备资料补录...
              </div>
            </div>
          ) : pageError ? (
            <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
              {pageError}
            </div>
          ) : (
            <>
              {missingFieldLabels.length > 0 ? (
                <div className="mb-4 rounded-3xl border border-white/10 bg-white/5 p-4">
                  <p className="text-sm text-stone-400">当前场景仍缺少的关键信息</p>
                  <div className="mt-2 flex flex-wrap gap-2">
                    {missingFieldLabels.map((field) => (
                      <span
                        key={field}
                        className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-stone-200"
                      >
                        {field}
                      </span>
                    ))}
                  </div>
                </div>
              ) : null}

              {step === 1 ? (
                <BasicProfileForm
                  initialValue={basicProfile}
                  submitLabel="保存并继续"
                  submitSuccessMessage="基础档案已更新"
                  isSubmitting={isSavingBasic}
                  secondaryAction={
                    <button
                      type="button"
                      onClick={onClose}
                      className="rounded-2xl border border-white/10 px-5 py-3 text-sm text-stone-200 transition hover:bg-white/8"
                    >
                      稍后补充
                    </button>
                  }
                  onSubmit={handleSaveBasicProfile}
                />
              ) : (
                <BodyMetricForm
                  submitLabel="保存并完成"
                  submitSuccessMessage="身体指标已记录"
                  isSubmitting={isSubmittingMetric}
                  onSubmit={handleCreateBodyMetric}
                />
              )}
            </>
          )}
        </div>
      </section>
    </div>
  );
}

function StepChip({ active, label }: { active: boolean; label: string }) {
  return (
    <span
      className={[
        "rounded-full px-4 py-2 text-sm",
        active
          ? "bg-amber-400 text-stone-950"
          : "border border-white/10 bg-black/20 text-stone-300"
      ].join(" ")}
    >
      {label}
    </span>
  );
}
