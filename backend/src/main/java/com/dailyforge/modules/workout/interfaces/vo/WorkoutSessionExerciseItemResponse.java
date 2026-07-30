package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "One planned workout exercise item with actual values")
public record WorkoutSessionExerciseItemResponse(
        @Schema(description = "Item index", example = "1") Integer itemIndex,
        @Schema(description = "Item type", example = "set", allowableValues = {"set", "segment"}) String itemType,
        @Schema(description = "Item display name", example = "Set 1", nullable = true) String itemName,
        @Schema(description = "Item note from the plan snapshot", example = "Working set", nullable = true) String note,
        @Schema(description = "Metrics in the item") List<WorkoutSessionExerciseMetricResponse> metrics) {
}
