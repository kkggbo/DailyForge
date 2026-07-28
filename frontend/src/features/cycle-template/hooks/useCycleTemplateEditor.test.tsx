import { renderHook, act } from "@testing-library/react";
import { useCycleTemplateEditor } from "./useCycleTemplateEditor";
import type { CycleTemplateEditorForm } from "../types/cycle-template";

const initialForm: CycleTemplateEditorForm = {
  templateName: "Push Pull Legs",
  goalType: "muscle_gain",
  cycleLength: "1",
  days: [
    {
      dayIndex: 1,
      dayName: "Push",
      exercises: [
        {
          localId: "exercise-1",
          sortOrder: 1,
          exerciseId: 1,
          exerciseName: "Bench Press",
          structureType: "set_based",
          note: "",
          items: [
            {
              localId: "item-1",
              itemIndex: 1,
              itemType: "set",
              itemName: "第1组",
              note: "top set",
              metrics: [
                {
                  localId: "metric-1",
                  sortOrder: 1,
                  metricKey: "weight_kg",
                  metricValueNumberText: "80"
                },
                {
                  localId: "metric-2",
                  sortOrder: 2,
                  metricKey: "reps",
                  metricValueNumberText: "8"
                }
              ]
            }
          ]
        }
      ]
    }
  ]
};

describe("useCycleTemplateEditor", () => {
  it("copies the previous set when adding a new set item", () => {
    const { result } = renderHook(() =>
      useCycleTemplateEditor(initialForm, { disableBeforeUnload: true })
    );

    act(() => {
      result.current.addItem(1, "exercise-1");
    });

    const items = result.current.form.days[0]?.exercises[0]?.items ?? [];
    expect(items).toHaveLength(2);
    expect(items[1]).toMatchObject({
      itemIndex: 2,
      itemType: "set",
      note: "",
      metrics: [
        { sortOrder: 1, metricKey: "weight_kg", metricValueNumberText: "80" },
        { sortOrder: 2, metricKey: "reps", metricValueNumberText: "8" }
      ]
    });
  });
});
