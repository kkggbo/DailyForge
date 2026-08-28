import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { StatsPage } from "./StatsPage";

const {
  getStatsSummaryMock,
  getExerciseProgressionMock,
  getBodyMetricsMock
} = vi.hoisted(() => ({
  getStatsSummaryMock: vi.fn(),
  getExerciseProgressionMock: vi.fn(),
  getBodyMetricsMock: vi.fn()
}));

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    accessToken: "stats-token"
  })
}));

vi.mock("../api/stats", () => ({
  getStatsSummary: getStatsSummaryMock,
  getExerciseProgression: getExerciseProgressionMock,
  getBodyMetrics: getBodyMetricsMock
}));

describe("StatsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();

    getStatsSummaryMock.mockResolvedValue({
      overall: {
        sessionCount: 12,
        totalSets: 200,
        totalReps: 1500,
        totalVolumeKg: 12345.5,
        totalDistanceKm: 88.5,
        totalDurationMinutes: 720,
        overviewCopy: "你累计训练 12 场、总容量 12345.5kg。"
      },
      exercises: [
        {
          exerciseId: 1001,
          name: "卧推",
          exerciseType: "strength",
          structureType: "set_based",
          appearanceCount: 5,
          setCount: 20,
          repCount: 150,
          totalVolumeKg: 8000.5,
          avgWeightKg: 60.2,
          maxWeightKg: 80.0,
          avgReps: 7.5,
          totalDurationSeconds: null,
          totalDistanceKm: null,
          avgSpeedKmh: null,
          funCopy: "你已经卧推 150 次。"
        }
      ]
    });

    getBodyMetricsMock.mockResolvedValue({
      metric: "weight_kg",
      unit: "kg",
      points: []
    });
  });

  it("renders the summary hero and the exercise stat card", async () => {
    render(
      <MemoryRouter>
        <StatsPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("训练统计")).toBeInTheDocument();
    expect(screen.getByText("你累计训练 12 场、总容量 12345.5kg。")).toBeInTheDocument();
    expect(screen.getByText("卧推")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "查看进阶" })).toBeInTheDocument();
    expect(getStatsSummaryMock).toHaveBeenCalledWith("stats-token", {});
  });
});
