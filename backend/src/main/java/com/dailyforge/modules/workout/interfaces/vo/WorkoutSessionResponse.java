package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Workout session data for the workspace day detail")
public record WorkoutSessionResponse(
        @Schema(description = "Workout session id", example = "501") Long sessionId,
        @Schema(description = "Session type", example = "workout", allowableValues = {"workout", "rest_day"})
        String sessionType,
        @Schema(description = "Session status", example = "in_progress",
                allowableValues = {"in_progress", "completed", "cancelled"}) String sessionStatus,
        @Schema(description = "Session start time", example = "2026-07-29T20:15:30") LocalDateTime startedAt,
        @Schema(description = "Session completion time", example = "2026-07-29T21:10:00", nullable = true)
        LocalDateTime completedAt,
        @Schema(description = "Combined session notes. Legacy overall feeling is merged for historical records.",
                example = "Increase leg press weight next time", nullable = true)
        String notes,
        @Schema(description = "Session exercises") List<WorkoutSessionExerciseResponse> exercises) {
}
