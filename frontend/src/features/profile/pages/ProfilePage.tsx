import { useEffect, useState } from "react";
import { useAuth } from "../../../app/providers/AuthProvider";
import {
  createBodyMetric,
  deleteLatestBodyMetric,
  getBasicProfile,
  getBodyMetricsPage,
  getCurrentBodyMetricSnapshot,
  getProfileCompletionSummary,
  updateBasicProfile
} from "../api/profile";
import { BasicProfileForm } from "../components/BasicProfileForm";
import { BodyMetricForm } from "../components/BodyMetricForm";
import { BodyMetricHistoryList } from "../components/BodyMetricHistoryList";
import { BodyMetricSummaryCard } from "../components/BodyMetricSummaryCard";
import { CompletionSummaryBanner } from "../components/CompletionSummaryBanner";
import { DeleteLatestMetricDialog } from "../components/DeleteLatestMetricDialog";
import { ProfileTabNav } from "../components/ProfileTabNav";
import type {
  BodyMetricPageQuery,
  BodyMetricSnapshotResponse,
  BodyMetricsPageResponse,
  CreateBodyMetricPayload,
  ProfileBasicResponse,
  ProfileCompletionSummaryResponse,
  ProfileTab,
  UpdateProfileBasicPayload
} from "../types/profile";
import { ApiRequestError } from "../../../shared/api/http";

