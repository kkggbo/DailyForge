import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { CycleSummaryPage } from "./CycleSummaryPage";
import type { AiCoachCapabilities } from "../types/ai-coach";

const navigateMock = vi.fn();
const {
  getAiCoachCapabilitiesMock,
  createCycleSummaryTaskMock
} = vi.hoisted(() => ({
  getAiCoachCapabilitiesMock: vi.fn(),
  createCycleSummaryTaskMock: vi.fn()
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
  createCycleSummaryTask: createCycleSummaryTaskMock
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
    ready: true,
    latestCompletedCycleRunId: 1201,
    latestCompletedAt: "2026-08-01T10:00:00",
    recommendedMissingFields: []
  }
};

function renderPage() {
  return render(
    <MemoryRouter>
      <CycleSummaryPage />
    </MemoryRouter>
  );
}

describe("CycleSummaryPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("crypto", {
      randomUUID: () => "summary-request-id"
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows an empty-state notice when there is no completed cycle", async () => {
    getAiCoachCapabilitiesMock.mockResolvedValue({
      ...readyCapabilities,
      cycleSummary: {
        ...readyCapabilities.cycleSummary,
        ready: false,
        latestCompletedCycleRunId: null,
        latestCompletedAt: null
      }
    } satisfies AiCoachCapabilities);

    renderPage();

    expect(await screen.findByText("暂时没有可分析的已完成循环")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "发起周期总结任务" })).not.toBeInTheDocument();
  });

  it("submits a cycle summary task and navigates to the task detail page", async () => {
    const user = userEvent.setup();
    getAiCoachCapabilitiesMock.mockResolvedValue(readyCapabilities);
    createCycleSummaryTaskMock.mockResolvedValue({
      taskId: 9101,
      taskType: "cycle_summary",
      taskStatus: "pending",
      createdAt: "2026-08-01T10:00:00",
      pollAfterSeconds: 2
    });

    renderPage();

    expect(await screen.findByRole("button", { name: "发起周期总结任务" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "发起周期总结任务" }));

    await waitFor(() => {
      expect(createCycleSummaryTaskMock).toHaveBeenCalledWith("ai-token", {
        clientRequestId: "summary-request-id",
        cycleRunId: 1201
      });
    });

    expect(navigateMock).toHaveBeenCalledWith(
      "/ai-coach/cycle-summary/tasks/9101",
      { replace: true }
    );
  });
});
