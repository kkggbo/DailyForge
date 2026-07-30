package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One day in the active workout cycle navigation")
public record WorkoutDayNavigationItemResponse(
        @Schema(description = "Day index in the current cycle", example = "3") Integer dayIndex,
        @Schema(description = "Day name", example = "Leg day") String dayName,
        @Schema(description = "Whether this day contains no exercises", example = "false") Boolean isRestDay,
        @Schema(description = "Day state", example = "current",
                allowableValues = {"completed", "current", "upcoming"}) String dayState,
        @Schema(description = "Created session id, if available", example = "501", nullable = true) Long sessionId,
        @Schema(description = "Created session status, if available", example = "in_progress", nullable = true,
                allowableValues = {"in_progress", "completed", "cancelled"}) String sessionStatus) {
}
