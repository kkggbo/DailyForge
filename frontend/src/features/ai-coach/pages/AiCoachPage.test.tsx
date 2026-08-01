import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { AiCoachPage } from "./AiCoachPage";
import type { AiCoachCapabilities } from "../types/ai-coach";

const { getAiCoachCapabilitiesMock } = vi.hoisted(() => ({
  getAiCoachCapabilitiesMock: vi.fn()
}));

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    accessToken: "ai-token"
  })
}));

vi.mock("../api/ai-coach", () => ({
  getAiCoachCapabilities: getAiCoachCapabilitiesMock
}));

function renderPage() {
  return render(
    <MemoryRouter>
      <AiCoachPage />
    </MemoryRouter>
  );
}

const baseCapabilities: AiCoachCapabilities = {
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

describe("AiCoachPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders capability cards after loading capabilities", async () => {
    getAiCoachCapabilitiesMock.mockResolvedValue(baseCapabilities);

    renderPage();

    expect(await screen.findByRole("heading", { name: "AI 生成训练模板" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "AI 周期总结" })).toBeInTheDocument();
    expect(screen.getByText("invited_ai")).toBeInTheDocument();
    expect(screen.getByText(/最近循环 ID #1201/)).toBeInTheDocument();
  });

  it("shows unavailable state and missing-field guidance when AI is not fully ready", async () => {
    getAiCoachCapabilitiesMock.mockResolvedValue({
      ...baseCapabilities,
      aiEnabled: false,
      templateGeneration: {
        ...baseCapabilities.templateGeneration,
        ready: false,
        missingRequiredFields: ["currentWeightKg"]
      },
      cycleSummary: {
        ...baseCapabilities.cycleSummary,
        recommendedMissingFields: ["currentWeightKg"]
      }
    } satisfies AiCoachCapabilities);

    renderPage();

    expect(await screen.findByText("当前账号暂未开通 AI Coach")).toBeInTheDocument();
    expect(screen.getAllByText("当前体重").length).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: "去补充资料" })).toHaveAttribute(
      "href",
      expect.stringContaining("/profile/ai-completion")
    );
  });
});
