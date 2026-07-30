package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Result of completing a workout session and advancing the cycle")
public record CompleteWorkoutSessionResponse(
        @Schema(description = "Completed workout session id", example = "501") Long sessionId,
        @Schema(description = "Completed session status", example = "completed") String sessionStatus,
        @Schema(description = "Completion time", example = "2026-07-29T21:10:00") LocalDateTime completedAt,
        @Schema(description = "Completed day index", example = "3") Integer completedDayIndex,
        @Schema(description = "Cycle run id", example = "31") Long cycleRunId,
        @Schema(description = "Cycle run status after completion", example = "active",
                allowableValues = {"active", "completed"}) String cycleRunStatus,
        @Schema(description = "Next actual current day index. Null after the final day.", example = "4", nullable = true)
        Integer nextCurrentDayIndex,
        @Schema(description = "Next day summary. Null after the final day.", nullable = true) WorkoutDaySummaryResponse nextDay,
        @Schema(description = "Completed day detail for the workspace to remain on", nullable = true)
        WorkoutDayDetailResponse completedDay) {
}
