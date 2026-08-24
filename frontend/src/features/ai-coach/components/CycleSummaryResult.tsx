import type { ReactNode } from "react";
import type { CycleSummaryTaskResult } from "../types/ai-coach";

type CycleSummaryResultProps = {
  result: CycleSummaryTaskResult;
};

export function CycleSummaryResult({ result }: CycleSummaryResultProps) {
  return (
    <div className="space-y-6">
      <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
          Cycle Summary
        </p>
        <h2 className="mt-3 text-3xl font-semibold text-white">
          {result.templateName} · 第 {result.runNo} 轮
        </h2>
        <p className="mt-3 max-w-3xl leading-7 text-stone-300">
          {result.executionOverview}
        </p>
        <div className="mt-5 flex flex-wrap gap-2 text-xs text-stone-200">
          <Tag>{`第 ${result.runNo} 轮 · ${result.cycleLength} 天周期`}</Tag>
        </div>
      </section>

      <SummarySection title="做得好的地方" items={result.strengths} />
      <SummarySection title="主要问题" items={result.issues} />
      <SummarySection title="原因分析" items={result.causeAnalysis} />
      <SummarySection title="下一轮建议" items={result.nextCycleSuggestions} />
      <SummarySection title="风险提醒" items={result.risks} />

      {result.dataCompletenessNotice ? (
        <section className="rounded-[28px] border border-amber-300/20 bg-amber-300/10 p-5">
          <p className="text-sm font-semibold text-white">资料完整度提醒</p>
          <p className="mt-3 text-sm leading-6 text-stone-200">
            {result.dataCompletenessNotice}
          </p>
        </section>
      ) : null}
    </div>
  );
}

function SummarySection({ title, items }: { title: string; items: string[] }) {
  return (
    <section className="rounded-[28px] border border-white/10 bg-black/20 p-5">
      <h3 className="text-xl font-semibold text-white">{title}</h3>
      {items.length > 0 ? (
        <ul className="mt-4 space-y-3 text-sm leading-6 text-stone-200">
          {items.map((item) => (
            <li key={item} className="rounded-2xl border border-white/10 bg-white/6 p-4">
              {item}
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-4 text-sm text-stone-400">暂无内容。</p>
      )}
    </section>
  );
}

function Tag({ children }: { children: ReactNode }) {
  return (
    <span className="rounded-full border border-white/10 bg-white/8 px-3 py-1">
      {children}
    </span>
  );
}
