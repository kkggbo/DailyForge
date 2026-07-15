import type { SystemExerciseOption } from "../../exercise/types/exercise";
import type {
  CycleTemplateDetailResponse,
  CycleTemplateEditorForm,
  EditorDayForm,
  EditorExerciseForm,
  EditorItemForm,
  EditorMetricForm,
  MetricKey,
  SaveCycleTemplateDayPayload,
  SaveCycleTemplateExercisePayload,
  SaveCycleTemplateItemPayload,
  SaveCycleTemplateMetricPayload,
  SaveCycleTemplatePayload,
  StructureType
} from "../types/cycle-template";

export function createEmptyEditorForm(): CycleTemplateEditorForm {
  return {
    templateName: "",
    goalType: "",
    cycleLength: "3",
    days: Array.from({ length: 3 }, (_, index) => createEmptyDay(index + 1))
  };
}

export function detailToEditorForm(
  detail: CycleTemplateDetailResponse
): CycleTemplateEditorForm {
  const cycleLength = detail.cycleLength ?? Math.max(detail.days.length, 1);
  const dayMap = new Map(detail.days.map((day) => [day.dayIndex, day]));

  return {
    templateName: detail.templateName ?? "",
    goalType: detail.goalType ?? "",
    cycleLength: detail.cycleLength ? String(detail.cycleLength) : "",
    days: Array.from({ length: cycleLength }, (_, index) => {
      const dayIndex = index + 1;
      const source = dayMap.get(dayIndex);

      if (!source) {
        return createEmptyDay(dayIndex);
      }

      return {
        dayIndex,
        dayName: source.dayName ?? "",
        exercises: source.exercises
          .slice()
          .sort((left, right) => left.sortOrder - right.sortOrder)
          .map((exercise) => ({
            localId: createLocalId(),
            sortOrder: exercise.sortOrder,
            exerciseId: exercise.exerciseId,
            exerciseName: exercise.exerciseName,
            structureType: exercise.structureType,
            note: exercise.note ?? "",
            items: exercise.items
              .slice()
              .sort((left, right) => left.itemIndex - right.itemIndex)
              .map((item) => ({
                localId: createLocalId(),
                itemIndex: item.itemIndex,
                itemType: item.itemType,
                itemName: item.itemName ?? "",
                note: item.note ?? "",
                metrics: item.metrics
                  .slice()
                  .sort((left, right) => left.sortOrder - right.sortOrder)
                  .map((metric) => ({
                    localId: createLocalId(),
                    sortOrder: metric.sortOrder,
                    metricKey: metric.metricKey,
                    metricValueNumberText: String(metric.metricValueNumber)
                  }))
              }))
          }))
      };
    })
  };
}

export function createEmptyDay(dayIndex: number): EditorDayForm {
  return {
    dayIndex,
    dayName: "",
    exercises: []
  };
}

export function createEmptyExercise(sortOrder: number): EditorExerciseForm {
  return {
    localId: createLocalId(),
    sortOrder,
    exerciseId: null,
    exerciseName: "",
    structureType: null,
    note: "",
    items: []
  };
}

export function createExerciseFromSystemOption(
  option: SystemExerciseOption,
  sortOrder: number
): EditorExerciseForm {
  return {
    localId: createLocalId(),
    sortOrder,
    exerciseId: option.exerciseId,
    exerciseName: option.exerciseName,
    structureType: option.defaultStructureType,
    note: "",
    items: [createDefaultItemByStructureType(option.defaultStructureType, 1)]
  };
}

export function createDefaultItemByStructureType(
  structureType: StructureType,
  itemIndex: number
): EditorItemForm {
  return {
    localId: createLocalId(),
    itemIndex,
    itemType: structureType === "set_based" ? "set" : "segment",
    itemName: structureType === "set_based" ? `第${itemIndex}组` : "主训练段",
    note: "",
    metrics: []
  };
}

export function createEmptyMetric(sortOrder: number): EditorMetricForm {
  return {
    localId: createLocalId(),
    sortOrder,
    metricKey: "",
    metricValueNumberText: ""
  };
}

