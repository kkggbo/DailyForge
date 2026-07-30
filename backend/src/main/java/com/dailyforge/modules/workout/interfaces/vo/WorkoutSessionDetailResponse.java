package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Full read-only or resumable workout session detail")
public record WorkoutSessionDetailResponse(
        @Schema(description = "Workout session id", example = "490") Long sessionId,
        @Schema(description = "Session type", example = "workout", allowableValues = {"workout", "rest_day"})
        String sessionType,
        @Schema(description = "Session status", example = "completed",
                allowableValues = {"in_progress", "completed", "cancelled"}) String sessionStatus,
        @Schema(description = "Cycle run id", example = "31") Long cycleRunId,
        @Schema(description = "Cycle run number", example = "2") Integer runNo,
        @Schema(description = "Template id", example = "101") Long templateId,
        @Schema(description = "Template name snapshot", example = "Push Pull Legs") String templateName,
        @Schema(description = "Day index", example = "1") Integer dayIndex,
        @Schema(description = "Day name snapshot", example = "Push") String dayName,
        @Schema(description = "Session start time", example = "2026-07-27T19:00:00") LocalDateTime startedAt,
        @Schema(description = "Session completion time", example = "2026-07-27T20:10:00", nullable = true)
        LocalDateTime completedAt,
        @Schema(description = "Combined session notes. Legacy overall feeling is merged for historical records.",
                nullable = true) String notes,
        @Schema(description = "Session exercises") List<WorkoutSessionExerciseResponse> exercises) {
}
