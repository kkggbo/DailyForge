import type {
  ExerciseCardMeta,
  SystemExerciseDetailResponse,
  SystemExerciseOption
} from "../types/exercise";

export function mapSystemExerciseOptionToCardMeta(
  option: SystemExerciseOption
): ExerciseCardMeta {
  return {
    exerciseId: option.exerciseId,
    exerciseType: option.exerciseType,
    movementType: option.movementType,
    defaultUnit: option.defaultUnit,
    videoUrl: option.videoUrl,
    primaryMuscles: option.primaryMuscles.map((muscle) => muscle.muscleName),
    secondaryMuscles: option.secondaryMuscles.map((muscle) => muscle.muscleName),
    equipmentNames: option.equipmentNames
  };
}

export function mapSystemExerciseDetailToCardMeta(
  detail: SystemExerciseDetailResponse
): ExerciseCardMeta {
  return {
    exerciseId: detail.exerciseId,
    exerciseType: detail.exerciseType,
    movementType: detail.movementType,
    defaultUnit: detail.defaultUnit,
    videoUrl: detail.videoUrl,
    primaryMuscles: detail.primaryMuscles.map((muscle) => muscle.muscleName),
    secondaryMuscles: detail.secondaryMuscles.map((muscle) => muscle.muscleName),
    equipmentNames: detail.equipments.map((equipment) => equipment.equipmentName)
  };
}
