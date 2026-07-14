import {
  formatCycleLength,
  formatGoalType,
  formatNullableNumber,
  formatStatus
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
              {formatCycleLength(detail.cycleLength)} · 目标：
              {formatGoalType(detail.goalType)}
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
            <div className="mt-5 space-y-3">
              {day.exercises.map((exercise) => (
                <article
                  key={`${day.dayIndex}-${exercise.sortOrder}-${exercise.exerciseId}`}
                  className="rounded-2xl border border-white/10 bg-white/6 p-4"
                >
                  <div className="flex flex-col gap-2 lg:flex-row lg:items-start lg:justify-between">
                    <div>
                      <p className="text-xs text-stone-400">
                        #{exercise.sortOrder}
                      </p>
                      <h3 className="text-xl font-semibold text-white">
                        {exercise.exerciseName}
                      </h3>
                    </div>
                    <div className="flex flex-wrap gap-2 text-xs text-stone-200">
                      <Badge>组数 {formatNullableNumber(exercise.targetSets)}</Badge>
                      <Badge>
                        次数 {formatNullableNumber(exercise.targetRepsMin)}-
                        {formatNullableNumber(exercise.targetRepsMax)}
                      </Badge>
                      <Badge>
                        重量 {formatNullableNumber(exercise.targetWeightKg, "kg")}
                      </Badge>
                      <Badge>
                        时长{" "}
                        {formatNullableNumber(exercise.targetDurationSeconds, "秒")}
                      </Badge>
                      <Badge>
                        休息 {formatNullableNumber(exercise.restSeconds, "秒")}
                      </Badge>
                      <Badge>RPE {formatNullableNumber(exercise.targetRpe)}</Badge>
                    </div>
                  </div>
                  {exercise.note ? (
                    <p className="mt-3 text-sm leading-6 text-stone-300">
                      {exercise.note}
                    </p>
                  ) : null}
                </article>
              ))}
            </div>
          )}
        </section>
      ))}
    </div>
  );
}

function Badge({ children }: { children: React.ReactNode }) {
  return (
    <span className="rounded-full border border-white/10 bg-white/8 px-3 py-1">
      {children}
    </span>
  );
}
