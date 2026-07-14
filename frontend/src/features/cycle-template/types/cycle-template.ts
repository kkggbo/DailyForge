export type CycleTemplateStatus = "draft" | "active" | "inactive" | "deleted";

export type CycleTemplateTab = "formal" | "drafts";

export type FormalTemplateListItem = {
  templateId: number;
  templateName: string;
  cycleLength: number | null;
  goalType: string | null;
  status: CycleTemplateStatus;
  isActive: boolean;
  currentDayIndex: number | null;
  updatedAt: string | null;
};

export type DraftTemplateListItem = {
  templateId: number;
  templateName: string;
  cycleLength: number | null;
  configuredDayCount: number;
  createdAt: string | null;
  updatedAt: string | null;
};

export type FormalTemplateListResponse = {
  activeTemplateId: number | null;
  records: FormalTemplateListItem[];
};

export type DraftTemplateListResponse = {
  records: DraftTemplateListItem[];
};

export type CycleTemplateExerciseResponse = {
  sortOrder: number;
  exerciseId: number;
  exerciseName: string;
  targetSets: number | null;
  targetRepsMin: number | null;
  targetRepsMax: number | null;
  targetWeightKg: number | null;
  targetDurationSeconds: number | null;
  restSeconds: number | null;
  targetRpe: number | null;
  note: string | null;
  targetExtraJson: Record<string, unknown> | null;
};

export type CycleTemplateDayResponse = {
  dayIndex: number;
  dayName: string | null;
  isRestDay: boolean;
  isLocked: boolean;
  exercises: CycleTemplateExerciseResponse[];
};

export type CycleTemplateDetailResponse = {
  templateId: number;
  templateName: string;
  goalType: string | null;
  status: CycleTemplateStatus;
  cycleLength: number | null;
  isActive: boolean;
  currentDayIndex: number | null;
  editableFromDayIndex: number;
  canActivate: boolean;
  canDelete: boolean;
  createdAt: string | null;
  updatedAt: string | null;
  days: CycleTemplateDayResponse[];
};

export type CurrentActiveTemplateResponse = {
  templateId: number;
  templateName: string;
  cycleLength: number;
  currentDayIndex: number;
  currentDayName: string | null;
  editableFromDayIndex: number;
  startedAt: string | null;
};

export type SaveCycleTemplateExercisePayload = {
  sortOrder: number;
  exerciseId: number;
  targetSets: number | null;
  targetRepsMin: number | null;
  targetRepsMax: number | null;
  targetWeightKg: number | null;
  targetDurationSeconds: number | null;
  restSeconds: number | null;
  targetRpe: number | null;
  note: string | null;
  targetExtraJson: Record<string, unknown> | null;
};

export type SaveCycleTemplateDayPayload = {
  dayIndex: number;
  dayName: string | null;
  exercises: SaveCycleTemplateExercisePayload[];
};

export type SaveCycleTemplatePayload = {
  templateName: string;
  cycleLength: number | null;
  goalType: string | null;
  days: SaveCycleTemplateDayPayload[];
};

export type CreateOrCopyCycleTemplateResponse = {
  templateId: number;
  status: CycleTemplateStatus;
};

export type ActivateCycleTemplatePayload = {
  confirmSwitch?: boolean;
};

export type ActivateCycleTemplateResponse = {
  templateId: number;
  status: CycleTemplateStatus;
  currentDayIndex: number;
  previousActiveTemplateId: number | null;
};

export type DeleteCycleTemplateResponse = {
  templateId: number;
  status: CycleTemplateStatus;
};

export type AiGenerateCycleTemplatePayload = {
  goalType: string | null;
  cycleLength: number | null;
  prompt: string;
  useProfileData: boolean;
};

export type SystemExerciseOption = {
  exerciseId: number;
  exerciseName: string;
  exerciseType: string;
  movementType: string | null;
  defaultUnit: string | null;
};

export type SystemExerciseSearchResponse = {
  records: SystemExerciseOption[];
  page?: number;
  pageSize?: number;
  total?: number;
};

export type CycleTemplateEditorForm = {
  templateName: string;
  goalType: string;
  cycleLength: string;
  days: EditorDayForm[];
};

export type EditorDayForm = {
  dayIndex: number;
  dayName: string;
  exercises: EditorExerciseForm[];
};

export type EditorExerciseForm = {
  localId: string;
  sortOrder: number;
  exerciseId: number | null;
  exerciseName: string;
  targetSets: string;
  targetRepsMin: string;
  targetRepsMax: string;
  targetWeightKg: string;
  targetDurationSeconds: string;
  restSeconds: string;
  targetRpe: string;
  note: string;
  targetExtraJsonText: string;
};

export type CycleTemplateFieldErrors = Record<string, string>;
