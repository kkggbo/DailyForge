package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Planned and actual values for one workout item metric")
public record WorkoutSessionExerciseMetricResponse(
        @Schema(description = "Metric display order", example = "1") Integer sortOrder,
        @Schema(description = "Metric key", example = "weight_kg") String metricKey,
        @Schema(description = "Metric unit derived from the metric key", example = "kg") String metricUnit,
        @Schema(description = "Planned numeric value", example = "60", nullable = true) BigDecimal plannedValueNumber,
        @Schema(description = "Actual numeric value", example = "50", nullable = true) BigDecimal actualValueNumber) {
}
