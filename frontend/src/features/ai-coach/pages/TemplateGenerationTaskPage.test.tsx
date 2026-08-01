import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { TemplateGenerationTaskPage } from "./TemplateGenerationTaskPage";

const { getTemplateGenerationTaskMock } = vi.hoisted(() => ({
  getTemplateGenerationTaskMock: vi.fn()
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
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

  it("renders the succeeded task result and draft edit entry", async () => {
    getTemplateGenerationTaskMock.mockResolvedValue({
      taskId: 9001,
      taskType: "template_generation",
      taskStatus: "succeeded",
      createdAt: "2026-08-01T10:00:00",
      startedAt: "2026-08-01T10:00:01",
      completedAt: "2026-08-01T10:00:05",
      errorCode: null,
      errorMessage: null,
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

    render(
      <MemoryRouter>
        <TemplateGenerationTaskPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("草稿模板预览")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "去编辑草稿" })).toHaveAttribute(
      "href",
      "/cycle-templates/501/edit"
    );
  });
});
