import type { ReactNode } from "react";
import { Link } from "react-router-dom";

type AiCoachCapabilityCardProps = {
  title: string;
  description: string;
  available: boolean;
  ready: boolean;
  ctaLabel: string;
  to: string;
  meta?: ReactNode;
};

export function AiCoachCapabilityCard({
  title,
  description,
  available,
  ready,
  ctaLabel,
  to,
  meta
}: AiCoachCapabilityCardProps) {
  return (
    <article className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-white">{title}</h2>
          <p className="mt-3 max-w-2xl leading-7 text-stone-300">{description}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <StatusPill active={available}>
            {available ? "已开通" : "未开通"}
          </StatusPill>
          <StatusPill active={ready}>
            {ready ? "可立即使用" : "待补资料"}
          </StatusPill>
        </div>
      </div>

      {meta ? <div className="mt-5">{meta}</div> : null}

      <Link
        to={to}
        className="mt-6 inline-flex rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
      >
        {ctaLabel}
      </Link>
    </article>
  );
}

function StatusPill({
  active,
  children
}: {
  active: boolean;
  children: ReactNode;
}) {
  return (
    <span
      className={[
        "rounded-full px-3 py-1 text-xs",
        active
          ? "bg-emerald-400/15 text-emerald-200"
          : "border border-white/10 bg-black/20 text-stone-300"
      ].join(" ")}
    >
      {children}
    </span>
  );
}
