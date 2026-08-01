import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { CycleSummaryTaskPage } from "./CycleSummaryTaskPage";

const { getCycleSummaryTaskMock } = vi.hoisted(() => ({
  getCycleSummaryTaskMock: vi.fn()
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
  return {
    ...actual,
    useParams: () => ({ taskId: "9101" })
  };
});

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    accessToken: "ai-token"
  })
}));

vi.mock("../api/ai-coach", () => ({
  getCycleSummaryTask: getCycleSummaryTaskMock
}));

describe("CycleSummaryTaskPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the succeeded cycle summary result", async () => {
    getCycleSummaryTaskMock.mockResolvedValue({
      taskId: 9101,
      taskType: "cycle_summary",
      taskStatus: "succeeded",
      createdAt: "2026-08-01T10:00:00",
      startedAt: "2026-08-01T10:00:01",
      completedAt: "2026-08-01T10:00:05",
      errorCode: null,
      errorMessage: null,
      result: {
        cycleRunId: 1201,
        templateId: 301,
        templateName: "四天分化",
        runNo: 3,
        cycleLength: 4,
        executionOverview: "本轮完成度稳定。",
        strengths: ["执行稳定"],
        issues: ["下肢后段疲劳"],
        causeAnalysis: ["下肢日总量偏高"],
        nextCycleSuggestions: ["降低下肢辅助动作"],
        risks: ["注意膝部恢复"],
        dataCompletenessNotice: "补充更多身体指标会更准确。"
      }
    });

    render(
      <MemoryRouter>
        <CycleSummaryTaskPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("本轮完成度稳定。")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "查看对应模板" })).toHaveAttribute(
      "href",
      "/cycle-templates/301"
    );
  });
});
