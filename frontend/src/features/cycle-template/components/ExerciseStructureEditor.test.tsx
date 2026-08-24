import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
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

type RenderOptions = {
  fieldErrors?: Record<string, string>;
  locked?: boolean;
};

function renderEditor(options: RenderOptions = {}) {
  const handlers = {
    onRequestReplace: vi.fn(),
    onUpdateExercise: vi.fn(),
    onRemoveExercise: vi.fn(),
    onMoveExercise: vi.fn(),
    onAddItem: vi.fn(),
    onUpdateItem: vi.fn(),
    onRemoveItem: vi.fn(),
    onMoveItem: vi.fn(),
    onAddMetric: vi.fn(),
    onUpdateMetric: vi.fn(),
    onRemoveMetric: vi.fn(),
    onMoveMetric: vi.fn()
  };

  render(
    <ExerciseStructureEditor
      dayIndex={0}
      exerciseIndex={0}
      exercise={exercise}
      locked={options.locked ?? false}
      fieldErrors={options.fieldErrors ?? {}}
      {...handlers}
    />
  );

  return handlers;
}

describe("ExerciseStructureEditor", () => {
  it("renders collapsed by default without the note editor or items", () => {
    renderEditor();

    expect(screen.queryByPlaceholderText("例如：最后一组接近力竭")).not.toBeInTheDocument();
    expect(screen.queryByText("执行项名称")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "更换动作" })).toBeInTheDocument();
  });

  it("expands to reveal the note editor and items, then collapses to hide them", async () => {
    const user = userEvent.setup();
    renderEditor();

    const expandButton = screen.getByRole("button", { name: "展开" });
    expect(expandButton).toHaveAttribute("aria-expanded", "false");

    await user.click(expandButton);
    expect(screen.getByPlaceholderText("例如：最后一组接近力竭")).toBeInTheDocument();
    expect(screen.getByText("执行项名称")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "收起" })).toHaveAttribute("aria-expanded", "true");

    await user.click(screen.getByRole("button", { name: "收起" }));
    expect(screen.queryByPlaceholderText("例如：最后一组接近力竭")).not.toBeInTheDocument();
    expect(screen.queryByText("执行项名称")).not.toBeInTheDocument();
  });

  it("shows a pending-fix badge when collapsed and errors exist", () => {
    renderEditor({
      fieldErrors: {
        "day.0.exercise.0.exerciseId": "还没有选择系统动作。",
        "day.0.exercise.0.items": "当前动作至少需要 1 个执行项。"
      }
    });

    expect(screen.getByText("2 项待修正")).toBeInTheDocument();
  });

  it("keeps the replace-exercise action clickable", async () => {
    const user = userEvent.setup();
    const handlers = renderEditor();

    await user.click(screen.getByRole("button", { name: "更换动作" }));
    expect(handlers.onRequestReplace).toHaveBeenCalledTimes(1);
  });

  it("calls onUpdateExercise with the note patch when editing the note", async () => {
    const user = userEvent.setup();
    const handlers = renderEditor();

    await user.click(screen.getByRole("button", { name: "展开" }));
    const noteTextarea = screen.getByPlaceholderText("例如：最后一组接近力竭");
    await user.type(noteTextarea, "Needs less warmup");

    expect(handlers.onUpdateExercise).toHaveBeenCalled();
    expect(handlers.onUpdateExercise.mock.lastCall?.[0]).toEqual({
      note: "Needs less warmup"
    });
  });

  it("allows expanding when locked while edit controls stay disabled", async () => {
    const user = userEvent.setup();
    renderEditor({ locked: true });

    expect(screen.getByRole("button", { name: "更换动作" })).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "展开" }));

    const noteTextarea = screen.getByPlaceholderText("例如：最后一组接近力竭");
    expect(noteTextarea).toBeInTheDocument();
    expect(noteTextarea).toBeDisabled();
  });
});
