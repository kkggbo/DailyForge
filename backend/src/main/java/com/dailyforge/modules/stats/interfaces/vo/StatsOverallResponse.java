package com.dailyforge.modules.stats.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Overall training statistics")
public record StatsOverallResponse(
        @Schema(description = "Number of sessions with actual workout data", example = "12")
        int sessionCount,
        @Schema(description = "Total sets with actual values", example = "200")
        int totalSets,
        @Schema(description = "Total reps", example = "1500")
        int totalReps,
        @Schema(description = "Total volume in kg (strength only)", example = "12345.5")
        BigDecimal totalVolumeKg,
        @Schema(description = "Total distance in km (cardio only)", example = "88.5")
        BigDecimal totalDistanceKm,
        @Schema(description = "Total duration in minutes", example = "720")
        BigDecimal totalDurationMinutes,
        @Schema(description = "Overview copy with fun equivalence", example = "你从开始运动到现在累计训练 12 场、总容量 12345.5kg、总里程 88.5km。")
        String overviewCopy) {
}
