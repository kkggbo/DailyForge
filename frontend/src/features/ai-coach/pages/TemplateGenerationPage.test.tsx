import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { TemplateGenerationPage } from "./TemplateGenerationPage";
import type { AiCoachCapabilities } from "../types/ai-coach";

const navigateMock = vi.fn();
const {
  getAiCoachCapabilitiesMock,
  createTemplateGenerationTaskMock
} = vi.hoisted(() => ({
  getAiCoachCapabilitiesMock: vi.fn(),
  createTemplateGenerationTaskMock: vi.fn()
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
  return {
    ...actual,
    useNavigate: () => navigateMock
  };
});

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    accessToken: "ai-token"
  })
}));

vi.mock("../api/ai-coach", () => ({
  getAiCoachCapabilities: getAiCoachCapabilitiesMock,
  createTemplateGenerationTask: createTemplateGenerationTaskMock
}));

const readyCapabilities: AiCoachCapabilities = {
  aiEnabled: true,
  accountTier: "invited_ai",
  platformRole: "user",
  templateGeneration: {
    available: true,
    ready: true,
    missingRequiredFields: [],
    allowedSceneTypes: ["gym", "home"],
    allowedGoalTypes: ["muscle_gain", "fat_loss", "health_maintenance"],
    minCycleLength: 1,
    maxCycleLength: 7
  },
  cycleSummary: {
    available: true,
    ready: false,
    latestCompletedCycleRunId: null,
    latestCompletedAt: null,
    recommendedMissingFields: []
  }
};

function renderPage() {
  return render(
    <MemoryRouter>
      <TemplateGenerationPage />
    </MemoryRouter>
  );
}

describe("TemplateGenerationPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("crypto", {
      randomUUID: () => "template-request-id"
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows missing-field guidance when template generation is not ready", async () => {
    getAiCoachCapabilitiesMock.mockResolvedValue({
      ...readyCapabilities,
      templateGeneration: {
        ...readyCapabilities.templateGeneration,
        ready: false,
        missingRequiredFields: ["currentWeightKg"]
      }
    } satisfies AiCoachCapabilities);

    renderPage();

    expect(await screen.findByText("当前体重")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "生成草稿任务" })).not.toBeInTheDocument();
  });

  it("submits a generation task and navigates to the task detail page", async () => {
    const user = userEvent.setup();
    getAiCoachCapabilitiesMock.mockResolvedValue(readyCapabilities);
    createTemplateGenerationTaskMock.mockResolvedValue({
      taskId: 9001,
      taskType: "template_generation",
      taskStatus: "pending",
      createdAt: "2026-08-01T10:00:00",
      pollAfterSeconds: 2
    });

    renderPage();

    expect(await screen.findByRole("button", { name: "生成草稿任务" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "生成草稿任务" }));

    await waitFor(() => {
      expect(createTemplateGenerationTaskMock).toHaveBeenCalledWith(
        "ai-token",
        expect.objectContaining({
          clientRequestId: "template-request-id",
          sceneType: "gym",
          goalType: "muscle_gain",
          cycleLength: 4,
          includeCardio: true
        })
      );
    });

    expect(navigateMock).toHaveBeenCalledWith(
      "/ai-coach/template-generation/tasks/9001",
      { replace: true }
    );
  });
});
