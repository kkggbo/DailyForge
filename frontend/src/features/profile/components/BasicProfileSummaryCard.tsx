import {
  activityLevelOptions,
  genderOptions,
  goalTypeOptions,
  trainingLevelOptions
} from "../lib/profile-enums";
import {
  formatDate,
  formatNullableNumber,
  formatNullableText
} from "../lib/profile-formatters";
import type { ProfileBasicResponse } from "../types/profile";

type BasicProfileSummaryCardProps = {
  basicProfile: ProfileBasicResponse | null;
};

export function BasicProfileSummaryCard({
  basicProfile
}: BasicProfileSummaryCardProps) {
  const genderLabel = genderOptions.find(
    (option) => option.value === basicProfile?.gender
  )?.label;
  const goalLabel = goalTypeOptions.find(
    (option) => option.value === basicProfile?.goalType
  )?.label;
  const trainingLabel = trainingLevelOptions.find(
    (option) => option.value === basicProfile?.trainingLevel
  )?.label;
  const activityLabel = activityLevelOptions.find(
    (option) => option.value === basicProfile?.activityLevel
  )?.label;

  const items = [
    { label: "性别", value: formatNullableText(genderLabel) },
    { label: "出生日期", value: formatDate(basicProfile?.birthDate) },
    {
      label: "身高",
      value: formatNullableNumber(basicProfile?.heightCm, { digits: 0, unit: "cm" })
    },
    { label: "训练目标", value: formatNullableText(goalLabel) },
    { label: "训练经验", value: formatNullableText(trainingLabel) },
    {
      label: "活动量",
      value: basicProfile?.activityLevel ? formatNullableText(activityLabel) : "--"
    },
    {
      label: "当前体重",
      value: formatNullableNumber(basicProfile?.currentWeightKg, { unit: "kg" })
    }
  ];

  return (
    <section className="rounded-[32px] border border-white/10 bg-white/5 p-6 backdrop-blur">
      <h3 className="text-2xl font-semibold text-white">基础档案</h3>
      <p className="mt-2 text-sm leading-6 text-stone-300">
        这些资料用于生成更贴合你的训练与饮食建议。
      </p>

      <div className="mt-6 grid grid-cols-2 gap-3 max-[299px]:grid-cols-1 sm:grid-cols-3">
        {items.map((item) => (
          <article
            key={item.label}
            className="rounded-3xl border border-white/10 bg-black/20 p-3 sm:p-4"
          >
            <p className="text-sm text-stone-400">{item.label}</p>
            <p className="mt-2 text-lg font-semibold text-white">{item.value}</p>
          </article>
        ))}
      </div>

      <div className="mt-4 rounded-3xl border border-white/10 bg-black/20 p-4">
        <p className="text-sm text-stone-400">伤病与注意事项</p>
        <p className="mt-2 whitespace-pre-wrap text-base leading-7 text-white">
          {formatNullableText(basicProfile?.injuryNotes)}
        </p>
      </div>
    </section>
  );
}
