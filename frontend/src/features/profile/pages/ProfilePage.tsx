import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { ApiRequestError } from "../../../shared/api/http";
import { getBasicProfile, getCurrentBodyMetricSnapshot } from "../api/profile";
import { BasicProfileSummaryCard } from "../components/BasicProfileSummaryCard";
import { BodyMetricSummaryCard } from "../components/BodyMetricSummaryCard";
import type {
  BodyMetricSnapshotResponse,
  ProfileBasicResponse
} from "../types/profile";

export function ProfilePage() {
  const { accessToken } = useAuth();
  const [basicProfile, setBasicProfile] = useState<ProfileBasicResponse | null>(null);
  const [snapshot, setSnapshot] = useState<BodyMetricSnapshotResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);

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
          <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
            Profile
          </p>
          <h1 className="mt-3 text-4xl font-semibold text-white sm:text-5xl">
            个人资料
          </h1>
          <p className="mt-4 max-w-3xl leading-8 text-stone-300">
            这里汇总你的基础档案和最新身体状态，用于生成更贴合的训练与饮食建议。
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <Link
            to="/profile/edit"
            className="inline-flex rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300"
          >
            更新个人信息
          </Link>
          <Link
            to="/profile/metrics/history"
            className="inline-flex rounded-full border border-white/10 bg-white/8 px-5 py-3 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
          >
            查看身体指标历史记录
          </Link>
          <Link
            to="/account"
            className="inline-flex rounded-full border border-white/10 bg-white/8 px-5 py-3 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
          >
            账号设置
          </Link>
        </div>
      </header>

      {pageError ? (
        <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
          {pageError}
        </div>
      ) : null}

      <BasicProfileSummaryCard basicProfile={basicProfile} />
      <BodyMetricSummaryCard snapshot={snapshot} />
    </section>
  );
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
