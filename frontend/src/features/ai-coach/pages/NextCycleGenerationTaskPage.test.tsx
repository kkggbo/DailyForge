import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { NextCycleGenerationTaskPage } from "./NextCycleGenerationTaskPage";

const { getNextCycleGenerationTaskMock } = vi.hoisted(() => ({
  getNextCycleGenerationTaskMock: vi.fn()
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>(
    "react-router-dom"
  );
  return {
    ...actual,
    useParams: () => ({ taskId: "9201" })
  };
});

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    accessToken: "ai-token"
  })
}));

vi.mock("../api/ai-coach", () => ({
  getNextCycleGenerationTask: getNextCycleGenerationTaskMock
}));

describe("NextCycleGenerationTaskPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the succeeded next-cycle draft template result", async () => {
    getNextCycleGenerationTaskMock.mockResolvedValue({
      taskId: 9201,
      taskType: "next_cycle_generation",
      taskStatus: "succeeded",
      createdAt: "2026-08-01T10:00:00",
      startedAt: "2026-08-01T10:00:01",
      completedAt: "2026-08-01T10:00:05",
      errorCode: null,
      errorMessage: null,
      progressStage: "completed",
      latestToolCall: null,
      requestSnapshot: {
        sceneType: "gym",
        goalType: "muscle_gain",
        cycleLength: 4,
        includeCardio: true,
        additionalRequirements: "延续上轮并降低下肢量。",
        sourceCycleRunId: 1201,
        sourceSummaryTaskId: 88
      },
      updatedAt: "2026-08-01T10:00:05",
      result: {
        draftTemplate: {
          templateId: 601,
          templateName: "Next Cycle Plan",
          templateStatus: "draft",
          cycleLength: 4,
          days: []
        },
        generationRationale: {
          overallDesignSummary: "延续上轮结构并吸收总结建议。",
          dayRationales: [],
          keyExerciseRationales: [],
          intensityRationale: {
            basisType: "historical_performance",
            summary: "基于上轮实际表现推进。"
          },
          warnings: []
        }
      }
    });

    render(
      <MemoryRouter>
        <NextCycleGenerationTaskPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("Next Cycle Plan")).toBeInTheDocument();
    expect(screen.getByText("延续上轮并降低下肢量。")).toBeInTheDocument();
  });
});
