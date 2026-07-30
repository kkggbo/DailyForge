package com.dailyforge.modules.workout.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Editable actual value for one metric in an exercise item")
public record WorkoutSessionExerciseMetricSaveRequest(
        @Schema(description = "Metric key from the item snapshot", example = "weight_kg",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"weight_kg", "reps", "duration_seconds", "distance_km", "speed_kmh",
                        "pace_seconds_per_km", "incline_percent", "rest_seconds", "rpe", "intensity_level"})
        @NotBlank @Size(max = 64) String metricKey,

        @Schema(description = "Actual numeric value. Null clears or leaves the value unfilled.", example = "50",
                nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        @PositiveOrZero @Digits(integer = 8, fraction = 2)
        BigDecimal actualValueNumber) {
}

