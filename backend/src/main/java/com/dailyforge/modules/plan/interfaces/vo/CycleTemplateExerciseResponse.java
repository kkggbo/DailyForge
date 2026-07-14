package com.dailyforge.modules.plan.interfaces.vo;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Cycle template exercise response")
public record CycleTemplateExerciseResponse(
        @Schema(description = "Exercise order", example = "1") Integer sortOrder,
        @Schema(description = "Exercise id", example = "1001") Long exerciseId,
        @Schema(description = "Exercise name", example = "Barbell Bench Press") String exerciseName,
        @Schema(description = "Target sets", example = "4") Integer targetSets,
        @Schema(description = "Target min reps", example = "6") Integer targetRepsMin,
        @Schema(description = "Target max reps", example = "8") Integer targetRepsMax,
        @Schema(description = "Target weight in kilograms", example = "60.00") BigDecimal targetWeightKg,
        @Schema(description = "Target duration in seconds", example = "1800") Integer targetDurationSeconds,
        @Schema(description = "Rest seconds", example = "180") Integer restSeconds,
        @Schema(description = "Target RPE", example = "8.0") BigDecimal targetRpe,
        @Schema(description = "Exercise note", example = "Last set close to failure") String note,
        @Schema(description = "Extra target config JSON", example = "{\"pace\":\"5:30\",\"incline\":6}")
        JsonNode targetExtraJson) {
}
