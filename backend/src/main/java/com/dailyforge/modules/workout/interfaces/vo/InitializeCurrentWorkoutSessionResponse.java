package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of idempotently initializing the current workout day session")
public record InitializeCurrentWorkoutSessionResponse(
        @Schema(description = "Whether a new session was created", example = "true") Boolean sessionCreated,
        @Schema(description = "Initialized current day") WorkoutDayDetailResponse day) {
}
