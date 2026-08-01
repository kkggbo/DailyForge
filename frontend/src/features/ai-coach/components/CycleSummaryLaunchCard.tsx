import { formatAiDateTime } from "../lib/ai-coach-formatters";
import type { CycleSummaryCapability } from "../types/ai-coach";

type CycleSummaryLaunchCardProps = {
  capability: CycleSummaryCapability;
  isSubmitting: boolean;
  submitError: string | null;
  onSubmit: () => void;
};

export function CycleSummaryLaunchCard({
  capability,
  isSubmitting,
  submitError,
  onSubmit
}: CycleSummaryLaunchCardProps) {
  return (
    <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
      <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
        Cycle Summary
      </p>
      <h2 className="mt-3 text-3xl font-semibold text-white">
        分析最近一轮已完成循环
      </h2>
      <p className="mt-3 max-w-2xl leading-7 text-stone-300">
        当前版本会基于最近一轮已完成循环发起总结任务，结果只返回结构化建议，不会自动修改模板。
      </p>

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <InfoCard
          label="循环 ID"
          value={String(capability.latestCompletedCycleRunId ?? "--")}
        />
        <InfoCard
          label="最近完成时间"
          value={formatAiDateTime(capability.latestCompletedAt)}
        />
        <InfoCard
          label="建议补充资料"
          value={
            capability.recommendedMissingFields.length > 0
              ? `${capability.recommendedMissingFields.length} 项`
              : "已较完整"
          }
        />
      </div>

      {submitError ? (
        <div className="mt-5 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
          {submitError}
        </div>
      ) : null}

      <div className="mt-6 flex justify-end">
        <button
          type="button"
          disabled={isSubmitting || !capability.latestCompletedCycleRunId}
          onClick={onSubmit}
          className="rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
        >
          {isSubmitting ? "提交中..." : "发起周期总结任务"}
        </button>
      </div>
    </section>
  );
}

function InfoCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-3xl border border-white/10 bg-black/20 p-4">
      <p className="text-xs uppercase tracking-[0.18em] text-stone-500">{label}</p>
      <p className="mt-3 text-lg font-semibold text-white">{value}</p>
    </div>
  );
}
