export type StructureType = "set_based" | "single_segment";

export type ExerciseType =
  | "strength"
  | "cardio"
  | "flexibility"
  | "mobility"
  | "balance"
  | string;

export type MovementType =
  | "push"
  | "pull"
  | "squat"
  | "hinge"
  | "lunge"
  | "carry"
  | "rotation"
  | "cardio"
  | string;

export type SceneType = "home" | "gym" | string;

export type SystemExerciseSearchQuery = {
  keyword?: string;
  exerciseType?: string;
  movementType?: string;
  structureType?: StructureType;
  sceneType?: string;
  muscleId?: number;
  page?: number;
  pageSize?: number;
};

export type SystemExerciseOption = {
  exerciseId: number;
  exerciseName: string;
  exerciseType: ExerciseType;
  movementType: MovementType | null;
  defaultUnit: string | null;
  defaultStructureType: StructureType;
  videoUrl: string | null;
  primaryMuscles: string[];
  secondaryMuscles: string[];
  equipmentNames: string[];
};

export type SystemExerciseSearchResponse = {
  page: number;
  pageSize: number;
  total: number;
  records: SystemExerciseOption[];
};

export type ExerciseMuscleRelation = {
  muscleId: number;
  muscleName: string;
  muscleCode: string;
  relationType: "primary" | "secondary";
};

export type ExerciseEquipment = {
  equipmentId: number;
  equipmentName: string;
  sceneType: SceneType;
};

export type SystemExerciseDetailResponse = {
  exerciseId: number;
  exerciseName: string;
  exerciseType: ExerciseType;
  movementType: MovementType | null;
  defaultUnit: string | null;
  defaultStructureType: StructureType;
  videoUrl: string | null;
  calorieBurnReference: number | null;
  calorieReferenceUnit: string | null;
  primaryMuscles: ExerciseMuscleRelation[];
  secondaryMuscles: ExerciseMuscleRelation[];
  equipments: ExerciseEquipment[];
};
