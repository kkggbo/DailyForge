package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "One recent workout session summary")
public record WorkoutRecentSessionItemResponse(
        @Schema(description = "Workout session id", example = "501") Long sessionId,
        @Schema(description = "Session type", example = "workout", allowableValues = {"workout", "rest_day"})
        String sessionType,
        @Schema(description = "Session status", example = "completed",
                allowableValues = {"in_progress", "completed", "cancelled"}) String sessionStatus,
        @Schema(description = "Template id", example = "101") Long templateId,
        @Schema(description = "Template name snapshot", example = "Push Pull Legs") String templateName,
        @Schema(description = "Cycle run id", example = "31") Long cycleRunId,
        @Schema(description = "Cycle run number", example = "2") Integer runNo,
        @Schema(description = "Day index", example = "3") Integer dayIndex,
        @Schema(description = "Day name snapshot", example = "Leg day") String dayName,
        @Schema(description = "Session start time", example = "2026-07-29T20:15:30") LocalDateTime startedAt,
        @Schema(description = "Session completion time", example = "2026-07-29T21:10:00", nullable = true)
        LocalDateTime completedAt) {
}
