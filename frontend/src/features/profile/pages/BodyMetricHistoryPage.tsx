import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { ApiRequestError } from "../../../shared/api/http";
import { deleteLatestBodyMetric, getBodyMetricsPage } from "../api/profile";
import { BodyMetricHistoryList } from "../components/BodyMetricHistoryList";
import { DeleteLatestMetricDialog } from "../components/DeleteLatestMetricDialog";
import type { BodyMetricsPageResponse } from "../types/profile";

export function BodyMetricHistoryPage() {
  const { accessToken } = useAuth();
  const [history, setHistory] = useState<BodyMetricsPageResponse | null>(null);
  const [page, setPage] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingPage, setIsLoadingPage] = useState(false);
  const [pageError, setPageError] = useState<string | null>(null);
  const [isDeletingLatest, setIsDeletingLatest] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [deleteDialogError, setDeleteDialogError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    void loadPage(accessToken, 1);
  }, [accessToken]);

  async function loadPage(token: string, nextPage: number) {
    const isInitial = page === 1 && history === null;
    if (isInitial) {
      setIsLoading(true);
    } else {
      setIsLoadingPage(true);
    }
    setPageError(null);

    try {
      const nextHistory = await getBodyMetricsPage(token, {
        page: nextPage,
        pageSize: 20
      });
      setHistory(nextHistory);
      setPage(nextHistory.page);
    } catch (error) {
      setPageError(getErrorMessage(error, "加载历史记录失败，请稍后重试"));
    } finally {
      setIsLoading(false);
      setIsLoadingPage(false);
    }
  }

  async function handleDeleteLatest() {
    const token = requireAccessToken(accessToken);
    setIsDeletingLatest(true);
    setPageError(null);
    setDeleteDialogError(null);

    try {
      await deleteLatestBodyMetric(token);
      setIsDeleteDialogOpen(false);
      setDeleteDialogError(null);
      await loadPage(token, 1);
    } catch (error) {
      const message = getDeleteLatestMetricErrorMessage(error);
      setDeleteDialogError(message);
      setPageError(message);
      await loadPage(token, 1);
    } finally {
      setIsDeletingLatest(false);
    }
  }

  return (
    <section className="space-y-8">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.28em] text-amber-300">Profile</p>
          <h1 className="mt-3 text-4xl font-semibold text-white sm:text-5xl">
            身体指标历史记录
          </h1>
          <p className="mt-4 max-w-3xl leading-8 text-stone-300">
            按时间查看你录入过的身体指标记录。最新一条会标注出来，也可以在此删除。
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

      {isLoading ? (
        <div className="flex min-h-[32vh] items-center justify-center">
          <div className="rounded-full border border-white/15 bg-white/8 px-4 py-2 text-sm text-stone-200">
            正在加载历史记录...
          </div>
        </div>
      ) : (
        <BodyMetricHistoryList
          data={history}
          isLoading={isLoadingPage}
          onPageChange={(nextPage) => {
            void loadPage(requireAccessToken(accessToken), nextPage);
          }}
          onDeleteLatestRequest={() => {
            setDeleteDialogError(null);
            setIsDeleteDialogOpen(true);
          }}
        />
      )}

      <DeleteLatestMetricDialog
        open={isDeleteDialogOpen}
        isSubmitting={isDeletingLatest}
        errorMessage={deleteDialogError}
        onClose={() => {
          setIsDeleteDialogOpen(false);
          setDeleteDialogError(null);
        }}
        onConfirm={() => {
          void handleDeleteLatest();
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
