import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { CycleTemplateReadOnly } from "./CycleTemplateReadOnly";
import type { CycleTemplateDetailResponse } from "../types/cycle-template";

const detail: CycleTemplateDetailResponse = {
  templateId: 301,
  templateName: "AI Push Pull",
  goalType: "muscle_gain",
  status: "draft",
  sourceType: "ai_generated",
  cycleLength: 2,
  isActive: false,
  currentDayIndex: null,
  editableFromDayIndex: 1,
  canActivate: false,
  canDelete: false,
  createdAt: null,
  updatedAt: null,
  days: [
    {
      dayIndex: 1,
      dayName: "Push",
      isRestDay: false,
      isLocked: false,
      exercises: [
        {
          sortOrder: 1,
          exerciseId: 101,
          exerciseName: "Bench Press",
          structureType: "set_based",
          note: null,
          items: [
            {
              itemIndex: 1,
              itemType: "set",
              itemName: "第 1 组",
              note: null,
              metrics: [
                {
                  sortOrder: 1,
                  metricKey: "weight_kg",
                  metricValueNumber: 60,
                  metricUnit: "kg"
                }
              ]
            }
          ]
        }
      ]
    },
    {
      dayIndex: 2,
      dayName: "Rest",
      isRestDay: true,
      isLocked: false,
      exercises: []
    }
  ]
};

describe("CycleTemplateReadOnly", () => {
  it("switches days like the workout day navigator and shows the ai badge", async () => {
    const user = userEvent.setup();

    render(<CycleTemplateReadOnly detail={detail} viewMode="day-tabs" />);

    expect(screen.getByText("AI生成")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Push" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Day 2.*Rest/i }));

    expect(screen.getByRole("heading", { name: "Rest" })).toBeInTheDocument();
    expect(screen.getAllByText("休息日").length).toBeGreaterThan(0);
    expect(screen.queryByRole("heading", { name: "Push" })).not.toBeInTheDocument();
  });
});