export function ProfilePage() {
  const { accessToken } = useAuth();
  const [activeTab, setActiveTab] = useState<ProfileTab>("basic");
  const [basicProfile, setBasicProfile] = useState<ProfileBasicResponse | null>(null);
  const [completionSummary, setCompletionSummary] =
    useState<ProfileCompletionSummaryResponse | null>(null);
  const [snapshot, setSnapshot] = useState<BodyMetricSnapshotResponse | null>(null);
  const [history, setHistory] = useState<BodyMetricsPageResponse | null>(null);
  const [page, setPage] = useState(1);
  const [isLoadingPage, setIsLoadingPage] = useState(true);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [pageError, setPageError] = useState<string | null>(null);
  const [isSavingBasic, setIsSavingBasic] = useState(false);
  const [isSubmittingMetric, setIsSubmittingMetric] = useState(false);
  const [isDeletingLatestMetric, setIsDeletingLatestMetric] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [deleteDialogError, setDeleteDialogError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    void loadPageData(accessToken, 1);
  }, [accessToken]);

  async function loadPageData(token: string, nextPage: number) {
    setIsLoadingPage(true);
    setPageError(null);

    try {
      const [nextBasic, nextSummary, nextSnapshot, nextHistory] = await Promise.all([
        getBasicProfile(token),
        getProfileCompletionSummary(token),
        getCurrentBodyMetricSnapshot(token),
        getBodyMetricsPage(token, { page: nextPage, pageSize: 20 })
      ]);

      setBasicProfile(nextBasic);
      setCompletionSummary(nextSummary);
      setSnapshot(nextSnapshot);
      setHistory(nextHistory);
      setPage(nextHistory.page);
    } catch (error) {
      setPageError(getErrorMessage(error, "加载个人资料失败，请稍后重试"));
    } finally {
      setIsLoadingPage(false);
    }
  }

  async function refreshBasicAndSummary() {
    const token = requireAccessToken(accessToken);
    const [nextBasic, nextSummary] = await Promise.all([
      getBasicProfile(token),
      getProfileCompletionSummary(token)
    ]);

    setBasicProfile(nextBasic);
    setCompletionSummary(nextSummary);
  }

  async function refreshAllWithCurrentPage(nextPage = page) {
    const token = requireAccessToken(accessToken);
    const [nextBasic, nextSummary, nextSnapshot, nextHistory] = await Promise.all([
      getBasicProfile(token),
      getProfileCompletionSummary(token),
      getCurrentBodyMetricSnapshot(token),
      getBodyMetricsPage(token, { page: nextPage, pageSize: 20 })
    ]);

    setBasicProfile(nextBasic);
    setCompletionSummary(nextSummary);
    setSnapshot(nextSnapshot);
    setHistory(nextHistory);
    setPage(nextHistory.page);
  }

  async function loadHistoryPage(query: BodyMetricPageQuery) {
    const token = requireAccessToken(accessToken);
    setIsLoadingHistory(true);

    try {
      const nextHistory = await getBodyMetricsPage(token, query);
      setHistory(nextHistory);
      setPage(nextHistory.page);
    } catch (error) {
      setPageError(getErrorMessage(error, "加载历史记录失败，请稍后重试"));
    } finally {
      setIsLoadingHistory(false);
    }
  }

  async function handleSaveBasicProfile(payload: UpdateProfileBasicPayload) {
    const token = requireAccessToken(accessToken);
    setIsSavingBasic(true);

    try {
      await updateBasicProfile(token, payload);
      await refreshBasicAndSummary();
    } finally {
      setIsSavingBasic(false);
    }
  }

  async function handleCreateBodyMetric(payload: CreateBodyMetricPayload) {
    const token = requireAccessToken(accessToken);
    setIsSubmittingMetric(true);

    try {
      await createBodyMetric(token, payload);
      await refreshAllWithCurrentPage(1);
      setPage(1);
    } finally {
      setIsSubmittingMetric(false);
    }
  }

  async function handleDeleteLatestMetric() {
    const token = requireAccessToken(accessToken);
    setIsDeletingLatestMetric(true);
    setPageError(null);
    setDeleteDialogError(null);

    try {
      await deleteLatestBodyMetric(token);
      setIsDeleteDialogOpen(false);
      setDeleteDialogError(null);
      await refreshAllWithCurrentPage(1);
      setPage(1);
    } catch (error) {
      const message = getDeleteLatestMetricErrorMessage(error);
      setDeleteDialogError(message);
      setPageError(message);
      await refreshAllWithCurrentPage(1);
      setPage(1);
    } finally {
      setIsDeletingLatestMetric(false);
    }
  }

  if (isLoadingPage) {
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
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
            Profile
          </p>
          <h1 className="mt-3 text-4xl font-semibold text-white sm:text-5xl">
            个人资料
          </h1>
          <p className="mt-4 max-w-3xl leading-8 text-stone-300">
            这里承接你的基础档案、身体指标记录和 AI 建议前置资料。普通功能不会因为资料不完整而被拦住，但完善后会明显提升建议质量。
          </p>
        </div>
        <ProfileTabNav activeTab={activeTab} onChange={setActiveTab} />
      </div>

      {pageError ? (
        <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
          {pageError}
        </div>
      ) : null}

      {completionSummary ? <CompletionSummaryBanner summary={completionSummary} /> : null}

      {activeTab === "basic" ? (
        <BasicProfileForm
          initialValue={basicProfile}
          submitLabel="保存基础档案"
          submitSuccessMessage="基础档案已更新"
          isSubmitting={isSavingBasic}
          onSubmit={handleSaveBasicProfile}
        />
      ) : (
        <div className="space-y-6">
          <BodyMetricSummaryCard snapshot={snapshot} />
          <BodyMetricForm
            title="录入身体指标"
            description="默认已为你带入最新一次的身体指标，你可以直接微调变化的字段；如果想从头填写，可以先点清空。"
            initialValue={snapshot}
            submitLabel="新增身体指标记录"
            submitSuccessMessage="身体指标记录已新增"
            isSubmitting={isSubmittingMetric}
            showClearAction
            onSubmit={handleCreateBodyMetric}
          />
          <BodyMetricHistoryList
            data={history}
            isLoading={isLoadingHistory}
            onPageChange={(nextPage) => {
              void loadHistoryPage({ page: nextPage, pageSize: history?.pageSize ?? 20 });
            }}
            onDeleteLatestRequest={() => {
              setDeleteDialogError(null);
              setIsDeleteDialogOpen(true);
            }}
          />
        </div>
      )}

      <DeleteLatestMetricDialog
        open={isDeleteDialogOpen}
        isSubmitting={isDeletingLatestMetric}
        errorMessage={deleteDialogError}
        onClose={() => {
          setIsDeleteDialogOpen(false);
          setDeleteDialogError(null);
        }}
        onConfirm={() => {
          void handleDeleteLatestMetric();
        }}
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

function getDeleteLatestMetricErrorMessage(error: unknown) {
  if (error instanceof ApiRequestError) {
    if (error.code === "BODY_METRIC_LATEST_ALREADY_DELETED") {
      return "最近一条身体指标记录已经被删除，请刷新列表后再试。";
    }

    if (error.code === "BODY_METRIC_NOT_FOUND") {
      return "当前没有可删除的身体指标记录。";
    }

    if (error.message) {
      return error.message;
    }
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "删除最新记录失败，请刷新后重试。";
}
