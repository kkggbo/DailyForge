import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { ApiRequestError } from "../../../shared/api/http";
import { WorkoutPage } from "./WorkoutPage";
import type { Workspace } from "../types/workout";

const navigateMock = vi.fn();
const {
  getWorkspaceMock,
  initializeCurrentDayMock,
  getRecentMock,
  getLatestCycleSummaryTaskByCycleRunMock
} = vi.hoisted(() => ({
  getWorkspaceMock: vi.fn(),
  initializeCurrentDayMock: vi.fn(),
  getRecentMock: vi.fn(),
  getLatestCycleSummaryTaskByCycleRunMock: vi.fn()
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>(
    "react-router-dom"
  );
  return {
    ...actual,
    useNavigate: () => navigateMock
  };
});

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    accessToken: "workout-test-token"
  })
}));

vi.mock("../../ai-coach/api/ai-coach", () => ({
  getLatestCycleSummaryTaskByCycleRun: getLatestCycleSummaryTaskByCycleRunMock
}));

vi.mock("../api/workout", () => ({
  getWorkspace: getWorkspaceMock,
  initializeCurrentDay: initializeCurrentDayMock,
  getDay: vi.fn(),
  saveSession: vi.fn(),
  completeSession: vi.fn(),
  getRecent: getRecentMock,
  restartCycle: vi.fn(),
  requestAiAnalysis: vi.fn()
}));

function renderPage() {
  return render(
    <MemoryRouter>
      <WorkoutPage />
    </MemoryRouter>
  );
}

describe("WorkoutPage cycle summary entry", () => {
  beforeEach(() => {
    navigateMock.mockReset();
    getWorkspaceMock.mockResolvedValue({
      workspaceState: "cycle_completed",
      templateId: 1,
      templateName: "Push Pull Legs",
      cycleRunId: 11,
      runNo: 1,
      cycleLength: 2,
      currentDayIndex: null,
      defaultDayIndex: null,
      days: []
    } satisfies Workspace);
    initializeCurrentDayMock.mockResolvedValue({
      sessionCreated: true,
      day: null
    });
    getRecentMock.mockResolvedValue({
      page: 1,
      pageSize: 10,
      total: 0,
      records: []
    });
    getLatestCycleSummaryTaskByCycleRunMock.mockResolvedValue({
      taskId: 9101
    });
  });

  it("directly opens the latest summary when the current cycle already has one", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(
      await screen.findByRole("heading", { name: "这一轮训练已完成" })
    ).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "AI 周期总结" }));

    await waitFor(() => {
      expect(getLatestCycleSummaryTaskByCycleRunMock).toHaveBeenCalledWith(
        "workout-test-token",
        11
      );
    });
    expect(navigateMock).toHaveBeenCalledWith(
      "/ai-coach/cycle-summary/tasks/9101"
    );
  });

  it("falls back to the cycle summary creation page when no history summary exists", async () => {
    const user = userEvent.setup();
    getLatestCycleSummaryTaskByCycleRunMock.mockRejectedValue(
      new ApiRequestError("summary not found", {
        code: "AI_TASK_NOT_FOUND",
        status: 404
      })
    );

    renderPage();

    expect(
      await screen.findByRole("heading", { name: "这一轮训练已完成" })
    ).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "AI 周期总结" }));

    await waitFor(() => {
      expect(getLatestCycleSummaryTaskByCycleRunMock).toHaveBeenCalledWith(
        "workout-test-token",
        11
      );
    });
    expect(navigateMock).toHaveBeenCalledWith("/ai-coach/cycle-summary");
  });
});
