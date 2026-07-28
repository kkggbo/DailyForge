import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ExercisePickerDialog } from "./ExercisePickerDialog";
import type {
  ExerciseCategoryOption,
  SystemExerciseOption
} from "../../exercise/types/exercise";

const categories: ExerciseCategoryOption[] = [
  {
    categoryCode: "chest",
    categoryName: "Chest",
    sortOrder: 1,
    children: [
      {
        muscleId: 11,
        muscleName: "Upper Chest",
        muscleCode: "upper_chest",
        parentMuscleId: 1,
        parentMuscleName: "Chest",
        sortOrder: 1
      }
    ]
  },
  {
    categoryCode: "back",
    categoryName: "Back",
    sortOrder: 2,
    children: []
  }
];

const results: SystemExerciseOption[] = [
  {
    exerciseId: 1,
    exerciseName: "Bench Press",
    exerciseType: "strength",
    movementType: "push",
    defaultUnit: "kg",
    defaultStructureType: "set_based",
    videoUrl: null,
    primaryMuscles: [{ muscleId: 11, muscleName: "Upper Chest", muscleCode: "upper_chest" }],
    secondaryMuscles: [],
    equipmentNames: ["Barbell"]
  }
];

describe("ExercisePickerDialog", () => {
  it("renders only when open and hides again after close", () => {
    const { rerender } = render(
      <ExercisePickerDialog
        open={false}
        mode="append"
        categories={categories}
        selectedCategoryCode="chest"
        selectedMuscleId={null}
        keyword=""
        results={results}
        isLoadingFilters={false}
        isLoadingResults={false}
        errorMessage={null}
        onClose={vi.fn()}
        onKeywordChange={vi.fn()}
        onKeywordSubmit={vi.fn()}
        onCategoryChange={vi.fn()}
        onMuscleChange={vi.fn()}
        onSelectExercise={vi.fn()}
      />
    );

    expect(screen.queryByText("Exercise Picker")).not.toBeInTheDocument();

    rerender(
      <ExercisePickerDialog
        open
        mode="append"
        categories={categories}
        selectedCategoryCode="chest"
        selectedMuscleId={null}
        keyword=""
        results={results}
        isLoadingFilters={false}
        isLoadingResults={false}
        errorMessage={null}
        onClose={vi.fn()}
        onKeywordChange={vi.fn()}
        onKeywordSubmit={vi.fn()}
        onCategoryChange={vi.fn()}
        onMuscleChange={vi.fn()}
        onSelectExercise={vi.fn()}
      />
    );

    expect(screen.getByText("Exercise Picker")).toBeInTheDocument();

    rerender(
      <ExercisePickerDialog
        open={false}
        mode="append"
        categories={categories}
        selectedCategoryCode="chest"
        selectedMuscleId={null}
        keyword=""
        results={results}
        isLoadingFilters={false}
        isLoadingResults={false}
        errorMessage={null}
        onClose={vi.fn()}
        onKeywordChange={vi.fn()}
        onKeywordSubmit={vi.fn()}
        onCategoryChange={vi.fn()}
        onMuscleChange={vi.fn()}
        onSelectExercise={vi.fn()}
      />
    );

    expect(screen.queryByText("Exercise Picker")).not.toBeInTheDocument();
  });

  it("supports category switching, keyword submit and exercise selection", async () => {
    const user = userEvent.setup();
    const onKeywordChange = vi.fn();
    const onKeywordSubmit = vi.fn();
    const onCategoryChange = vi.fn();
    const onMuscleChange = vi.fn();
    const onSelectExercise = vi.fn();

    render(
      <ExercisePickerDialog
        open
        mode="append"
        categories={categories}
        selectedCategoryCode="chest"
        selectedMuscleId={null}
        keyword=""
        results={results}
        isLoadingFilters={false}
        isLoadingResults={false}
        errorMessage={null}
        onClose={vi.fn()}
        onKeywordChange={onKeywordChange}
        onKeywordSubmit={onKeywordSubmit}
        onCategoryChange={onCategoryChange}
        onMuscleChange={onMuscleChange}
        onSelectExercise={onSelectExercise}
      />
    );

    await user.click(screen.getByRole("button", { name: "Back" }));
    expect(onCategoryChange).toHaveBeenCalledWith("back");

    await user.click(screen.getByRole("button", { name: "Upper Chest" }));
    expect(onMuscleChange).toHaveBeenCalledWith(11);

    const keywordInput = screen.getByRole("textbox");
    await user.type(keywordInput, "bench");
    expect(onKeywordChange).toHaveBeenLastCalledWith("h");

    await user.keyboard("{Enter}");
    expect(onKeywordSubmit).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole("button", { name: /Bench Press/i }));
    expect(onSelectExercise).toHaveBeenCalledWith(results[0]);
  });
});
