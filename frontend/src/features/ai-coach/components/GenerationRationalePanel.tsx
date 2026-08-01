import { formatIntensityBasisType } from "../lib/ai-coach-formatters";
import type { GenerationRationale } from "../types/ai-coach";

type GenerationRationalePanelProps = {
  rationale: GenerationRationale;
};

export function GenerationRationalePanel({
  rationale
}: GenerationRationalePanelProps) {
  return (
    <section className="space-y-5 rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
      <div>
        <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
          Generation Rationale
        </p>
        <h2 className="mt-3 text-3xl font-semibold text-white">AI 设计说明</h2>
        <p className="mt-3 leading-7 text-stone-300">
          {rationale.overallDesignSummary}
        </p>
      </div>

      <section className="rounded-3xl border border-white/10 bg-black/20 p-5">
        <p className="text-sm font-semibold text-white">强度建议依据</p>
        <p className="mt-2 text-xs uppercase tracking-[0.18em] text-amber-300">
          {formatIntensityBasisType(rationale.intensityRationale.basisType)}
        </p>
        <p className="mt-3 leading-7 text-stone-300">
          {rationale.intensityRationale.summary}
        </p>
      </section>

      {rationale.dayRationales.length > 0 ? (
        <section className="rounded-3xl border border-white/10 bg-black/20 p-5">
          <p className="text-sm font-semibold text-white">每日目标与安排</p>
          <div className="mt-4 space-y-4">
            {rationale.dayRationales.map((item) => (
              <article
                key={`${item.dayIndex}-${item.dayName}`}
                className="rounded-2xl border border-white/10 bg-white/6 p-4"
              >
                <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-xs uppercase tracking-[0.16em] text-amber-300">
                      Day {item.dayIndex}
                    </p>
                    <h3 className="mt-1 text-lg font-semibold text-white">
                      {item.dayName}
                    </h3>
                  </div>
                  <span className="rounded-full border border-white/10 bg-black/20 px-3 py-1 text-xs text-stone-200">
                    {item.focusSummary}
                  </span>
                </div>
                <p className="mt-3 text-sm leading-6 text-stone-300">
                  {item.rationale}
                </p>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      {rationale.keyExerciseRationales.length > 0 ? (
        <section className="rounded-3xl border border-white/10 bg-black/20 p-5">
          <p className="text-sm font-semibold text-white">关键动作说明</p>
          <div className="mt-4 space-y-3">
            {rationale.keyExerciseRationales.map((item) => (
              <article
                key={`${item.dayIndex}-${item.exerciseId}`}
                className="rounded-2xl border border-white/10 bg-white/6 p-4"
              >
                <p className="text-sm font-semibold text-white">
                  Day {item.dayIndex} · {item.exerciseName}
                </p>
                <p className="mt-2 text-sm leading-6 text-stone-300">
                  {item.rationale}
                </p>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      {rationale.warnings.length > 0 ? (
        <section className="rounded-3xl border border-amber-300/20 bg-amber-300/10 p-5">
          <p className="text-sm font-semibold text-white">风险提醒</p>
          <ul className="mt-4 space-y-2 text-sm leading-6 text-stone-200">
            {rationale.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </section>
      ) : null}
    </section>
  );
}
