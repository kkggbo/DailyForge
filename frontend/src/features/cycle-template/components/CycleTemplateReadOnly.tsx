import type { ReactNode } from "react";
import {
  formatCycleLength,
  formatGoalType,
  formatItemType,
  formatMetricKey,
  formatMetricValue,
  formatStatus,
  formatStructureType
} from "../lib/cycle-template-formatters";
import type { CycleTemplateDetailResponse } from "../types/cycle-template";

export function CycleTemplateReadOnly({
  detail
}: {
  detail: CycleTemplateDetailResponse;
}) {
  return (
    <div className="space-y-6">
      <section className="rounded-[28px] border border-white/10 bg-white/8 p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              {formatStatus(detail.status)}
            </p>
            <h1 className="mt-3 text-4xl font-semibold text-white">
              {detail.templateName}
            </h1>
            <p className="mt-3 text-stone-300">
              {formatCycleLength(detail.cycleLength)} · 目标：{formatGoalType(detail.goalType)}
            </p>
          </div>
          {detail.isActive ? (
            <span className="rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950">
              当前启用 · Day {detail.currentDayIndex}
            </span>
          ) : null}
        </div>
      </section>

      {detail.days.map((day) => (
        <section
          key={day.dayIndex}
          className="rounded-[28px] border border-white/10 bg-black/20 p-5"
        >
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-sm text-amber-300">Day {day.dayIndex}</p>
              <h2 className="text-2xl font-semibold text-white">
                {day.dayName ?? `Day ${day.dayIndex}`}
              </h2>
            </div>
            {day.isLocked ? (
              <span className="rounded-full border border-white/10 px-3 py-1 text-xs text-stone-300">
                已锁定
              </span>
            ) : null}
          </div>

          {day.exercises.length === 0 ? (
            <div className="mt-5 rounded-2xl border border-dashed border-white/12 bg-white/5 px-4 py-6 text-center text-stone-300">
              休息日
            </div>
          ) : (
            <div className="mt-5 space-y-4">
              {day.exercises.map((exercise) => (
                <article
                  key={`${day.dayIndex}-${exercise.sortOrder}-${exercise.exerciseId}`}
                  className="rounded-3xl border border-white/10 bg-white/6 p-5"
                >
                  <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                    <div>
                      <p className="text-xs uppercase tracking-[0.2em] text-amber-300">
                        动作 {exercise.sortOrder}
                      </p>
                      <h3 className="mt-1 text-xl font-semibold text-white">
                        {exercise.exerciseName}
                      </h3>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Badge>{formatStructureType(exercise.structureType)}</Badge>
                      <Badge>{exercise.items.length} 个执行项</Badge>
                    </div>
                  </div>

                  {exercise.note ? (
                    <p className="mt-3 text-sm leading-6 text-stone-300">
                      {exercise.note}
                    </p>
                  ) : null}

                  <div className="mt-4 space-y-3">
                    {exercise.items.map((item) => (
                      <div
                        key={`${exercise.exerciseId}-${item.itemIndex}`}
                        className="rounded-2xl border border-white/10 bg-stone-950/45 p-4"
                      >
                        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                          <div>
                            <p className="text-sm font-semibold text-white">
                              {item.itemName || `${formatItemType(item.itemType)} ${item.itemIndex}`}
                            </p>
                            <p className="text-xs text-stone-400">
                              {formatItemType(item.itemType)}
                            </p>
                          </div>
                          <div className="flex flex-wrap gap-2 text-xs text-stone-200">
                            {item.metrics.map((metric) => (
                              <Badge key={`${item.itemIndex}-${metric.sortOrder}-${metric.metricKey}`}>
                                {formatMetricKey(metric.metricKey)} {formatMetricValue(metric)}
                              </Badge>
                            ))}
                          </div>
                        </div>

                        {item.note ? (
                          <p className="mt-3 text-sm leading-6 text-stone-300">
                            {item.note}
                          </p>
                        ) : null}
                      </div>
                    ))}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      ))}
    </div>
  );
}

function Badge({ children }: { children: ReactNode }) {
  return (
    <span className="rounded-full border border-white/10 bg-white/8 px-3 py-1 text-xs">
      {children}
    </span>
  );
}
