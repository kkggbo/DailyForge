import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import {
  createBodyMetric,
  getBasicProfile,
  updateBasicProfile
} from "../api/profile";
import { BasicProfileForm } from "../components/BasicProfileForm";
import { BodyMetricForm } from "../components/BodyMetricForm";
import { markProfileOnboardingCompleted } from "../lib/onboarding-storage";
import type {
  CreateBodyMetricPayload,
  ProfileBasicResponse,
  UpdateProfileBasicPayload
} from "../types/profile";

export function ProfileOnboardingPage() {
  const navigate = useNavigate();
  const { accessToken, currentUser } = useAuth();
  const [step, setStep] = useState<1 | 2>(1);
  const [basicProfile, setBasicProfile] = useState<ProfileBasicResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [isSavingBasic, setIsSavingBasic] = useState(false);
  const [isSubmittingMetric, setIsSubmittingMetric] = useState(false);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    void loadBasicProfile(accessToken);
  }, [accessToken]);

  async function loadBasicProfile(token: string) {
    setIsLoading(true);
    setPageError(null);

    try {
      const nextBasic = await getBasicProfile(token);
      setBasicProfile(nextBasic);
    } catch (error) {
      setPageError(error instanceof Error ? error.message : "加载引导资料失败");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSaveBasicProfile(payload: UpdateProfileBasicPayload) {
    if (!accessToken) {
      throw new Error("当前未登录，请重新登录后再试");
    }

    setIsSavingBasic(true);

    try {
      await updateBasicProfile(accessToken, payload);
      const nextBasic = await getBasicProfile(accessToken);
      setBasicProfile(nextBasic);
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
      completeOnboarding();
    } finally {
      setIsSubmittingMetric(false);
    }
  }

  function completeOnboarding() {
    if (currentUser) {
      markProfileOnboardingCompleted(currentUser.userId);
    }
    navigate("/app", { replace: true });
  }

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
          正在准备首次引导...
        </div>
      </div>
    );
  }

  return (
    <section className="mx-auto max-w-6xl space-y-8">
      <div className="rounded-[36px] border border-white/10 bg-white/6 p-8 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
          Onboarding
        </p>
        <h1 className="mt-4 text-4xl font-semibold text-white sm:text-5xl">
          先把你的基础资料补齐一点
        </h1>
        <p className="mt-4 max-w-3xl leading-8 text-stone-300">
          这是注册后的轻量引导，不是使用门槛。你可以现在就补，也可以先跳过，后续再回到个人资料页继续完善。
        </p>

        <div className="mt-6 flex flex-wrap gap-3">
          <StepChip active={step === 1} label="第 1 步：基础档案" />
          <StepChip active={step === 2} label="第 2 步：身体指标" />
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
              onClick={completeOnboarding}
              className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/8"
            >
              跳过并进入应用
            </button>
            <button
              type="button"
              onClick={() => setStep(2)}
              className="rounded-full border border-amber-300/20 px-4 py-2 text-sm text-amber-200 transition hover:bg-amber-400/10"
            >
              下一步，暂不保存
            </button>
          </div>

          <BasicProfileForm
            initialValue={basicProfile}
            submitLabel="保存并继续"
            submitSuccessMessage="基础档案已保存，继续填写身体指标"
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
              onClick={completeOnboarding}
              className="rounded-full border border-amber-300/20 px-4 py-2 text-sm text-amber-200 transition hover:bg-amber-400/10"
            >
              跳过并进入应用
            </button>
          </div>

          <BodyMetricForm
            submitLabel="保存并完成"
            submitSuccessMessage="身体指标已记录，正在进入应用"
            isSubmitting={isSubmittingMetric}
            onSubmit={handleCreateBodyMetric}
          />
        </div>
      )}
    </section>
  );
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
