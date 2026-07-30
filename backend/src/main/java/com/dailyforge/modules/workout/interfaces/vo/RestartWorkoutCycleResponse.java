package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of restarting a completed cycle with the current template")
public record RestartWorkoutCycleResponse(
        @Schema(description = "Template id", example = "101") Long templateId,
        @Schema(description = "Template name", example = "Push Pull Legs") String templateName,
        @Schema(description = "New cycle run id", example = "32") Long cycleRunId,
        @Schema(description = "New cycle run number", example = "3") Integer runNo,
        @Schema(description = "New cycle run status", example = "active") String cycleRunStatus,
        @Schema(description = "Current day index of the new run", example = "1") Integer currentDayIndex) {
}
