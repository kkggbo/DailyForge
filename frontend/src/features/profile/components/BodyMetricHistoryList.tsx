import {
  formatDate,
  formatNullableInteger,
  formatNullableNumber,
  formatNullableText
} from "../lib/profile-formatters";
import type { BodyMetricsPageResponse } from "../types/profile";

type BodyMetricHistoryListProps = {
  data: BodyMetricsPageResponse | null;
  isLoading: boolean;
  onPageChange: (page: number) => void;
  onDeleteLatestRequest: () => void;
};

export function BodyMetricHistoryList({
  data,
  isLoading,
  onPageChange,
  onDeleteLatestRequest
}: BodyMetricHistoryListProps) {
  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.pageSize)) : 1;

  return (
    <section className="rounded-[32px] border border-white/10 bg-white/5 p-6 backdrop-blur">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h3 className="text-2xl font-semibold text-white">历史记录</h3>
          <p className="mt-2 text-sm leading-6 text-stone-300">
            第一版先保留简洁分页，趋势图和更复杂的分析后续再接到统计模块。
          </p>
        </div>
        <p className="text-sm text-stone-400">共 {data?.total ?? 0} 条记录</p>
      </div>

      {isLoading ? (
        <div className="mt-6 rounded-3xl border border-white/10 bg-black/20 px-5 py-4 text-sm text-stone-300">
          正在加载历史记录...
        </div>
      ) : data && data.records.length > 0 ? (
        <>
          <div className="mt-6 space-y-4">
            {data.records.map((record) => (
              <article
                key={record.id}
                className="rounded-3xl border border-white/10 bg-black/20 p-5"
              >
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h4 className="text-lg font-semibold text-white">
                        {formatDate(record.recordDate)}
                      </h4>
                      {record.isLatest ? (
                        <span className="rounded-full bg-amber-400/15 px-3 py-1 text-xs text-amber-200">
                          最新记录
                        </span>
                      ) : null}
                    </div>
                    <p className="mt-2 text-sm text-stone-400">ID: {record.id}</p>
                  </div>

                  {record.isLatest ? (
                    <button
                      type="button"
                      onClick={onDeleteLatestRequest}
                      className="rounded-full border border-rose-300/25 px-4 py-2 text-sm text-rose-200 transition hover:bg-rose-400/10"
                    >
                      删除最新记录
                    </button>
                  ) : null}
                </div>

                <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                  <HistoryMetric label="体重" value={formatNullableNumber(record.weightKg, { unit: "kg" })} />
                  <HistoryMetric label="体脂率" value={formatNullableNumber(record.bodyFatPercent, { unit: "%" })} />
                  <HistoryMetric label="BMI" value={formatNullableNumber(record.bmi)} />
                  <HistoryMetric label="骨骼肌率" value={formatNullableNumber(record.skeletalMusclePercent, { unit: "%" })} />
                  <HistoryMetric label="身体水分" value={formatNullableNumber(record.bodyWaterPercent, { unit: "%" })} />
                  <HistoryMetric label="基础代谢" value={formatNullableNumber(record.basalMetabolicRateKcal, { unit: "kcal" })} />
                  <HistoryMetric label="腰围" value={formatNullableNumber(record.waistCm, { unit: "cm" })} />
                  <HistoryMetric label="臀围" value={formatNullableNumber(record.hipCm, { unit: "cm" })} />
                  <HistoryMetric label="腰臀比" value={formatNullableNumber(record.waistHipRatio)} />
                  <HistoryMetric label="身体年龄" value={formatNullableInteger(record.bodyAge, " 岁")} />
                  <HistoryMetric label="体型" value={formatNullableText(record.bodyType)} />
                </div>

                <div className="mt-4 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-stone-300">
                  <span className="text-stone-400">备注：</span>
                  {formatNullableText(record.note)}
                </div>
              </article>
            ))}
          </div>

          <div className="mt-6 flex items-center justify-between gap-3">
            <button
              type="button"
              disabled={!data || data.page <= 1}
              onClick={() => onPageChange((data?.page ?? 1) - 1)}
              className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/8 disabled:cursor-not-allowed disabled:opacity-50"
            >
              上一页
            </button>
            <p className="text-sm text-stone-400">
              第 {data?.page ?? 1} / {totalPages} 页
            </p>
            <button
              type="button"
              disabled={!data || data.page >= totalPages}
              onClick={() => onPageChange((data?.page ?? 1) + 1)}
              className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/8 disabled:cursor-not-allowed disabled:opacity-50"
            >
              下一页
            </button>
          </div>
        </>
      ) : (
        <div className="mt-6 rounded-3xl border border-dashed border-white/10 bg-black/20 px-5 py-8 text-center text-sm text-stone-400">
          还没有身体指标记录，先录入一条作为后续分析和建议的基础。
        </div>
      )}
    </section>
  );
}

type HistoryMetricProps = {
  label: string;
  value: string;
};

function HistoryMetric({ label, value }: HistoryMetricProps) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
      <p className="text-xs uppercase tracking-[0.18em] text-stone-500">{label}</p>
      <p className="mt-2 text-sm font-medium text-white">{value}</p>
    </div>
  );
}
