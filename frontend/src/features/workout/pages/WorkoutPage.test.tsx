import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { WorkoutPage } from "./WorkoutPage";
import type { DayDetail, RecentWorkouts, Workspace } from "../types/workout";

const {
  getWorkspaceMock,
  initializeCurrentDayMock,
  getDayMock,
  saveSessionMock,
  completeSessionMock,
  getRecentMock,
  restartCycleMock,
  requestAiAnalysisMock
} = vi.hoisted(() => ({
  getWorkspaceMock: vi.fn(),
  initializeCurrentDayMock: vi.fn(),
  getDayMock: vi.fn(),
  saveSessionMock: vi.fn(),
  completeSessionMock: vi.fn(),
  getRecentMock: vi.fn(),
  restartCycleMock: vi.fn(),
  requestAiAnalysisMock: vi.fn()
}));

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    accessToken: "workout-test-token"
  })
}));

vi.mock("../api/workout", () => ({
  getWorkspace: getWorkspaceMock,
  initializeCurrentDay: initializeCurrentDayMock,
  getDay: getDayMock,
  saveSession: saveSessionMock,
  completeSession: completeSessionMock,
  getRecent: getRecentMock,
  restartCycle: restartCycleMock,
  requestAiAnalysis: requestAiAnalysisMock
}));

const recent: RecentWorkouts = {
  page: 1,
  pageSize: 10,
  total: 0,
  records: []
};

const activeWorkspace: Workspace = {
  workspaceState: "active",
  templateId: 1,
  templateName: "Push Pull Legs",
  cycleRunId: 11,
  runNo: 1,
  cycleLength: 2,
  currentDayIndex: 1,
  defaultDayIndex: 1,
  days: [
    {
      dayIndex: 1,
      dayName: "Push",
      isRestDay: false,
      dayState: "current",
      sessionId: null,
      sessionStatus: null
    },
    {
      dayIndex: 2,
      dayName: "Cardio",
      isRestDay: false,
      dayState: "upcoming",
      sessionId: null,
      sessionStatus: null
    }
  ]
};

const editableDay: DayDetail = {
  cycleRunId: 11,
  runNo: 1,
  templateId: 1,
  templateName: "Push Pull Legs",
  dayIndex: 1,
  dayName: "Push",
  isRestDay: false,
  dayState: "current",
  viewMode: "editable",
  canInitializeSession: true,
  session: {
    sessionId: 101,
    sessionType: "workout",
    sessionStatus: "in_progress",
    startedAt: "2026-07-29T20:00:00",
    completedAt: null,
    notes: null,
    exercises: [
      {
        sessionExerciseId: 201,
        sortOrder: 1,
        exerciseId: 301,
        exerciseName: "Bench Press",
        structureType: "set_based",
        exerciseStatus: null,
        failureReason: null,
        feedback: null,
        items: [
          {
            itemIndex: 1,
            itemType: "set",
            itemName: "第1组",
            note: null,
            metrics: [
              {
                sortOrder: 1,
                metricKey: "weight_kg",
                metricUnit: "kg",
                plannedValueNumber: 60,
                actualValueNumber: null
              }
            ]
          }
        ]
      }
    ]
  }
};

const upcomingDay: DayDetail = {
  ...editableDay,
  dayIndex: 2,
  dayName: "Cardio",
  dayState: "upcoming",
  viewMode: "preview",
  canInitializeSession: false,
  session: null,
  exercises: [
    {
      sessionExerciseId: null,
      sortOrder: 1,
      exerciseId: 302,
      exerciseName: "Treadmill Running",
      structureType: "single_segment",
      exerciseStatus: null,
      failureReason: null,
        feedback: null,
      items: [
        {
          itemIndex: 1,
          itemType: "segment",
          itemName: "主训练段",
          note: null,
          metrics: [
            {
              sortOrder: 1,
              metricKey: "duration_seconds",
              metricUnit: "sec",
              plannedValueNumber: 1200,
              actualValueNumber: null
            }
          ]
        }
      ]
    }
  ]
};

