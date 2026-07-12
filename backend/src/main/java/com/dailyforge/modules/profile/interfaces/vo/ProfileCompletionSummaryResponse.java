package com.dailyforge.modules.profile.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Profile completion summary for AI readiness")
public record ProfileCompletionSummaryResponse(
        @Schema(description = "Whether required basic profile fields are ready", example = "true")
        boolean basicProfileReady,
        @Schema(description = "Whether current user has a usable weight record", example = "true")
        boolean hasWeightRecord,
        @Schema(description = "Current weight from snapshot", example = "76.50") BigDecimal currentWeightKg,
        @Schema(description = "Missing required basic profile fields") List<String> missingBasicProfileFields,
        @Schema(description = "Whether AI plan generation is ready", example = "true") boolean aiPlanReady,
        @Schema(description = "Missing fields for AI plan generation") List<String> aiPlanMissingFields,
        @Schema(description = "Whether AI nutrition generation is ready", example = "true") boolean aiNutritionReady,
        @Schema(description = "Missing fields for AI nutrition generation") List<String> aiNutritionMissingFields,
        @Schema(description = "Whether AI summary generation is ready", example = "true") boolean aiSummaryReady,
        @Schema(description = "Missing fields for AI summary generation") List<String> aiSummaryMissingFields) {
}
