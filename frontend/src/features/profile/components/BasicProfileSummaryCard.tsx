import { useState } from "react";
import { createPortal } from "react-dom";
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
import type { ActivityLevel, ProfileBasicResponse } from "../types/profile";

type BasicProfileSummaryCardProps = {
  basicProfile: ProfileBasicResponse | null;
};

const activityLevelExplanations: Array<{
  value: ActivityLevel;
  label: string;
  meaning: string;
  example: string;
}> = [
  {
    value: "sedentary",
    label: "久坐",
    meaning: "基本不运动或很少运动，日常以坐着办公为主。",
    example: "例如：办公室工作，没有规律锻炼。"
  },
  {
    value: "light",
    label: "轻度",
    meaning: "每周有 1~3 次轻度运动或活动。",
    example: "例如：每周散步、慢跑 1~3 次。"
  },
  {
    value: "moderate",
    label: "中度",
    meaning: "每周有 3~5 次中等强度运动。",
    example: "例如：每周去健身房训练 3~5 次。"
  },
  {
    value: "high",
    label: "高强度",
    meaning: "每周有 6~7 次较高强度训练。",
    example: "例如：几乎每天大重量训练或高强度间歇训练。"
  },
  {
    value: "very_high",
    label: "极高",
    meaning: "每天进行大强度训练或体力劳动。",
    example: "例如：专业运动员一天两练，或高强度体力工作。"
  }
];

export function BasicProfileSummaryCard({
  basicProfile
}: BasicProfileSummaryCardProps) {
  const [showActivityHelp, setShowActivityHelp] = useState(false);
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
    { label: "性别", value: formatNullableText(genderLabel), helpKey: null as string | null },
    { label: "出生日期", value: formatDate(basicProfile?.birthDate), helpKey: null },
    {
      label: "身高",
      value: formatNullableNumber(basicProfile?.heightCm, { digits: 0, unit: "cm" }),
      helpKey: null
    },
    { label: "训练目标", value: formatNullableText(goalLabel), helpKey: null },
    { label: "训练经验", value: formatNullableText(trainingLabel), helpKey: null },
    {
      label: "活动量",
      value: basicProfile?.activityLevel ? formatNullableText(activityLabel) : "--",
      helpKey: "activity"
    },
    {
      label: "当前体重",
      value: formatNullableNumber(basicProfile?.currentWeightKg, { unit: "kg" }),
      helpKey: null
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
            <p className="flex items-center gap-1.5 text-sm text-stone-400">
              {item.label}
              {item.helpKey === "activity" ? (
                <button
                  type="button"
                  onClick={() => setShowActivityHelp(true)}
                  aria-label="查看活动量说明"
                  className="df-round-btn flex h-5 w-5 shrink-0 items-center justify-center rounded-full border border-white/20 text-xs text-stone-400 transition hover:bg-white/10 hover:text-white"
                >
                  ?
                </button>
              ) : null}
            </p>
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

      {showActivityHelp
        ? createPortal(
            <ActivityLevelHelpDialog onClose={() => setShowActivityHelp(false)} />,
            document.body
          )
        : null}
    </section>
  );
}

function ActivityLevelHelpDialog({ onClose }: { onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 px-6">
      <div className="max-h-[86vh] w-full max-w-lg overflow-y-auto rounded-[32px] border border-white/10 bg-stone-950 p-6 shadow-[0_24px_80px_rgba(0,0,0,0.55)]">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              Activity Level
            </p>
            <h3 className="mt-1 text-2xl font-semibold text-white">活动量说明</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="关闭说明"
            className="rounded-full border border-white/10 px-4 py-2 text-sm text-stone-200 transition hover:bg-white/10"
          >
            关闭
          </button>
        </div>

        <p className="mt-4 text-sm leading-6 text-stone-400">
          活动量用于估算你每天的总能量消耗，会直接影响「每日目标」的自动计算。
        </p>

        <div className="mt-5 space-y-3">
          {activityLevelExplanations.map((item) => (
            <article
              key={item.value}
              className="rounded-2xl border border-white/10 bg-white/5 p-4"
            >
              <p className="font-semibold text-white">{item.label}</p>
              <p className="mt-1 text-sm leading-6 text-stone-300">
                {item.meaning}
              </p>
              <p className="mt-1 text-sm leading-6 text-stone-400">
                {item.example}
              </p>
            </article>
          ))}
        </div>
      </div>
    </div>
  );
}
