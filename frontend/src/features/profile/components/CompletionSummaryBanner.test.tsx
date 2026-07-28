import { render, screen } from "@testing-library/react";
import { CompletionSummaryBanner } from "./CompletionSummaryBanner";
import { getProfileFieldLabel } from "../lib/profile-enums";

describe("CompletionSummaryBanner", () => {
  it("shows readiness status and missing fields for AI-related prompts", () => {
    render(
      <CompletionSummaryBanner
        summary={{
          basicProfileReady: false,
          hasWeightRecord: true,
          currentWeightKg: 72.3,
          missingBasicProfileFields: ["goalType", "trainingLevel"],
          aiPlanReady: false,
          aiPlanMissingFields: ["goalType", "heightCm"],
          aiNutritionReady: true,
          aiNutritionMissingFields: [],
          aiSummaryReady: false,
          aiSummaryMissingFields: ["trainingLevel"]
        }}
      />
    );

    expect(screen.getByText("Profile Readiness")).toBeInTheDocument();
    expect(screen.getByText("72.30 kg")).toBeInTheDocument();
    expect(screen.getAllByText(getProfileFieldLabel("goalType"))).toHaveLength(2);
    expect(screen.getByText(getProfileFieldLabel("trainingLevel"))).toBeInTheDocument();
    expect(screen.getByText(getProfileFieldLabel("heightCm"))).toBeInTheDocument();
  });
});
