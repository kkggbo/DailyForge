package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Result of saving an in-progress workout session")
public record SaveWorkoutSessionResponse(
        @Schema(description = "Workout session id", example = "501") Long sessionId,
        @Schema(description = "Session status", example = "in_progress") String sessionStatus,
        @Schema(description = "Time the session was saved", example = "2026-07-29T20:30:00")
        LocalDateTime savedAt) {
}
