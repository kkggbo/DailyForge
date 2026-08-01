import { Link } from "react-router-dom";

type AiCoachUnavailableStateProps = {
  title: string;
  description: string;
  actionLabel?: string;
  actionTo?: string;
};

export function AiCoachUnavailableState({
  title,
  description,
  actionLabel,
  actionTo
}: AiCoachUnavailableStateProps) {
  return (
    <section className="rounded-[32px] border border-amber-300/20 bg-amber-300/10 p-8">
      <p className="text-sm uppercase tracking-[0.28em] text-amber-200">
        AI Coach
      </p>
      <h2 className="mt-3 text-3xl font-semibold text-white">{title}</h2>
      <p className="mt-3 max-w-2xl leading-7 text-stone-200">{description}</p>
      {actionLabel && actionTo ? (
        <Link
          to={actionTo}
          className="mt-6 inline-flex rounded-full border border-white/10 bg-white/8 px-5 py-3 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
        >
          {actionLabel}
        </Link>
      ) : null}
    </section>
  );
}
