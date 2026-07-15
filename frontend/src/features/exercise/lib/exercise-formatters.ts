import type { ExerciseType, MovementType, StructureType } from "../types/exercise";

const structureTypeLabels: Record<StructureType, string> = {
  set_based: "按组动作",
  single_segment: "单段动作"
};

const exerciseTypeLabels: Record<string, string> = {
  strength: "力量",
  cardio: "有氧",
  flexibility: "柔韧",
  mobility: "灵活性",
  balance: "平衡"
};

const movementTypeLabels: Record<string, string> = {
  push: "推",
  pull: "拉",
  squat: "蹲",
  hinge: "髋铰链",
  lunge: "弓步",
  carry: "负重行走",
  rotation: "旋转",
  cardio: "有氧"
};

export function formatExerciseStructureType(value: StructureType) {
  return structureTypeLabels[value] ?? value;
}

export function formatExerciseType(value: ExerciseType | null | undefined) {
  if (!value) {
    return "未分类";
  }

  return exerciseTypeLabels[value] ?? value;
}

export function formatMovementType(value: MovementType | null | undefined) {
  if (!value) {
    return "未标记";
  }

  return movementTypeLabels[value] ?? value;
}
