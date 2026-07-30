package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Minimal day summary returned after a workout is completed")
public record WorkoutDaySummaryResponse(
        @Schema(description = "Day index", example = "4") Integer dayIndex,
        @Schema(description = "Day name", example = "Rest") String dayName,
        @Schema(description = "Whether this day contains no exercises", example = "true") Boolean isRestDay) {
}
