import type { SystemExerciseOption } from "../../exercise/types/exercise";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  createDefaultItemByStructureType,
  createEmptyDay,
  createEmptyEditorForm,
  createEmptyExercise,
  createEmptyMetric,
  createExerciseFromSystemOption,
  normalizeExerciseSortOrder,
  normalizeItemIndexes,
  normalizeMetricSortOrder,
  syncDaysWithCycleLength
} from "../lib/cycle-template-mappers";
import {
  hasFieldErrors,
  validateCycleTemplateEditor
} from "../lib/cycle-template-validators";
import type {
  CycleTemplateEditorForm,
  CycleTemplateFieldErrors,
  EditorExerciseForm,
  EditorItemForm,
  EditorMetricForm
} from "../types/cycle-template";

type EditorOptions = {
  allowEmptyCycleLength?: boolean;
  disableBeforeUnload?: boolean;
};

export function useCycleTemplateEditor(
  initialForm: CycleTemplateEditorForm = createEmptyEditorForm(),
  options: EditorOptions = {}
) {
  const [form, setForm] = useState(initialForm);
  const [baseline, setBaseline] = useState(initialForm);
  const [history, setHistory] = useState<CycleTemplateEditorForm[]>([]);
  const formRef = useRef(form);

  useEffect(() => {
    setForm(initialForm);
    setBaseline(initialForm);
    setHistory([]);
  }, [initialForm]);

  useEffect(() => {
    formRef.current = form;
  }, [form]);

  const isDirty = useMemo(
    () => JSON.stringify(form) !== JSON.stringify(baseline),
    [baseline, form]
  );

  const fieldErrors = useMemo<CycleTemplateFieldErrors>(
    () =>
      validateCycleTemplateEditor(form, {
        allowEmptyCycleLength: options.allowEmptyCycleLength ?? false
      }),
    [form, options.allowEmptyCycleLength]
  );

  useEffect(() => {
    if (options.disableBeforeUnload) {
      return;
    }

    function handleBeforeUnload(event: BeforeUnloadEvent) {
      if (JSON.stringify(formRef.current) === JSON.stringify(baseline)) {
        return;
      }

      event.preventDefault();
      event.returnValue = "";
    }

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [baseline, options.disableBeforeUnload]);

  function commit(updater: (current: CycleTemplateEditorForm) => CycleTemplateEditorForm) {
    setForm((current) => {
      setHistory((previous) => [...previous.slice(-19), current]);
      return updater(current);
    });
  }

  function setBaselineToCurrent() {
    setBaseline(formRef.current);
    setHistory([]);
  }

  function resetToBaseline() {
    setForm(baseline);
    setHistory([]);
  }

  function undo() {
    setHistory((previous) => {
      const last = previous.at(-1);
      if (!last) {
        return previous;
      }

      setForm(last);
      return previous.slice(0, -1);
    });
  }

  function updateRootField<K extends keyof CycleTemplateEditorForm>(
    key: K,
    value: CycleTemplateEditorForm[K]
  ) {
    commit((current) => {
      if (key === "cycleLength") {
        return syncDaysWithCycleLength(current, String(value));
      }

      return {
        ...current,
        [key]: value
      };
    });
  }

  function updateDay(dayIndex: number, patch: Partial<CycleTemplateEditorForm["days"][number]>) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              ...patch
            }
          : day
      )
    }));
  }

  function addExercise(dayIndex: number) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              exercises: [
                ...day.exercises,
                createEmptyExercise(day.exercises.length + 1)
              ]
            }
          : day
      )
    }));
  }

  function selectSystemExercise(
    dayIndex: number,
    localId: string,
    option: SystemExerciseOption
  ) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              exercises: normalizeExerciseSortOrder(
                day.exercises.map((exercise) =>
                  exercise.localId === localId
                    ? {
                        ...createExerciseFromSystemOption(option, exercise.sortOrder),
                        localId: exercise.localId,
                        note: exercise.note
                      }
                    : exercise
                )
              )
            }
          : day
      )
    }));
  }

  function updateExercise(
    dayIndex: number,
    localId: string,
    patch: Partial<EditorExerciseForm>
  ) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              exercises: day.exercises.map((exercise) =>
                exercise.localId === localId
                  ? {
                      ...exercise,
                      ...patch
                    }
                  : exercise
              )
            }
          : day
      )
    }));
  }

  function removeExercise(dayIndex: number, localId: string) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              exercises: normalizeExerciseSortOrder(
                day.exercises.filter((exercise) => exercise.localId !== localId)
              )
            }
          : day
      )
    }));
  }

  function moveExercise(dayIndex: number, localId: string, direction: -1 | 1) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) => {
        if (day.dayIndex !== dayIndex) {
          return day;
        }

        return {
          ...day,
          exercises: reorderArrayByLocalId(day.exercises, localId, direction)
        };
      })
    }));
  }

  function reorderExercise(dayIndex: number, fromLocalId: string, toLocalId: string) {
    if (fromLocalId === toLocalId) {
      return;
    }

    commit((current) => ({
      ...current,
      days: current.days.map((day) => {
        if (day.dayIndex !== dayIndex) {
          return day;
        }

        return {
          ...day,
          exercises: reorderArrayByDropTarget(day.exercises, fromLocalId, toLocalId)
        };
      })
    }));
  }

  function addItem(dayIndex: number, exerciseLocalId: string) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) => {
        if (day.dayIndex !== dayIndex) {
          return day;
        }

        return {
          ...day,
          exercises: day.exercises.map((exercise) => {
            if (exercise.localId !== exerciseLocalId || !exercise.structureType) {
              return exercise;
            }

            if (exercise.structureType === "single_segment") {
              return exercise;
            }

            return {
              ...exercise,
              items: normalizeItemIndexes([
                ...exercise.items,
                createDefaultItemByStructureType(
                  exercise.structureType,
                  exercise.items.length + 1
                )
              ])
            };
          })
        };
      })
    }));
  }

  function updateItem(
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    patch: Partial<EditorItemForm>
  ) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              exercises: day.exercises.map((exercise) =>
                exercise.localId === exerciseLocalId
                  ? {
                      ...exercise,
                      items: exercise.items.map((item) =>
                        item.localId === itemLocalId
                          ? {
                              ...item,
                              ...patch
                            }
                          : item
                      )
                    }
                  : exercise
              )
            }
          : day
      )
    }));
  }

  function removeItem(dayIndex: number, exerciseLocalId: string, itemLocalId: string) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              exercises: day.exercises.map((exercise) => {
                if (exercise.localId !== exerciseLocalId) {
                  return exercise;
                }

                return {
                  ...exercise,
                  items: normalizeItemIndexes(
                    exercise.items.filter((item) => item.localId !== itemLocalId)
                  )
                };
              })
            }
          : day
      )
    }));
  }

  function moveItem(
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    direction: -1 | 1
  ) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              exercises: day.exercises.map((exercise) => {
                if (exercise.localId !== exerciseLocalId) {
                  return exercise;
                }

                return {
                  ...exercise,
                  items: normalizeItemIndexes(
                    reorderArrayByLocalId(exercise.items, itemLocalId, direction)
                  )
                };
              })
            }
          : day
      )
    }));
  }

  function addMetric(dayIndex: number, exerciseLocalId: string, itemLocalId: string) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              exercises: day.exercises.map((exercise) =>
                exercise.localId === exerciseLocalId
                  ? {
                      ...exercise,
                      items: exercise.items.map((item) =>
                        item.localId === itemLocalId
                          ? {
                              ...item,
                              metrics: normalizeMetricSortOrder([
                                ...item.metrics,
                                createEmptyMetric(item.metrics.length + 1)
                              ])
                            }
                          : item
                      )
                    }
                  : exercise
              )
            }
          : day
      )
    }));
  }

  function updateMetric(
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    metricLocalId: string,
    patch: Partial<EditorMetricForm>
  ) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              exercises: day.exercises.map((exercise) =>
                exercise.localId === exerciseLocalId
                  ? {
                      ...exercise,
                      items: exercise.items.map((item) =>
                        item.localId === itemLocalId
                          ? {
                              ...item,
                              metrics: item.metrics.map((metric) =>
                                metric.localId === metricLocalId
                                  ? {
                                      ...metric,
                                      ...patch
                                    }
                                  : metric
                              )
                            }
                          : item
                      )
                    }
                  : exercise
              )
            }
          : day
      )
    }));
  }

  function removeMetric(
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    metricLocalId: string
  ) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              exercises: day.exercises.map((exercise) =>
                exercise.localId === exerciseLocalId
                  ? {
                      ...exercise,
                      items: exercise.items.map((item) =>
                        item.localId === itemLocalId
                          ? {
                              ...item,
                              metrics: normalizeMetricSortOrder(
                                item.metrics.filter(
                                  (metric) => metric.localId !== metricLocalId
                                )
                              )
                            }
                          : item
                      )
                    }
                  : exercise
              )
            }
          : day
      )
    }));
  }

  function moveMetric(
    dayIndex: number,
    exerciseLocalId: string,
    itemLocalId: string,
    metricLocalId: string,
    direction: -1 | 1
  ) {
    commit((current) => ({
      ...current,
      days: current.days.map((day) =>
        day.dayIndex === dayIndex
          ? {
              ...day,
              exercises: day.exercises.map((exercise) =>
                exercise.localId === exerciseLocalId
                  ? {
                      ...exercise,
                      items: exercise.items.map((item) =>
                        item.localId === itemLocalId
                          ? {
                              ...item,
                              metrics: normalizeMetricSortOrder(
                                reorderArrayByLocalId(
                                  item.metrics,
                                  metricLocalId,
                                  direction
                                )
                              )
                            }
                          : item
                      )
                    }
                  : exercise
              )
            }
          : day
      )
    }));
  }

  return {
    form,
    fieldErrors,
    hasErrors: hasFieldErrors(fieldErrors),
    isDirty,
    canUndo: history.length > 0,
    updateRootField,
    updateDay,
    addExercise,
    selectSystemExercise,
    updateExercise,
    removeExercise,
    moveExercise,
    reorderExercise,
    addItem,
    updateItem,
    removeItem,
    moveItem,
    addMetric,
    updateMetric,
    removeMetric,
    moveMetric,
    undo,
    resetToBaseline,
    setBaselineToCurrent
  };
}

function reorderArrayByLocalId<T extends { localId: string }>(
  items: T[],
  localId: string,
  direction: -1 | 1
) {
  const nextItems = [...items];
  const currentIndex = nextItems.findIndex((item) => item.localId === localId);
  const targetIndex = currentIndex + direction;

  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= nextItems.length) {
    return items;
  }

  const [moved] = nextItems.splice(currentIndex, 1);
  if (!moved) {
    return items;
  }

  nextItems.splice(targetIndex, 0, moved);
  return nextItems;
}

function reorderArrayByDropTarget<T extends { localId: string }>(
  items: T[],
  fromLocalId: string,
  toLocalId: string
) {
  const fromIndex = items.findIndex((item) => item.localId === fromLocalId);
  const toIndex = items.findIndex((item) => item.localId === toLocalId);

  if (fromIndex < 0 || toIndex < 0 || fromIndex === toIndex) {
    return items;
  }

  const nextItems = [...items];
  const [moved] = nextItems.splice(fromIndex, 1);
  if (!moved) {
    return items;
  }

  nextItems.splice(toIndex, 0, moved);
  return nextItems;
}