export function syncDaysWithCycleLength(
  form: CycleTemplateEditorForm,
  nextCycleLengthText: string
): CycleTemplateEditorForm {
  const nextLength = Number(nextCycleLengthText);

  if (!Number.isInteger(nextLength) || nextLength < 1 || nextLength > 7) {
    return {
      ...form,
      cycleLength: nextCycleLengthText
    };
  }

  return {
    ...form,
    cycleLength: nextCycleLengthText,
    days: Array.from({ length: nextLength }, (_, index) => {
      const dayIndex = index + 1;
      return form.days.find((day) => day.dayIndex === dayIndex) ?? createEmptyDay(dayIndex);
    })
  };
}

export function editorFormToPayload(
  form: CycleTemplateEditorForm,
  options: { includeOnlyEditableFromDay?: number } = {}
): SaveCycleTemplatePayload {
  return {
    templateName: form.templateName.trim(),
    cycleLength: parseOptionalInteger(form.cycleLength),
    goalType: toNullableText(form.goalType),
    days: form.days
      .filter((day) =>
        options.includeOnlyEditableFromDay
          ? day.dayIndex >= options.includeOnlyEditableFromDay
          : true
      )
      .map(mapDayToPayload)
  };
}

function mapDayToPayload(day: EditorDayForm): SaveCycleTemplateDayPayload {
  return {
    dayIndex: day.dayIndex,
    dayName: toNullableText(day.dayName),
    exercises: normalizeExerciseSortOrder(day.exercises).map((exercise, exerciseIndex) =>
      mapExerciseToPayload(exercise, exerciseIndex + 1)
    )
  };
}

function mapExerciseToPayload(
  exercise: EditorExerciseForm,
  sortOrder: number
): SaveCycleTemplateExercisePayload {
  return {
    sortOrder,
    exerciseId: exercise.exerciseId ?? 0,
    structureType: exercise.structureType ?? "set_based",
    note: toNullableText(exercise.note),
    items: normalizeItemIndexes(exercise.items).map((item, itemIndex) =>
      mapItemToPayload(item, itemIndex + 1)
    )
  };
}

function mapItemToPayload(
  item: EditorItemForm,
  itemIndex: number
): SaveCycleTemplateItemPayload {
  return {
    itemIndex,
    itemType: item.itemType,
    itemName: toNullableText(item.itemName),
    note: toNullableText(item.note),
    metrics: normalizeMetricSortOrder(item.metrics).map((metric, metricIndex) =>
      mapMetricToPayload(metric, metricIndex + 1)
    )
  };
}

function mapMetricToPayload(
  metric: EditorMetricForm,
  sortOrder: number
): SaveCycleTemplateMetricPayload {
  return {
    sortOrder,
    metricKey: metric.metricKey as MetricKey,
    metricValueNumber: Number(metric.metricValueNumberText)
  };
}

export function normalizeExerciseSortOrder(exercises: EditorExerciseForm[]) {
  return exercises.map((exercise, index) => ({
    ...exercise,
    sortOrder: index + 1
  }));
}

export function normalizeItemIndexes(items: EditorItemForm[]) {
  return items.map((item, index) => ({
    ...item,
    itemIndex: index + 1,
    itemName: deriveDefaultItemName(item.itemType, index + 1, item.itemName)
  }));
}

export function normalizeMetricSortOrder(metrics: EditorMetricForm[]) {
  return metrics.map((metric, index) => ({
    ...metric,
    sortOrder: index + 1
  }));
}

function deriveDefaultItemName(
  itemType: EditorItemForm["itemType"],
  itemIndex: number,
  currentValue: string
) {
  const trimmed = currentValue.trim();
  if (trimmed) {
    return trimmed;
  }

  return itemType === "set" ? `第${itemIndex}组` : "主训练段";
}

function toNullableText(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function parseOptionalInteger(value: string) {
  const trimmed = value.trim();
  return trimmed ? Number.parseInt(trimmed, 10) : null;
}

function createLocalId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
