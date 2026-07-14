import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import {
  createBodyMetric,
  getBasicProfile,
  getProfileCompletionSummary,
  updateBasicProfile
} from "../api/profile";
import { BasicProfileForm } from "../components/BasicProfileForm";
import { BodyMetricForm } from "../components/BodyMetricForm";
import { aiSceneMetaMap } from "../lib/profile-enums";
import {
  getMissingFieldsForScene,
  getSceneReady,
  mapMissingFieldsToLabels,
  normalizeRedirectPath,
  shouldStartFromMetricStep
} from "../lib/profile-mappers";
import type {
  AiCompletionScene,
  CreateBodyMetricPayload,
  ProfileBasicResponse,
  ProfileCompletionSummaryResponse,
  UpdateProfileBasicPayload
} from "../types/profile";

export function ProfileAiCompletionPage() {
  const navigate = useNavigate();
  const { accessToken } = useAuth();
  const [searchParams] = useSearchParams();
  const [step, setStep] = useState<1 | 2>(1);
  const [basicProfile, setBasicProfile] = useState<ProfileBasicResponse | null>(null);
  const [summary, setSummary] = useState<ProfileCompletionSummaryResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [isSavingBasic, setIsSavingBasic] = useState(false);
  const [isSubmittingMetric, setIsSubmittingMetric] = useState(false);

  const scene = getScene(searchParams.get("scene"));
  const redirectPath = normalizeRedirectPath(searchParams.get("redirect"));
  const sceneMeta = aiSceneMetaMap[scene];
  const missingFieldLabels = useMemo(
    () =>
      summary ? mapMissingFieldsToLabels(getMissingFieldsForScene(summary, scene)) : [],
    [scene, summary]
  );
  const isSceneReady = summary ? getSceneReady(summary, scene) : false;

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    void loadPageData(accessToken);
  }, [accessToken]);

  async function loadPageData(token: string) {
    setIsLoading(true);
    setPageError(null);

    try {
      const [nextBasic, nextSummary] = await Promise.all([
        getBasicProfile(token),
        getProfileCompletionSummary(token)
      ]);

      setBasicProfile(nextBasic);
      setSummary(nextSummary);

      if (shouldStartFromMetricStep(getMissingFieldsForScene(nextSummary, scene))) {
        setStep(2);
      } else {
        setStep(1);
      }
    } catch (error) {
      setPageError(error instanceof Error ? error.message : "加载 AI 补录页面失败");
    } finally {
      setIsLoading(false);
    }
  }

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
      await refreshBasicAndSummary();
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
      navigate(redirectPath, { replace: true });
    } finally {
      setIsSubmittingMetric(false);
    }
  }

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
          正在准备资料补录页面...
        </div>
      </div>
    );
  }

  return (
    <section className="mx-auto max-w-6xl space-y-8">
      <div className="rounded-[36px] border border-white/10 bg-white/6 p-8 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
          AI Completion
        </p>
        <h1 className="mt-4 text-4xl font-semibold text-white sm:text-5xl">
          {sceneMeta.title}
        </h1>
        <p className="mt-4 max-w-3xl leading-8 text-stone-300">
          {sceneMeta.description}
        </p>

        <div className="mt-6 flex flex-wrap gap-3">
          <StepChip active={step === 1} label="第 1 步：基础档案" />
          <StepChip active={step === 2} label="第 2 步：身体指标" />
          <StatusChip ready={isSceneReady} />
        </div>

        <div className="mt-6 rounded-3xl border border-white/10 bg-black/20 p-5">
          <p className="text-sm text-stone-400">当前场景仍缺少的关键信息</p>
          {missingFieldLabels.length > 0 ? (
            <div className="mt-3 flex flex-wrap gap-2">
              {missingFieldLabels.map((field) => (
                <span
                  key={field}
                  className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-stone-200"
                >
                  {field}
                </span>
              ))}
            </div>
          ) : (
            <p className="mt-3 text-sm text-emerald-200">
              当前场景所需的关键资料已经齐备，你也可以继续补充更多数据。
            </p>
          )}
        </div>
      </div>

      {pageError ? (
        <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
          {pageError}
        </div>
      ) : null}

      {step === 1 ? (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => navigate(redirectPath, { replace: true })}
              className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/8"
            >
              稍后补充
            </button>
            <button
              type="button"
              onClick={() => setStep(2)}
              className="rounded-full border border-amber-300/20 px-4 py-2 text-sm text-amber-200 transition hover:bg-amber-400/10"
            >
              下一步，先看身体指标
            </button>
          </div>

          <BasicProfileForm
            initialValue={basicProfile}
            submitLabel="保存并继续"
            submitSuccessMessage="基础档案已更新，继续补充身体指标"
            isSubmitting={isSavingBasic}
            onSubmit={handleSaveBasicProfile}
          />
        </div>
      ) : (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => setStep(1)}
              className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/8"
            >
              上一步
            </button>
            <button
              type="button"
              onClick={() => navigate(redirectPath, { replace: true })}
              className="rounded-full border border-amber-300/20 px-4 py-2 text-sm text-amber-200 transition hover:bg-amber-400/10"
            >
              稍后补充
            </button>
          </div>

          <BodyMetricForm
            submitLabel="保存并返回"
            submitSuccessMessage="身体指标已记录，正在返回"
            isSubmitting={isSubmittingMetric}
            onSubmit={handleCreateBodyMetric}
          />
        </div>
      )}
    </section>
  );
}

function getScene(rawScene: string | null): AiCompletionScene {
  if (
    rawScene === "ai-plan" ||
    rawScene === "ai-nutrition" ||
    rawScene === "ai-summary"
  ) {
    return rawScene;
  }

  return "ai-plan";
}

type StepChipProps = {
  active: boolean;
  label: string;
};

function StepChip({ active, label }: StepChipProps) {
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

type StatusChipProps = {
  ready: boolean;
};

function StatusChip({ ready }: StatusChipProps) {
  return (
    <span
      className={[
        "rounded-full px-4 py-2 text-sm",
        ready
          ? "bg-emerald-400/15 text-emerald-200"
          : "border border-white/10 bg-black/20 text-stone-300"
      ].join(" ")}
    >
      {ready ? "当前资料已满足该场景" : "当前资料仍需补充"}
    </span>
  );
}
