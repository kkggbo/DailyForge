package com.dailyforge.modules.aicoach.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "AI coach capabilities response")
public record AiCoachCapabilitiesResponse(
        @Schema(description = "Whether AI is enabled for current account", example = "true") boolean aiEnabled,
        @Schema(description = "Current account tier", example = "invited_ai") String accountTier,
        @Schema(description = "Current platform role", example = "user") String platformRole,
        @Schema(description = "Template generation capability") TemplateGenerationCapability templateGeneration,
        @Schema(description = "Cycle summary capability") CycleSummaryCapability cycleSummary,
        @Schema(description = "Next cycle generation capability") NextCycleGenerationCapability nextCycleGeneration) {

    @Schema(description = "Template generation capability")
    public record TemplateGenerationCapability(
            @Schema(description = "Whether template generation is available", example = "true") boolean available,
            @Schema(description = "Whether required data is ready", example = "true") boolean ready,
            @Schema(description = "Missing required fields") List<String> missingRequiredFields,
            @Schema(description = "Allowed scene types") List<String> allowedSceneTypes,
            @Schema(description = "Allowed goal types") List<String> allowedGoalTypes,
            @Schema(description = "Min cycle length", example = "1") int minCycleLength,
            @Schema(description = "Max cycle length", example = "7") int maxCycleLength) {
    }

    @Schema(description = "Cycle summary capability")
    public record CycleSummaryCapability(
            @Schema(description = "Whether cycle summary is available", example = "true") boolean available,
            @Schema(description = "Whether one completed cycle is ready", example = "false") boolean ready,
            @Schema(description = "Latest completed cycle run id", example = "1201") Long latestCompletedCycleRunId,
            @Schema(description = "Latest completed time", example = "2026-07-31T09:20:10")
            LocalDateTime latestCompletedAt,
            @Schema(description = "Recommended profile fields for better result")
            List<String> recommendedMissingFields) {
    }

    @Schema(description = "Next cycle generation capability")
    public record NextCycleGenerationCapability(
            @Schema(description = "Whether next-cycle generation is available", example = "true") boolean available,
            @Schema(description = "Whether a completed cycle run with a succeeded summary is ready",
                    example = "false") boolean ready,
            @Schema(description = "Latest completed cycle run id", example = "1201") Long latestCompletedCycleRunId,
            @Schema(description = "Latest completed time", example = "2026-07-31T09:20:10")
            LocalDateTime latestCompletedAt,
            @Schema(description = "Reason the capability is not ready",
                    example = "no_cycle_summary") String missingReason) {
    }
}