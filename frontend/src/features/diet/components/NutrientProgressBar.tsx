type NutrientProgressBarProps = {
  label: string;
  current: number;
  target: number | null;
  pct: number | null;
  colorClass?: string;
};

/**
 * 单个营养素「已摄入 / 目标」+ 进度条。
 * target 为 null 时只显示当前值，不渲染进度条轨道。
 */
export function NutrientProgressBar({
  label,
  current,
  target,
  pct,
  colorClass = "bg-amber-400"
}: NutrientProgressBarProps) {
  const showBar = target !== null && pct !== null;
  const width =
    pct === null ? 0 : Math.min(Math.max(pct, 0), 100);

  return (
    <div>
      <div className="flex items-baseline justify-between gap-3">
        <span className="text-sm text-stone-300">{label}</span>
        <span className="text-sm text-stone-400">
          <span className="font-semibold text-white">{Math.round(current)}</span>
          {target !== null ? ` / ${Math.round(target)}` : ""}
        </span>
      </div>
      {showBar ? (
        <>
          <div className="mt-2 h-2.5 w-full overflow-hidden rounded-full bg-white/8">
            <div
              className={`h-full rounded-full ${colorClass}`}
              style={{ width: `${width}%` }}
            />
          </div>
          {pct !== null ? (
            <p className="mt-1 text-xs text-stone-500">{Math.round(pct)}%</p>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
