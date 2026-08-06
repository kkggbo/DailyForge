import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { TemplateGenerationTaskPage } from "./TemplateGenerationTaskPage";

const { getTemplateGenerationTaskMock } = vi.hoisted(() => ({
  getTemplateGenerationTaskMock: vi.fn()
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>(
    "react-router-dom"
  );
  return {
    ...actual,
    useParams: () => ({ taskId: "9001" })
  };
});

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    accessToken: "ai-token"
  })
}));

vi.mock("../api/ai-coach", () => ({
  getTemplateGenerationTask: getTemplateGenerationTaskMock
}));

describe("TemplateGenerationTaskPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the succeeded task result, hides duplicate progress sections, and shows additional requirements", async () => {
    getTemplateGenerationTaskMock.mockResolvedValue({
      taskId: 9001,
      taskType: "template_generation",
      taskStatus: "succeeded",
      createdAt: "2026-08-01T10:00:00",
      startedAt: "2026-08-01T10:00:01",
      completedAt: "2026-08-01T10:00:05",
      errorCode: null,
      errorMessage: null,
      progressStage: "completed",
      latestToolCall: {
        roundNo: 1,
        toolName: "search_candidate_exercises",
        toolDisplayName: "搜索候选动作",
        status: "succeeded",
        createdAt: "2026-08-01T10:00:02"
      },
      requestSnapshot: {
        sceneType: "gym",
        goalType: "muscle_gain",
        cycleLength: 4,
        includeCardio: true,
        additionalRequirements: "保留一整天完整休息日"
      },
      updatedAt: "2026-08-01T10:00:05",
      result: {
        draftTemplate: {
          templateId: 501,
          templateName: "AI Push Pull",
          templateStatus: "draft",
          cycleLength: 4,
          days: []
        },
        generationRationale: {
          overallDesignSummary: "以推拉腿为基础生成。",
          dayRationales: [],
          keyExerciseRationales: [],
          intensityRationale: {
            basisType: "starting_recommendation",
            summary: "从保守起始重量开始。"
          },
          warnings: []
        }
      }
    });

    const { container } = render(
      <MemoryRouter>
        <TemplateGenerationTaskPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("AI Push Pull")).toBeInTheDocument();
    expect(screen.getByText("保留一整天完整休息日")).toBeInTheDocument();
    expect(container.querySelector('a[href="/cycle-templates/501/edit"]')).not.toBeNull();
    expect(screen.queryByText("search_candidate_exercises")).not.toBeInTheDocument();
    expect(screen.queryByText("Draft Preview")).not.toBeInTheDocument();
  });

  it("falls back to the raw tool name when the localized tool display name is missing", async () => {
    getTemplateGenerationTaskMock.mockResolvedValue({
      taskId: 9001,
      taskType: "template_generation",
      taskStatus: "running",
      createdAt: "2026-08-01T10:00:00",
      startedAt: "2026-08-01T10:00:01",
      completedAt: null,
      errorCode: null,
      errorMessage: null,
      progressStage: "calling_tool",
      latestToolCall: {
        roundNo: 2,
        toolName: "search_candidate_exercises",
        toolDisplayName: null,
        status: "succeeded",
        createdAt: "2026-08-01T10:00:02"
      },
      requestSnapshot: {
        sceneType: "gym",
        goalType: "muscle_gain",
        cycleLength: 4,
        includeCardio: true,
        additionalRequirements: null
      },
      updatedAt: "2026-08-01T10:00:03",
      result: null
    });

    render(
      <MemoryRouter>
        <TemplateGenerationTaskPage />
      </MemoryRouter>
    );

    expect(
      await screen.findByText("search_candidate_exercises")
    ).toBeInTheDocument();
  });
});
