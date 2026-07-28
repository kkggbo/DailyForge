import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { ExerciseStructureEditor } from "./ExerciseStructureEditor";
import type { EditorExerciseForm } from "../types/cycle-template";

const exercise: EditorExerciseForm = {
  localId: "exercise-1",
  sortOrder: 1,
  exerciseId: 1001,
  exerciseName: "Bench Press",
  structureType: "set_based",
  note: "",
  items: [
    {
      localId: "item-1",
      itemIndex: 1,
      itemType: "set",
      itemName: "第1组",
      note: "",
      metrics: []
    }
  ]
};

describe("ExerciseStructureEditor", () => {
  it("expands note editing on demand and allows replacing the exercise", async () => {
    const user = userEvent.setup();
    const onRequestReplace = vi.fn();
    const onUpdateExercise = vi.fn();

    function TestHarness() {
      const [currentExercise, setCurrentExercise] = useState(exercise);

      return (
        <ExerciseStructureEditor
          dayIndex={0}
          exerciseIndex={0}
          exercise={currentExercise}
          locked={false}
          fieldErrors={{}}
          onRequestReplace={onRequestReplace}
          onUpdateExercise={(patch) => {
            onUpdateExercise(patch);
            setCurrentExercise((previous) => ({
              ...previous,
              ...patch
            }));
          }}
          onRemoveExercise={vi.fn()}
          onMoveExercise={vi.fn()}
          onAddItem={vi.fn()}
          onUpdateItem={vi.fn()}
          onRemoveItem={vi.fn()}
          onMoveItem={vi.fn()}
          onAddMetric={vi.fn()}
          onUpdateMetric={vi.fn()}
          onRemoveMetric={vi.fn()}
          onMoveMetric={vi.fn()}
        />
      );
    }

    const { container } = render(
      <TestHarness />
    );

    expect(container.querySelector("textarea")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /备注/ }));
    expect(container.querySelector("textarea")).toBeInTheDocument();

    await user.type(container.querySelector("textarea") as HTMLTextAreaElement, "Needs less warmup");
    expect(onUpdateExercise).toHaveBeenCalled();
    expect(onUpdateExercise.mock.lastCall?.[0]).toEqual({ note: "Needs less warmup" });

    await user.click(screen.getByRole("button", { name: /更换/ }));
    expect(onRequestReplace).toHaveBeenCalledTimes(1);
  });
});
