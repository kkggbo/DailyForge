type StatsEmptyStateProps = {
  title: string;
  description?: string;
};

export function StatsEmptyState({ title, description }: StatsEmptyStateProps) {
  return (
    <div className="rounded-3xl border border-dashed border-white/10 bg-black/20 px-5 py-10 text-center">
      <p className="text-base font-medium text-white">{title}</p>
      {description ? (
        <p className="mt-2 text-sm leading-6 text-stone-400">{description}</p>
      ) : null}
    </div>
  );
}
