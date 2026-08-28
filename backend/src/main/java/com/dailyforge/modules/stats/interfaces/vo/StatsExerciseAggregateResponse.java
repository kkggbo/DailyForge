package com.dailyforge.modules.stats.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Single exercise aggregate statistics")
public record StatsExerciseAggregateResponse(
        @Schema(description = "Exercise id", example = "1001") Long exerciseId,
        @Schema(description = "Exercise name", example = "卧推") String name,
        @Schema(description = "Exercise type", example = "strength") String exerciseType,
        @Schema(description = "Structure type", example = "set_based") String structureType,
        @Schema(description = "Appearance count (distinct sessions with actual data)", example = "5")
        int appearanceCount,
        @Schema(description = "Total sets with actual values (strength only)", example = "20")
        Integer setCount,
        @Schema(description = "Total reps (strength only)", example = "150") Integer repCount,
        @Schema(description = "Total volume in kg (strength only)", example = "8000.5")
        BigDecimal totalVolumeKg,
        @Schema(description = "Average weight in kg (strength only)", example = "60.2")
        BigDecimal avgWeightKg,
        @Schema(description = "Max weight in kg (strength only)", example = "80.0")
        BigDecimal maxWeightKg,
        @Schema(description = "Average reps (strength only)", example = "7.5") BigDecimal avgReps,
        @Schema(description = "Total duration in seconds (cardio only)") BigDecimal totalDurationSeconds,
        @Schema(description = "Total distance in km (cardio only)", example = "42.2") BigDecimal totalDistanceKm,
        @Schema(description = "Average speed in km/h (cardio only)") BigDecimal avgSpeedKmh,
        @Schema(description = "Fun copy for this exercise") String funCopy) {
}
