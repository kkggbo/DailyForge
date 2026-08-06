import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { AiCoachHistoryPage } from "./AiCoachHistoryPage";

const { getTemplateGenerationHistoryMock, getCycleSummaryHistoryMock } = vi.hoisted(
  () => ({
    getTemplateGenerationHistoryMock: vi.fn(),
    getCycleSummaryHistoryMock: vi.fn()
  })
);

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    accessToken: "ai-token"
  })
}));

vi.mock("../api/ai-coach", () => ({
  getTemplateGenerationHistory: getTemplateGenerationHistoryMock,
  getCycleSummaryHistory: getCycleSummaryHistoryMock
}));

describe("AiCoachHistoryPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getCycleSummaryHistoryMock.mockResolvedValue({
      page: 1,
      pageSize: 10,
      total: 0,
      records: []
    });
  });

  it("shows only one terminal badge for completed records", async () => {
    getTemplateGenerationHistoryMock.mockResolvedValue({
      page: 1,
      pageSize: 10,
      total: 1,
      records: [
        {
          taskId: 9001,
          taskType: "template_generation",
          taskStatus: "succeeded",
          progressStage: "completed",
          sceneType: "gym",
          goalType: "muscle_gain",
          cycleLength: 4,
          includeCardio: true,
          additionalRequirements: "保留一整天休息",
          templateId: 501,
          templateName: "AI Push Pull",
          summaryText: "四天分化。",
          createdAt: "2026-08-01T10:00:00",
          completedAt: "2026-08-01T10:00:05",
          updatedAt: "2026-08-01T10:00:05"
        }
      ]
    });

    render(
      <MemoryRouter initialEntries={["/ai-coach/history?tab=template-generations"]}>
        <AiCoachHistoryPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("AI Push Pull")).toBeInTheDocument();
    expect(screen.getAllByText("已完成")).toHaveLength(1);
    expect(screen.getByText("含补充要求")).toBeInTheDocument();
  });
});
