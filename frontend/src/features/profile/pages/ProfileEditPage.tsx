import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { ApiRequestError } from "../../../shared/api/http";
import {
  createBodyMetric,
  getBasicProfile,
  getCurrentBodyMetricSnapshot,
  updateBasicProfile
} from "../api/profile";
import { BasicProfileForm } from "../components/BasicProfileForm";
import { BodyMetricForm } from "../components/BodyMetricForm";
import type {
  BodyMetricSnapshotResponse,
  CreateBodyMetricPayload,
  ProfileBasicResponse,
  UpdateProfileBasicPayload
} from "../types/profile";

export function ProfileEditPage() {
  const { accessToken } = useAuth();
  const [basicProfile, setBasicProfile] = useState<ProfileBasicResponse | null>(null);
  const [snapshot, setSnapshot] = useState<BodyMetricSnapshotResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [isSavingBasic, setIsSavingBasic] = useState(false);
  const [isSubmittingMetric, setIsSubmittingMetric] = useState(false);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    void load(accessToken);
  }, [accessToken]);

  async function load(token: string) {
    setIsLoading(true);
    setPageError(null);

    try {
      const [nextBasic, nextSnapshot] = await Promise.all([
        getBasicProfile(token),
        getCurrentBodyMetricSnapshot(token)
      ]);
      setBasicProfile(nextBasic);
      setSnapshot(nextSnapshot);
    } catch (error) {
      setPageError(getErrorMessage(error, "加载个人资料失败，请稍后重试"));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleSaveBasicProfile(payload: UpdateProfileBasicPayload) {
    const token = requireAccessToken(accessToken);
    setIsSavingBasic(true);

    try {
      await updateBasicProfile(token, payload);
      const nextBasic = await getBasicProfile(token);
      setBasicProfile(nextBasic);
    } finally {
      setIsSavingBasic(false);
    }
  }

  async function handleCreateBodyMetric(payload: CreateBodyMetricPayload) {
    const token = requireAccessToken(accessToken);
    setIsSubmittingMetric(true);

    try {
      await createBodyMetric(token, payload);
      const nextSnapshot = await getCurrentBodyMetricSnapshot(token);
      setSnapshot(nextSnapshot);
    } finally {
      setIsSubmittingMetric(false);
    }
  }

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
          正在加载个人资料...
        </div>
      </div>
    );
  }

  return (
    <section className="space-y-8">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.28em] text-amber-300">Profile</p>
          <h1 className="mt-3 text-4xl font-semibold text-white sm:text-5xl">
            更新个人信息
          </h1>
          <p className="mt-4 max-w-3xl leading-8 text-stone-300">
            在这里维护基础档案，并新增一条身体指标记录。保存后会作为最新快照展示在个人资料页。
          </p>
        </div>
        <Link
          to="/profile"
          className="inline-flex rounded-full border border-white/10 bg-white/8 px-5 py-3 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
        >
          返回个人资料
        </Link>
      </header>

      {pageError ? (
        <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
          {pageError}
        </div>
      ) : null}

      <BasicProfileForm
        initialValue={basicProfile}
        submitLabel="保存基础档案"
        submitSuccessMessage="基础档案已更新"
        isSubmitting={isSavingBasic}
        onSubmit={handleSaveBasicProfile}
      />

      <BodyMetricForm
        title="新增身体指标记录"
        description="默认已为你带入最新一次的身体指标，你可以直接微调变化的字段；如果想从头填写，可以先点清空。"
        initialValue={snapshot}
        submitLabel="新增身体指标记录"
        submitSuccessMessage="身体指标记录已新增"
        isSubmitting={isSubmittingMetric}
        showClearAction
        onSubmit={handleCreateBodyMetric}
      />
    </section>
  );
}

function requireAccessToken(accessToken: string | null) {
  if (!accessToken) {
    throw new Error("当前未登录，请重新登录后再试");
  }

  return accessToken;
}

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiRequestError && error.message) {
    return error.message;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}
