import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { CycleTemplateEditPage } from "./CycleTemplateEditPage";
import {
  getCycleTemplateDetail,
  updateDraftTemplate,
  updateFormalTemplate
} from "../api/cycle-template";
import type { CycleTemplateDetailResponse } from "../types/cycle-template";

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({ accessToken: "test-token" })
}));

vi.mock("../api/cycle-template", () => ({
  getCycleTemplateDetail: vi.fn(),
  updateDraftTemplate: vi.fn(),
  updateFormalTemplate: vi.fn()
}));

vi.mock("../components/CycleTemplateEditor", () => ({
  CycleTemplateEditor: ({ onSubmit, isSubmitting }: { onSubmit: () => void; isSubmitting: boolean }) => (
    <button type="button" disabled={isSubmitting} onClick={onSubmit}>
      保存模板
    </button>
  )
}));

const activeTemplateDetail: CycleTemplateDetailResponse = {
  templateId: 101,
  templateName: "Active Push Plan",
  goalType: "muscle_gain",
  status: "active",
  cycleLength: 1,
  isActive: true,
  currentDayIndex: 1,
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
          exerciseId: 201,
          exerciseName: "Bench Press",
          structureType: "set_based",
          note: null,
          items: [
            {
              itemIndex: 1,
              itemType: "set",
              itemName: "Set 1",
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
    }
  ]
};

describe("CycleTemplateEditPage", () => {
  beforeEach(() => {
    vi.mocked(getCycleTemplateDetail).mockResolvedValue(activeTemplateDetail);
    vi.mocked(updateDraftTemplate).mockResolvedValue({ templateId: 101, status: "draft" });
    vi.mocked(updateFormalTemplate).mockResolvedValue({ templateId: 101, status: "active" });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("requires confirmation before saving an active template because it overwrites the current workout session", async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={["/cycle-templates/101/edit"]}>
        <Routes>
          <Route path="/cycle-templates/:templateId/edit" element={<CycleTemplateEditPage />} />
        </Routes>
      </MemoryRouter>
    );

    await user.click(await screen.findByRole("button", { name: "保存模板" }));

    expect(screen.getByRole("heading", { name: "确认保存正在运行的模板？" })).toBeInTheDocument();
    expect(updateFormalTemplate).not.toHaveBeenCalled();

    await user.click(screen.getByRole("button", { name: "确认保存并覆盖当前训练日" }));

    await waitFor(() => expect(updateFormalTemplate).toHaveBeenCalledTimes(1));
    expect(updateFormalTemplate).toHaveBeenCalledWith(
      "test-token",
      101,
      expect.objectContaining({
        templateName: "Active Push Plan",
        confirmOverwriteCurrentSession: true,
        days: expect.arrayContaining([expect.objectContaining({ dayIndex: 1 })])
      })
    );
  });
});