const completedDay: DayDetail = {
  ...editableDay,
  dayState: "completed",
  viewMode: "readonly",
  canInitializeSession: false,
  session: {
    ...editableDay.session!,
    sessionStatus: "completed",
    completedAt: "2026-07-29T21:00:00",
    exercises: [
      {
        ...editableDay.session!.exercises[0]!,
        exerciseStatus: "completed",
        items: editableDay.session!.exercises[0]!.items.map((item) => ({
          ...item,
          metrics: item.metrics.map((metric) => ({
            ...metric,
            actualValueNumber: 60
          }))
        }))
      }
    ]
  }
};

function renderPage() {
  return render(
    <MemoryRouter>
      <WorkoutPage />
    </MemoryRouter>
  );
}

describe("WorkoutPage", () => {
  beforeEach(() => {
    getWorkspaceMock.mockResolvedValue(activeWorkspace);
    initializeCurrentDayMock.mockResolvedValue({
      sessionCreated: true,
      day: editableDay
    });
    getDayMock.mockResolvedValue(upcomingDay);
    saveSessionMock.mockResolvedValue({
      sessionId: 101,
      sessionStatus: "in_progress",
      savedAt: "2026-07-29T20:30:00"
    });
    completeSessionMock.mockResolvedValue({
      sessionId: 101,
      sessionStatus: "completed",
      completedAt: "2026-07-29T21:00:00",
      completedDayIndex: 1,
      cycleRunId: 11,
      cycleRunStatus: "active",
      nextCurrentDayIndex: 2,
      completedDay
    });
    getRecentMock.mockResolvedValue(recent);
    restartCycleMock.mockResolvedValue({
      templateId: 1,
      templateName: "Push Pull Legs",
      cycleRunId: 12,
      runNo: 2,
      cycleRunStatus: "active",
      currentDayIndex: 1
    });
    requestAiAnalysisMock.mockResolvedValue(undefined);
  });

  it("automatically initializes the default current day after loading an active workspace", async () => {
    renderPage();

    expect(await screen.findByRole("heading", { name: "Bench Press" })).toBeInTheDocument();
    expect(getWorkspaceMock).toHaveBeenCalledWith("workout-test-token");
    expect(initializeCurrentDayMock).toHaveBeenCalledWith("workout-test-token");
  });

  it("browses a future day without initializing another session", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole("heading", { name: "Bench Press" });
    await user.click(screen.getByRole("button", { name: /Day 2.*Cardio/i }));

    expect(await screen.findByText("未来训练计划预览，不会创建 session。")).toBeInTheDocument();
    expect(getDayMock).toHaveBeenCalledWith("workout-test-token", 2);
    expect(initializeCurrentDayMock).toHaveBeenCalledTimes(1);
  });

  it("saves the full editable session payload", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole("heading", { name: "Bench Press" });
    await user.selectOptions(screen.getByLabelText("Bench Press 完成状态"), "completed");
    await user.click(screen.getByRole("button", { name: "记录实际值" }));
    await user.type(screen.getByPlaceholderText("实际值"), "60");
    await user.click(screen.getByRole("button", { name: "添加感受/备注" }));
    await user.type(screen.getByLabelText("感受/备注"), "三头提前疲劳");
    await user.type(screen.getByLabelText("训练备注"), "下轮降低热身量");
    await user.click(screen.getByRole("button", { name: "手动保存" }));

    await waitFor(() => {
      expect(saveSessionMock).toHaveBeenCalledWith(
        "workout-test-token",
        101,
        expect.objectContaining({
          notes: "下轮降低热身量",
          exercises: [
            expect.objectContaining({
              sessionExerciseId: 201,
              exerciseStatus: "completed",
              feedback: "三头提前疲劳",
              items: [
                {
                  itemIndex: 1,
                  metrics: [
                    {
                      metricKey: "weight_kg",
                      actualValueNumber: 60
                    }
                  ]
                }
              ]
            })
          ]
        })
      );
    });
  });

  it("repopulates the merged inputs from feedback and notes", async () => {
    const user = userEvent.setup();
    initializeCurrentDayMock.mockResolvedValueOnce({
      sessionCreated: false,
      day: {
        ...editableDay,
        session: {
          ...editableDay.session!,
          notes: "训练整体备注",
          exercises: editableDay.session!.exercises.map((exercise) => ({
            ...exercise,
            feedback: "动作反馈"
          }))
        }
      }
    });
    renderPage();

    await screen.findByRole("heading", { name: "Bench Press" });
    expect(screen.getByLabelText("训练备注")).toHaveValue("训练整体备注");
    expect(screen.getByRole("button", { name: "收起感受/备注" })).toBeInTheDocument();
    expect(screen.getByLabelText("感受/备注")).toHaveValue("动作反馈");
    await user.click(screen.getByRole("button", { name: "收起感受/备注" }));
  });
  it("keeps actual metric inputs collapsed by default and saves null actual values", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole("heading", { name: "Bench Press" });
    expect(screen.queryByPlaceholderText("实际值")).not.toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("Bench Press 完成状态"), "completed");
    await user.click(screen.getByRole("button", { name: "手动保存" }));

    await waitFor(() => {
      expect(saveSessionMock).toHaveBeenCalledWith(
        "workout-test-token",
        101,
        expect.objectContaining({
          exercises: [
            expect.objectContaining({
              exerciseStatus: "completed",
              items: [
                {
                  itemIndex: 1,
                  metrics: [
                    {
                      metricKey: "weight_kg",
                      actualValueNumber: null
                    }
                  ]
                }
              ]
            })
          ]
        })
      );
    });
  });

  it("blocks saving invalid actual metric values", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole("heading", { name: "Bench Press" });
    await user.click(screen.getByRole("button", { name: "记录实际值" }));
    await user.type(screen.getByPlaceholderText("实际值"), "-10");
    await user.click(screen.getByRole("button", { name: "手动保存" }));

    expect(await screen.findByText("重量：请输入大于或等于 0 的数字。")).toBeInTheDocument();
    expect(saveSessionMock).not.toHaveBeenCalled();
  });

  it("shows failure reason only for non-completed exercise states", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole("heading", { name: "Bench Press" });
    expect(screen.queryByText("失败 / 跳过原因")).not.toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("Bench Press 完成状态"), "skipped");
    expect(screen.getByText("失败 / 跳过原因")).toBeInTheDocument();

    const [statusSelect, failureReasonSelect] = screen.getAllByRole("combobox");
    expect(statusSelect.compareDocumentPosition(failureReasonSelect) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();

    await user.selectOptions(screen.getByLabelText("Bench Press 完成状态"), "completed");
    expect(screen.queryByText("失败 / 跳过原因")).not.toBeInTheDocument();
  });
  it("falls back to planned values when completed records have no explicit actual values", async () => {
    const user = userEvent.setup();
    completeSessionMock.mockResolvedValueOnce({
      sessionId: 101,
      sessionStatus: "completed",
      completedAt: "2026-07-29T21:00:00",
      completedDayIndex: 1,
      cycleRunId: 11,
      cycleRunStatus: "active",
      nextCurrentDayIndex: 2,
      completedDay: {
        ...completedDay,
        session: {
          ...completedDay.session!,
          exercises: completedDay.session!.exercises.map((exercise) => ({
            ...exercise,
            items: exercise.items.map((item) => ({
              ...item,
              metrics: item.metrics.map((metric) => ({
                ...metric,
                actualValueNumber: null
              }))
            }))
          }))
        }
      }
    });
    renderPage();

    await screen.findByRole("heading", { name: "Bench Press" });
    await user.selectOptions(screen.getByRole("combobox"), "completed");
    await user.click(screen.getByRole("button", { name: /打卡/ }));

    expect(await screen.findByText("实际：60 kg")).toBeInTheDocument();
    expect(screen.queryByText("实际：未填写")).not.toBeInTheDocument();
  });
  it("keeps the completed day visible instead of automatically moving to the next day", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole("heading", { name: "Bench Press" });
    await user.selectOptions(screen.getByLabelText("Bench Press 完成状态"), "completed");
    await user.click(screen.getByRole("button", { name: "完成训练打卡" }));

    expect(await screen.findByText("本 Day 已完成。当前页面保留在这条只读记录上。")).toBeInTheDocument();
    expect(screen.getByText("已完成记录仅供查看，不能编辑。")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "完成训练打卡" })).not.toBeInTheDocument();
    expect(getDayMock).not.toHaveBeenCalled();
  });

  it("disables Day navigation until a pending completion request finishes", async () => {
    let resolveCompletion: ((value: {
      sessionId: number;
      sessionStatus: "completed";
      completedAt: string;
      completedDayIndex: number;
      cycleRunId: number;
      cycleRunStatus: "active" | "completed" | "cancelled";
      nextCurrentDayIndex: number | null;
      completedDay: DayDetail;
    }) => void) | undefined;
    const pendingCompletion = new Promise<{
      sessionId: number;
      sessionStatus: "completed";
      completedAt: string;
      completedDayIndex: number;
      cycleRunId: number;
      cycleRunStatus: "active" | "completed" | "cancelled";
      nextCurrentDayIndex: number | null;
      completedDay: DayDetail;
    }>((resolve) => {
      resolveCompletion = resolve;
    });
    completeSessionMock.mockReturnValueOnce(pendingCompletion);

    const user = userEvent.setup();
    renderPage();

    await screen.findByRole("heading", { name: "Bench Press" });
    await user.selectOptions(screen.getAllByRole("combobox")[0]!, "completed");
    await user.click(screen.getByRole("button", { name: "完成训练打卡" }));

    const nextDayButton = screen.getByRole("button", { name: /Day 2.*Cardio/i });
    expect(nextDayButton).toBeDisabled();
    await user.click(nextDayButton);
    expect(getDayMock).not.toHaveBeenCalled();

    resolveCompletion?.({
      sessionId: 101,
      sessionStatus: "completed",
      completedAt: "2026-07-29T21:00:00",
      completedDayIndex: 1,
      cycleRunId: 11,
      cycleRunStatus: "active",
      nextCurrentDayIndex: 2,
      completedDay
    });

    await waitFor(() => expect(nextDayButton).not.toBeDisabled());
    expect(screen.queryByRole("button", { name: /完成训练打卡/ })).not.toBeInTheDocument();
  });
  it("allows a rest-day session to complete with an empty exercise payload", async () => {
    const restDay: DayDetail = {
      ...editableDay,
      isRestDay: true,
      dayName: "Rest",
      session: {
        ...editableDay.session!,
        sessionType: "rest_day",
        exercises: []
      }
    };
    initializeCurrentDayMock.mockResolvedValueOnce({
      sessionCreated: true,
      day: restDay
    });
    completeSessionMock.mockResolvedValueOnce({
      sessionId: 101,
      sessionStatus: "completed",
      completedAt: "2026-07-29T21:00:00",
      completedDayIndex: 1,
      cycleRunId: 11,
      cycleRunStatus: "active",
      nextCurrentDayIndex: 2,
      completedDay: {
        ...restDay,
        dayState: "completed",
        viewMode: "readonly",
        canInitializeSession: false,
        session: {
          ...restDay.session!,
          sessionStatus: "completed"
        }
      }
    });
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByRole("button", { name: "完成休息日打卡" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "完成休息日打卡" }));

    await waitFor(() => {
      expect(completeSessionMock).toHaveBeenCalledWith(
        "workout-test-token",
        101,
        expect.objectContaining({ exercises: [] })
      );
    });
  });

  it("renders no-template and cycle-completed workspace states", async () => {
    getWorkspaceMock.mockResolvedValueOnce({
      workspaceState: "no_active_template",
      templateId: null,
      templateName: null,
      cycleRunId: null,
      runNo: null,
      cycleLength: null,
      currentDayIndex: null,
      defaultDayIndex: null,
      days: []
    } satisfies Workspace);
    const { unmount } = renderPage();

    expect(await screen.findByText("先启用一个训练模板")).toBeInTheDocument();
    expect(initializeCurrentDayMock).not.toHaveBeenCalled();
    unmount();

    getWorkspaceMock.mockResolvedValueOnce({
      ...activeWorkspace,
      workspaceState: "cycle_completed",
      currentDayIndex: null,
      defaultDayIndex: null,
      days: []
    } satisfies Workspace);
    renderPage();

    expect(await screen.findByText("这一轮训练已完成")).toBeInTheDocument();
  });

  it("renders an actionable error state when the workspace cannot load", async () => {
    getWorkspaceMock.mockRejectedValueOnce(new Error("network unavailable"));

    renderPage();

    expect(await screen.findByText("network unavailable")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "重新加载" })).toBeInTheDocument();
    expect(initializeCurrentDayMock).not.toHaveBeenCalled();
  });
});




