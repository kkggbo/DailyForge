export type CycleTemplateStatus = "draft" | "active" | "inactive" | "deleted";

export type CycleTemplateTab = "formal" | "drafts";

export type StructureType = "set_based" | "single_segment";

export type ItemType = "set" | "segment";

export type MetricKey =
  | "weight_kg"
  | "reps"
  | "duration_seconds"
  | "distance_km"
  | "speed_kmh"
  | "pace_seconds_per_km"
  | "incline_percent"
  | "rest_seconds"
  | "rpe"
  | "intensity_level";

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

export type CycleTemplateMetricResponse = {
  sortOrder: number;
  metricKey: MetricKey;
  metricValueNumber: number;
  metricUnit: string | null;
};

export type CycleTemplateItemResponse = {
  itemIndex: number;
  itemType: ItemType;
  itemName: string | null;
  note: string | null;
  metrics: CycleTemplateMetricResponse[];
};

export type CycleTemplateExerciseResponse = {
  sortOrder: number;
  exerciseId: number;
  exerciseName: string;
  structureType: StructureType;
  note: string | null;
  items: CycleTemplateItemResponse[];
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

export type SaveCycleTemplateMetricPayload = {
  sortOrder: number;
  metricKey: MetricKey;
  metricValueNumber: number;
};

export type SaveCycleTemplateItemPayload = {
  itemIndex: number;
  itemType: ItemType;
  itemName: string | null;
  note: string | null;
  metrics: SaveCycleTemplateMetricPayload[];
};

export type SaveCycleTemplateExercisePayload = {
  sortOrder: number;
  exerciseId: number;
  structureType: StructureType;
  note: string | null;
  items: SaveCycleTemplateItemPayload[];
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
  structureType: StructureType | null;
  note: string;
  items: EditorItemForm[];
};

export type EditorItemForm = {
  localId: string;
  itemIndex: number;
  itemType: ItemType;
  itemName: string;
  note: string;
  metrics: EditorMetricForm[];
};

export type EditorMetricForm = {
  localId: string;
  sortOrder: number;
  metricKey: MetricKey | "";
  metricValueNumberText: string;
};

export type CycleTemplateFieldErrors = Record<string, string>;
