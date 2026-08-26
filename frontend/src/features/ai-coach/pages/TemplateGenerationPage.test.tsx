import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { TemplateGenerationPage } from "./TemplateGenerationPage";
import type { AiCoachCapabilities } from "../types/ai-coach";

const navigateMock = vi.fn();
const {
  getAiCoachCapabilitiesMock,
  createTemplateGenerationTaskMock,
  getBasicProfileMock,
  getProfileCompletionSummaryMock,
  updateBasicProfileMock,
  createBodyMetricMock
} = vi.hoisted(() => ({
  getAiCoachCapabilitiesMock: vi.fn(),
  createTemplateGenerationTaskMock: vi.fn(),
  getBasicProfileMock: vi.fn(),
  getProfileCompletionSummaryMock: vi.fn(),
  updateBasicProfileMock: vi.fn(),
  createBodyMetricMock: vi.fn()
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

vi.mock("../../profile/api/profile", () => ({
  getBasicProfile: getBasicProfileMock,
  getProfileCompletionSummary: getProfileCompletionSummaryMock,
  updateBasicProfile: updateBasicProfileMock,
  createBodyMetric: createBodyMetricMock
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
  },
  nextCycleGeneration: {
    available: true,
    ready: false,
    latestCompletedCycleRunId: null,
    latestCompletedAt: null,
    missingReason: null
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

  it("opens an in-page completion modal instead of navigating when clicking 去补充资料", async () => {
    const user = userEvent.setup();
    getAiCoachCapabilitiesMock.mockResolvedValue({
      ...readyCapabilities,
      templateGeneration: {
        ...readyCapabilities.templateGeneration,
        ready: false,
        missingRequiredFields: ["gender"]
      }
    } satisfies AiCoachCapabilities);
    getBasicProfileMock.mockResolvedValue({});
    getProfileCompletionSummaryMock.mockResolvedValue({
      basicProfileReady: false,
      hasWeightRecord: false,
      currentWeightKg: null,
      missingBasicProfileFields: ["gender"],
      aiPlanReady: false,
      aiPlanMissingFields: ["gender"],
      aiNutritionReady: false,
      aiNutritionMissingFields: [],
      aiSummaryReady: false,
      aiSummaryMissingFields: []
    });

    renderPage();

    expect(await screen.findByRole("button", { name: "去补充资料" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "去补充资料" }));

    expect(
      await screen.findByText("补充资料以生成更贴合的训练计划")
    ).toBeInTheDocument();
    expect(screen.getByText("第 1 步：基础档案")).toBeInTheDocument();
    expect(navigateMock).not.toHaveBeenCalled();
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
