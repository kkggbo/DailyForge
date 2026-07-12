import type {
  AiCompletionScene,
  Gender,
  GoalType,
  ProfileTab,
  TrainingLevel
} from "../types/profile";

export const profileTabOptions: Array<{ value: ProfileTab; label: string }> = [
  { value: "basic", label: "基础档案" },
  { value: "metrics", label: "身体指标" }
];

export const genderOptions: Array<{ value: Gender; label: string }> = [
  { value: "male", label: "男" },
  { value: "female", label: "女" }
];

export const goalTypeOptions: Array<{ value: GoalType; label: string }> = [
  { value: "fat_loss", label: "减脂" },
  { value: "muscle_gain", label: "增肌" },
  { value: "health_maintenance", label: "维持体态 / 保持健康" }
];

export const trainingLevelOptions: Array<{
  value: TrainingLevel;
  label: string;
}> = [
  { value: "beginner", label: "新手" },
  { value: "experienced", label: "有经验" }
];

export const profileFieldLabels: Record<string, string> = {
  gender: "性别",
  birthDate: "出生日期",
  heightCm: "身高",
  goalType: "训练目标",
  trainingLevel: "训练经验",
  injuryNotes: "伤病与注意事项",
  weightKg: "体重",
  recordDate: "记录日期",
  bodyFatPercent: "体脂率",
  bmi: "BMI",
  skeletalMusclePercent: "骨骼肌率",
  bodyWaterPercent: "身体水分",
  basalMetabolicRateKcal: "基础代谢",
  waistCm: "腰围",
  hipCm: "臀围",
  waistHipRatio: "腰臀比",
  bodyAge: "身体年龄",
  bodyType: "体型",
  note: "备注"
};

export const aiSceneMetaMap: Record<
  AiCompletionScene,
  { title: string; description: string }
> = {
  "ai-plan": {
    title: "补充资料以生成更贴合的训练计划",
    description:
      "系统会结合你的基础档案和当前身体状态，给出更合理的训练安排。"
  },
  "ai-nutrition": {
    title: "补充资料以生成更贴合的饮食建议",
    description:
      "完善目标和身体指标后，系统才能更准确地估算每日摄入与调整方向。"
  },
  "ai-summary": {
    title: "补充资料以生成更准确的周期总结",
    description:
      "更完整的档案和体征数据，可以让系统更好地理解你的训练背景与变化。"
  }
};

export function getProfileFieldLabel(field: string) {
  return profileFieldLabels[field] ?? field;
}
