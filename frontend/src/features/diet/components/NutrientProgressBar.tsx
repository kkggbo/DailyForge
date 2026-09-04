type NutrientProgressBarProps = {
  label: string;
  current: number;
  target: number | null;
  pct: number | null;
  colorClass?: string;
  /** 无目标时展示的单位（如 g / 千卡），避免名称与数值紧贴且无单位 */
  unit?: string;
};

/**
 * 单个营养素「已摄入 / 目标」+ 进度条。
 * - target 为 null（无目标）时：名称与数值分行展示（名称一行、数值一行），不渲染进度条轨道；
 * - target 存在时：名称与「已摄入 / 目标」同行两端对齐，下方渲染进度条。
 */
export function NutrientProgressBar({
  label,
  current,
  target,
  pct,
  colorClass = "bg-amber-400",
  unit
}: NutrientProgressBarProps) {
  const showBar = target !== null && pct !== null;
  const width = pct === null ? 0 : Math.min(Math.max(pct, 0), 100);

  if (target === null) {
    return (
      <div>
        <p className="text-sm text-stone-400">{label}</p>
        <p className="mt-1 text-2xl font-bold text-white">
          {Math.round(current)}
          {unit ? (
            <span className="ml-1 text-sm font-normal text-stone-400">{unit}</span>
          ) : null}
        </p>
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-baseline justify-between gap-3">
        <span className="text-sm text-stone-300">{label}</span>
        <span className="text-sm text-stone-400">
          <span className="font-semibold text-white">{Math.round(current)}</span>
          {` / ${Math.round(target)}`}
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
