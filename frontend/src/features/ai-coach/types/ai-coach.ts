import type { GoalType } from "../../profile/types/profile";
import type {
  ItemType,
  MetricKey,
  StructureType
} from "../../cycle-template/types/cycle-template";

export type MissingFieldCode =
  | "gender"
  | "birthDate"
  | "heightCm"
  | "goalType"
  | "trainingLevel"
  | "currentWeightKg";

export type SceneType = "gym" | "home";

export type IntensityBasisType =
  | "historical_performance"
  | "starting_recommendation";

export type AiTaskType = "template_generation" | "cycle_summary";

export type AiTaskStatus = "pending" | "running" | "succeeded" | "failed";

export type AiTaskProgressStage =
  | "queued"
  | "generating_result"
  | "calling_tool"
  | "repairing_output"
  | "completed"
  | "failed";

export type AiTaskToolCallStatus = "succeeded" | "failed";

export type TemplateGenerationCapability = {
  available: boolean;
  ready: boolean;
  missingRequiredFields: MissingFieldCode[];
  allowedSceneTypes: SceneType[];
  allowedGoalTypes: GoalType[];
  minCycleLength: number;
  maxCycleLength: number;
};

export type CycleSummaryCapability = {
  available: boolean;
  ready: boolean;
  latestCompletedCycleRunId: number | null;
  latestCompletedAt: string | null;
  recommendedMissingFields: MissingFieldCode[];
};

export type AiCoachCapabilities = {
  aiEnabled: boolean;
  accountTier: string;
  platformRole: string;
  templateGeneration: TemplateGenerationCapability;
  cycleSummary: CycleSummaryCapability;
};

export type TemplateGenerationForm = {
  sceneType: SceneType;
  goalType: GoalType;
  cycleLengthText: string;
  includeCardio: boolean;
  additionalRequirements: string;
};

export type CreateTemplateGenerationPayload = {
  clientRequestId: string;
  sceneType: SceneType;
  goalType: GoalType;
  cycleLength: number;
  includeCardio: boolean;
  additionalRequirements: string | null;
};

export type GeneratedDraftTemplate = {
  templateId: number;
  templateName: string;
  templateStatus: "draft";
  cycleLength: number;
  days: GeneratedDraftTemplateDay[];
};

export type GeneratedDraftTemplateMetric = {
  sortOrder: number;
  metricKey: MetricKey;
  metricValueNumber: number;
  metricUnit: string | null;
};

export type GeneratedDraftTemplateItem = {
  itemIndex: number;
  itemType: ItemType;
  itemName: string | null;
  note: string | null;
  metrics: GeneratedDraftTemplateMetric[];
};

export type GeneratedDraftTemplateExercise = {
  sortOrder: number;
  exerciseId: number;
  exerciseName: string;
  structureType: StructureType;
  note: string | null;
  items: GeneratedDraftTemplateItem[];
};

export type GeneratedDraftTemplateDay = {
  dayIndex: number;
  dayName: string | null;
  isRestDay: boolean;
  exercises: GeneratedDraftTemplateExercise[];
};

export type GenerationRationale = {
  overallDesignSummary: string;
  dayRationales: Array<{
    dayIndex: number;
    dayName: string;
    focusSummary: string;
    rationale: string;
  }>;
  keyExerciseRationales: Array<{
    dayIndex: number;
    exerciseId: number;
    exerciseName: string;
    rationale: string;
  }>;
  intensityRationale: {
    basisType: IntensityBasisType;
    summary: string;
  };
  warnings: string[];
};

export type TemplateGenerationTaskResult = {
  draftTemplate: GeneratedDraftTemplate;
  generationRationale: GenerationRationale;
};

export type CreateCycleSummaryPayload = {
  clientRequestId: string;
  cycleRunId: number;
};

export type CycleSummaryTaskResult = {
  cycleRunId: number;
  templateId: number;
  templateName: string;
  runNo: number;
  cycleLength: number;
  executionOverview: string;
  strengths: string[];
  issues: string[];
  causeAnalysis: string[];
  nextCycleSuggestions: string[];
  risks: string[];
  dataCompletenessNotice: string | null;
};

export type AiTaskAcceptedResponse<TTaskType extends AiTaskType = AiTaskType> = {
  taskId: number;
  taskType: TTaskType;
  taskStatus: AiTaskStatus;
  createdAt: string;
  pollAfterSeconds: number;
};

export type AiTaskLatestToolCall = {
  roundNo: number;
  toolName: string;
  toolDisplayName?: string | null;
  status: AiTaskToolCallStatus;
  createdAt: string;
};

export type TemplateGenerationRequestSnapshot = {
  sceneType: SceneType;
  goalType: GoalType;
  cycleLength: number;
  includeCardio: boolean;
  additionalRequirements: string | null;
};

export type AiTaskBase<TTaskType extends AiTaskType = AiTaskType> = {
  taskId: number;
  taskType: TTaskType;
  taskStatus: AiTaskStatus;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  progressStage: AiTaskProgressStage;
  latestToolCall: AiTaskLatestToolCall | null;
  requestSnapshot?: TemplateGenerationRequestSnapshot | null;
  updatedAt: string;
  pollAfterSeconds?: number | null;
};

export type AiTaskResponse<
  TTaskType extends AiTaskType,
  TResult
> = AiTaskBase<TTaskType> & {
  result: TResult | null;
};

export type TemplateGenerationTaskResponse = AiTaskResponse<
  "template_generation",
  TemplateGenerationTaskResult
>;

export type CycleSummaryTaskResponse = AiTaskResponse<
  "cycle_summary",
  CycleSummaryTaskResult
>;

export type AiTaskHistoryQuery = {
  page?: number;
  pageSize?: number;
};

export type AiTaskHistoryPage<TRecord> = {
  page: number;
  pageSize: number;
  total: number;
  records: TRecord[];
};

export type TemplateGenerationHistoryItem = {
  taskId: number;
  taskType: "template_generation";
  taskStatus: AiTaskStatus;
  progressStage: AiTaskProgressStage;
  sceneType: SceneType;
  goalType: GoalType;
  cycleLength: number;
  includeCardio: boolean;
  additionalRequirements: string | null;
  templateId: number | null;
  templateName: string | null;
  summaryText: string | null;
  createdAt: string;
  completedAt: string | null;
  updatedAt: string;
};

export type CycleSummaryHistoryItem = {
  taskId: number;
  taskType: "cycle_summary";
  taskStatus: AiTaskStatus;
  progressStage: AiTaskProgressStage;
  cycleRunId: number;
  templateId: number | null;
  templateName: string | null;
  runNo: number | null;
  cycleLength: number | null;
  summaryText: string | null;
  createdAt: string;
  completedAt: string | null;
  updatedAt: string;
};

export type TemplateGenerationHistoryPage = AiTaskHistoryPage<TemplateGenerationHistoryItem>;

export type CycleSummaryHistoryPage = AiTaskHistoryPage<CycleSummaryHistoryItem>;
