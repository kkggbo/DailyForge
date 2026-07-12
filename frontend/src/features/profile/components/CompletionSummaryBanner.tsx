import { getProfileFieldLabel } from "../lib/profile-enums";
import { formatNullableNumber } from "../lib/profile-formatters";
import type { ProfileCompletionSummaryResponse } from "../types/profile";

type CompletionSummaryBannerProps = {
  summary: ProfileCompletionSummaryResponse;
};

export function CompletionSummaryBanner({
  summary
}: CompletionSummaryBannerProps) {
  return (
    <section className="rounded-[32px] border border-amber-300/15 bg-black/25 p-6 backdrop-blur">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
            Profile Readiness
          </p>
          <h2 className="mt-3 text-2xl font-semibold text-white">
            当前资料完成度
          </h2>
          <p className="mt-2 max-w-3xl leading-7 text-stone-300">
            这里不会拦住你使用普通功能，但资料越完整，后续 AI 给出的训练和饮食建议会越贴合。
          </p>
        </div>

        <div className="rounded-3xl border border-white/10 bg-white/5 px-5 py-4">
          <p className="text-sm text-stone-400">当前体重</p>
          <p className="mt-2 text-3xl font-semibold text-white">
            {formatNullableNumber(summary.currentWeightKg, { unit: "kg" })}
          </p>
        </div>
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-4">
        <StatusCard
          title="基础档案"
          ready={summary.basicProfileReady}
          description="是否具备生成 AI 建议所需的核心档案"
        />
        <StatusCard
          title="体重记录"
          ready={summary.hasWeightRecord}
          description="是否已有可用体重，用于估算基础消耗与建议"
        />
        <StatusCard
          title="AI 训练计划"
          ready={summary.aiPlanReady}
          description="是否满足 AI 训练计划生成的最小输入"
        />
        <StatusCard
          title="AI 饮食建议"
          ready={summary.aiNutritionReady}
          description="是否满足 AI 饮食建议生成的最小输入"
        />
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <MissingFieldPanel
          title="基础档案仍缺少"
          fields={summary.missingBasicProfileFields}
        />
        <MissingFieldPanel
          title="当前 AI 训练计划仍缺少"
          fields={summary.aiPlanMissingFields}
        />
      </div>
    </section>
  );
}

type StatusCardProps = {
  title: string;
  description: string;
  ready: boolean;
};

function StatusCard({ title, description, ready }: StatusCardProps) {
  return (
    <article className="rounded-3xl border border-white/10 bg-white/5 p-4">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-medium text-white">{title}</p>
        <span
          className={[
            "rounded-full px-3 py-1 text-xs font-medium",
            ready
              ? "bg-emerald-400/15 text-emerald-200"
              : "bg-amber-400/15 text-amber-200"
          ].join(" ")}
        >
          {ready ? "已就绪" : "待补充"}
        </span>
      </div>
      <p className="mt-3 text-sm leading-6 text-stone-400">{description}</p>
    </article>
  );
}

type MissingFieldPanelProps = {
  title: string;
  fields: string[];
};

function MissingFieldPanel({ title, fields }: MissingFieldPanelProps) {
  return (
    <article className="rounded-3xl border border-white/10 bg-white/4 p-4">
      <p className="text-sm font-medium text-white">{title}</p>
      {fields.length > 0 ? (
        <div className="mt-3 flex flex-wrap gap-2">
          {fields.map((field) => (
            <span
              key={field}
              className="rounded-full border border-white/10 bg-black/20 px-3 py-1.5 text-xs text-stone-200"
            >
              {getProfileFieldLabel(field)}
            </span>
          ))}
        </div>
      ) : (
        <p className="mt-3 text-sm text-stone-400">当前没有缺失项。</p>
      )}
    </article>
  );
}
