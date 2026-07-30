package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Page of recent workout sessions")
public record WorkoutRecentSessionPageResponse(
        @Schema(description = "One-based page number", example = "1") int page,
        @Schema(description = "Page size", example = "20") int pageSize,
        @Schema(description = "Total matching sessions", example = "2") long total,
        @Schema(description = "Recent workout session records") List<WorkoutRecentSessionItemResponse> records) {
}
