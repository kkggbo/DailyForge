package com.dailyforge.modules.stats.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "One progression point for an exercise on a given date")
public record StatsProgressionPointResponse(
        @Schema(description = "Date", example = "2026-07-01") String date,
        @Schema(description = "Max weight in kg (strength only)", example = "60.0") BigDecimal maxWeightKg,
        @Schema(description = "Max reps (strength only)", example = "10") Integer maxReps,
        @Schema(description = "Total volume in kg (strength only)", example = "1200.0") BigDecimal totalVolumeKg,
        @Schema(description = "Total duration in seconds (cardio only)") BigDecimal totalDurationSeconds,
        @Schema(description = "Total distance in km (cardio only)", example = "5.0") BigDecimal totalDistanceKm) {
}
