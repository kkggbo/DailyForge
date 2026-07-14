import type {
  CycleTemplateDetailResponse,
  CycleTemplateEditorForm,
  EditorDayForm,
  EditorExerciseForm,
  SaveCycleTemplateDayPayload,
  SaveCycleTemplateExercisePayload,
  SaveCycleTemplatePayload
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
            targetSets: toText(exercise.targetSets),
            targetRepsMin: toText(exercise.targetRepsMin),
            targetRepsMax: toText(exercise.targetRepsMax),
            targetWeightKg: toText(exercise.targetWeightKg),
            targetDurationSeconds: toText(exercise.targetDurationSeconds),
            restSeconds: toText(exercise.restSeconds),
            targetRpe: toText(exercise.targetRpe),
            note: exercise.note ?? "",
            targetExtraJsonText: exercise.targetExtraJson
              ? JSON.stringify(exercise.targetExtraJson, null, 2)
              : ""
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
    targetSets: "",
    targetRepsMin: "",
    targetRepsMax: "",
    targetWeightKg: "",
    targetDurationSeconds: "",
    restSeconds: "",
    targetRpe: "",
    note: "",
    targetExtraJsonText: ""
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

  const days = Array.from({ length: nextLength }, (_, index) => {
    const dayIndex = index + 1;
    return form.days.find((day) => day.dayIndex === dayIndex) ?? createEmptyDay(dayIndex);
  });

  return {
    ...form,
    cycleLength: nextCycleLengthText,
    days
  };
}

export function editorFormToPayload(
  form: CycleTemplateEditorForm,
  options: { includeOnlyEditableFromDay?: number } = {}
): SaveCycleTemplatePayload {
  const cycleLength = parseOptionalInteger(form.cycleLength);
  const days = form.days
    .filter((day) =>
      options.includeOnlyEditableFromDay
        ? day.dayIndex >= options.includeOnlyEditableFromDay
        : true
    )
    .map(mapDayToPayload);

  return {
    templateName: form.templateName.trim(),
    cycleLength,
    goalType: toNullableText(form.goalType),
    days
  };
}

function mapDayToPayload(day: EditorDayForm): SaveCycleTemplateDayPayload {
  return {
    dayIndex: day.dayIndex,
    dayName: toNullableText(day.dayName),
    exercises: day.exercises.map((exercise, index) => mapExerciseToPayload(exercise, index))
  };
}

function mapExerciseToPayload(
  exercise: EditorExerciseForm,
  index: number
): SaveCycleTemplateExercisePayload {
  return {
    sortOrder: index + 1,
    exerciseId: exercise.exerciseId ?? 0,
    targetSets: parseOptionalInteger(exercise.targetSets),
    targetRepsMin: parseOptionalInteger(exercise.targetRepsMin),
    targetRepsMax: parseOptionalInteger(exercise.targetRepsMax),
    targetWeightKg: parseOptionalNumber(exercise.targetWeightKg),
    targetDurationSeconds: parseOptionalInteger(exercise.targetDurationSeconds),
    restSeconds: parseOptionalInteger(exercise.restSeconds),
    targetRpe: parseOptionalNumber(exercise.targetRpe),
    note: toNullableText(exercise.note),
    targetExtraJson: parseOptionalJson(exercise.targetExtraJsonText)
  };
}

function toText(value: number | null | undefined) {
  return value === null || value === undefined ? "" : String(value);
}

function toNullableText(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function parseOptionalInteger(value: string) {
  const trimmed = value.trim();
  return trimmed ? Number.parseInt(trimmed, 10) : null;
}

function parseOptionalNumber(value: string) {
  const trimmed = value.trim();
  return trimmed ? Number(trimmed) : null;
}

function parseOptionalJson(value: string) {
  const trimmed = value.trim();
  return trimmed ? (JSON.parse(trimmed) as Record<string, unknown>) : null;
}

function createLocalId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
