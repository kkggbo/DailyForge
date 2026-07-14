import { useEffect, useMemo, useRef, useState } from "react";
import {
  createEmptyEditorForm,
  createEmptyExercise,
  syncDaysWithCycleLength
} from "../lib/cycle-template-mappers";
import {
  hasFieldErrors,
  validateCycleTemplateEditor
} from "../lib/cycle-template-validators";
import type {
  CycleTemplateEditorForm,
  CycleTemplateFieldErrors,
  EditorExerciseForm
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
              exercises: normalizeSortOrder(
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

        const nextExercises = [...day.exercises];
        const currentIndex = nextExercises.findIndex(
          (exercise) => exercise.localId === localId
        );
        const targetIndex = currentIndex + direction;

        if (
          currentIndex < 0 ||
          targetIndex < 0 ||
          targetIndex >= nextExercises.length
        ) {
          return day;
        }

        const [moved] = nextExercises.splice(currentIndex, 1);
        if (!moved) {
          return day;
        }

        nextExercises.splice(targetIndex, 0, moved);

        return {
          ...day,
          exercises: normalizeSortOrder(nextExercises)
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

        const fromIndex = day.exercises.findIndex(
          (exercise) => exercise.localId === fromLocalId
        );
        const toIndex = day.exercises.findIndex(
          (exercise) => exercise.localId === toLocalId
        );

        if (fromIndex < 0 || toIndex < 0) {
          return day;
        }

        const nextExercises = [...day.exercises];
        const [moved] = nextExercises.splice(fromIndex, 1);
        if (!moved) {
          return day;
        }

        nextExercises.splice(toIndex, 0, moved);

        return {
          ...day,
          exercises: normalizeSortOrder(nextExercises)
        };
      })
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
    updateExercise,
    removeExercise,
    moveExercise,
    reorderExercise,
    undo,
    resetToBaseline,
    setBaselineToCurrent
  };
}

function normalizeSortOrder(exercises: EditorExerciseForm[]) {
  return exercises.map((exercise, index) => ({
    ...exercise,
    sortOrder: index + 1
  }));
}
