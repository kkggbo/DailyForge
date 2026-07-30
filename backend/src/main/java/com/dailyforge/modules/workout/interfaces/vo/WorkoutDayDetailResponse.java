package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "A browsed day in the current workout cycle")
public record WorkoutDayDetailResponse(
        @Schema(description = "Cycle run id", example = "31") Long cycleRunId,
        @Schema(description = "Cycle run number", example = "2") Integer runNo,
        @Schema(description = "Template id", example = "101") Long templateId,
        @Schema(description = "Template name", example = "Push Pull Legs") String templateName,
        @Schema(description = "Day index", example = "3") Integer dayIndex,
        @Schema(description = "Day name", example = "Leg day") String dayName,
        @Schema(description = "Whether this day contains no exercises", example = "false") Boolean isRestDay,
        @Schema(description = "Day state", example = "current",
                allowableValues = {"completed", "current", "upcoming"}) String dayState,
        @Schema(description = "View mode", example = "editable",
                allowableValues = {"editable", "readonly", "preview"}) String viewMode,
        @Schema(description = "Whether the current day can initialize a session", example = "true")
        Boolean canInitializeSession,
        @Schema(description = "Created session. Null for a future-day preview.", nullable = true) WorkoutSessionResponse session,
        @Schema(description = "Plan exercises when session is null. Null when a session is returned.", nullable = true)
        List<WorkoutSessionExerciseResponse> exercises) {
}
