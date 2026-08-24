import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import {
  formatAiDateTime,
  formatAiTaskProgressStage,
  formatAiTaskStatus
} from "../lib/ai-coach-formatters";
import type {
  AiTaskHistoryPage,
  AiTaskProgressStage,
  AiTaskStatus
} from "../types/ai-coach";

type HistoryRecordBase = {
  taskId: number;
  taskStatus: AiTaskStatus;
  progressStage: AiTaskProgressStage;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
  summaryText: string | null;
};

type AiTaskHistoryListProps<TRecord extends HistoryRecordBase> = {
  title: string;
  description: string;
  emptyMessage: string;
  taskLinkLabel: string;
  primaryAction?: boolean;
  isLoading: boolean;
  error: string | null;
  data: AiTaskHistoryPage<TRecord> | null;
  getTaskLink: (record: TRecord) => string;
  renderHeading: (record: TRecord) => ReactNode;
  renderMeta: (record: TRecord) => ReactNode;
  onPageChange: (page: number) => void;
};

export function AiTaskHistoryList<TRecord extends HistoryRecordBase>({
  title,
  description,
  emptyMessage,
  taskLinkLabel,
  primaryAction = false,
  isLoading,
  error,
  data,
  getTaskLink,
  renderHeading,
  renderMeta,
  onPageChange
}: AiTaskHistoryListProps<TRecord>) {
  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.pageSize)) : 1;

  return (
    <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-white">{title}</h2>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-stone-300">
            {description}
          </p>
        </div>
        <p className="text-sm text-stone-400">共 {data?.total ?? 0} 条</p>
      </div>

      {error ? (
        <div className="mt-5 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {error}
        </div>
      ) : null}

      {isLoading ? (
        <div className="mt-6 rounded-3xl border border-white/10 bg-black/20 px-5 py-4 text-sm text-stone-300">
          正在加载历史记录...
        </div>
      ) : data && data.records.length > 0 ? (
        <>
          <div className="mt-6 space-y-4">
            {data.records.map((record) => (
              <article
                key={record.taskId}
                className="rounded-3xl border border-white/10 bg-black/20 p-5"
              >
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div className="min-w-0">
                    <h3 className="text-lg font-semibold text-white">
                      {renderHeading(record)}
                    </h3>
                    <div className="mt-3 flex flex-wrap gap-2 text-xs text-stone-200">
                      {renderMeta(record)}
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    <StatusPill>
                      {formatHistoryStatus(record.taskStatus, record.progressStage)}
                    </StatusPill>
                  </div>
                </div>

                <div className="mt-4 grid gap-3 sm:grid-cols-3">
                  <InfoItem label="创建时间" value={formatAiDateTime(record.createdAt)} />
                  <InfoItem label="最近更新" value={formatAiDateTime(record.updatedAt)} />
                  <InfoItem label="完成时间" value={formatAiDateTime(record.completedAt)} />
                </div>

                <div className="mt-4 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-stone-300">
                  {record.summaryText ?? "暂无可预览的摘要内容。"}
                </div>

                <div className="mt-4 flex justify-end">
                  <Link
                    to={getTaskLink(record)}
                    className={
                      primaryAction
                        ? "rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 transition hover:bg-amber-300"
                        : "rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
                    }
                  >
                    {taskLinkLabel}
                  </Link>
                </div>
              </article>
            ))}
          </div>

          <div className="mt-6 flex items-center justify-between gap-3">
            <button
              type="button"
              disabled={!data || data.page <= 1}
              onClick={() => onPageChange((data?.page ?? 1) - 1)}
              className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/8 disabled:cursor-not-allowed disabled:opacity-50"
            >
              上一页
            </button>
            <p className="text-sm text-stone-400">
              第 {data?.page ?? 1} / {totalPages} 页
            </p>
            <button
              type="button"
              disabled={!data || data.page >= totalPages}
              onClick={() => onPageChange((data?.page ?? 1) + 1)}
              className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/8 disabled:cursor-not-allowed disabled:opacity-50"
            >
              下一页
            </button>
          </div>
        </>
      ) : (
        <div className="mt-6 rounded-3xl border border-dashed border-white/10 bg-black/20 px-5 py-8 text-center text-sm text-stone-400">
          {emptyMessage}
        </div>
      )}
    </section>
  );
}

function StatusPill({ children }: { children: string }) {
  return (
    <span className="rounded-full bg-white/8 px-3 py-1 text-xs text-stone-200">
      {children}
    </span>
  );
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
      <p className="text-xs uppercase tracking-[0.18em] text-stone-500">{label}</p>
      <p className="mt-2 text-sm font-medium text-white">{value}</p>
    </div>
  );
}

function formatHistoryStatus(
  taskStatus: AiTaskStatus,
  progressStage: AiTaskProgressStage
) {
  if (taskStatus === "succeeded" || taskStatus === "failed") {
    return formatAiTaskStatus(taskStatus);
  }

  return formatAiTaskProgressStage(progressStage);
}
