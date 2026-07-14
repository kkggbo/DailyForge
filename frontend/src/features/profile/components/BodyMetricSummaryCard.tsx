import {
  formatDateTime,
  formatNullableInteger,
  formatNullableNumber,
  formatNullableText
} from "../lib/profile-formatters";
import type { BodyMetricSnapshotResponse } from "../types/profile";

type BodyMetricSummaryCardProps = {
  snapshot: BodyMetricSnapshotResponse | null;
};

export function BodyMetricSummaryCard({
  snapshot
}: BodyMetricSummaryCardProps) {
  const items = [
    { label: "体重", value: formatNullableNumber(snapshot?.currentWeightKg, { unit: "kg" }) },
    {
      label: "体脂率",
      value: formatNullableNumber(snapshot?.currentBodyFatPercent, { unit: "%" })
    },
    { label: "BMI", value: formatNullableNumber(snapshot?.currentBmi) },
    {
      label: "骨骼肌率",
      value: formatNullableNumber(snapshot?.currentSkeletalMusclePercent, {
        unit: "%"
      })
    },
    {
      label: "身体水分",
      value: formatNullableNumber(snapshot?.currentBodyWaterPercent, { unit: "%" })
    },
    {
      label: "基础代谢",
      value: formatNullableNumber(snapshot?.currentBasalMetabolicRateKcal, {
        unit: "kcal"
      })
    },
    { label: "腰围", value: formatNullableNumber(snapshot?.currentWaistCm, { unit: "cm" }) },
    { label: "臀围", value: formatNullableNumber(snapshot?.currentHipCm, { unit: "cm" }) },
    { label: "腰臀比", value: formatNullableNumber(snapshot?.currentWaistHipRatio) },
    { label: "身体年龄", value: formatNullableInteger(snapshot?.currentBodyAge, " 岁") },
    { label: "体型", value: formatNullableText(snapshot?.currentBodyType) }
  ];

  return (
    <section className="rounded-[32px] border border-white/10 bg-white/5 p-6 backdrop-blur">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h3 className="text-2xl font-semibold text-white">当前身体状态</h3>
          <p className="mt-2 text-sm leading-6 text-stone-300">
            这里展示的是当前已知快照，不要求所有字段都来自同一次测量。
          </p>
        </div>
        <p className="text-sm text-stone-400">
          最近更新：{formatDateTime(snapshot?.updatedAt)}
        </p>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {items.map((item) => (
          <article
            key={item.label}
            className="rounded-3xl border border-white/10 bg-black/20 p-4"
          >
            <p className="text-sm text-stone-400">{item.label}</p>
            <p className="mt-2 text-lg font-semibold text-white">{item.value}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
