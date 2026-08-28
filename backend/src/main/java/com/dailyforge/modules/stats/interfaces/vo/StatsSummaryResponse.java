package com.dailyforge.modules.stats.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Stats summary response")
public record StatsSummaryResponse(
        @Schema(description = "Overall statistics across all exercises") StatsOverallResponse overall,
        @Schema(description = "Per-exercise aggregates, ordered by appearance count descending")
        List<StatsExerciseAggregateResponse> exercises) {
}
