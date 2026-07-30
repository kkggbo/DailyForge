package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Workout workspace context and active cycle navigation")
public record WorkoutWorkspaceContextResponse(
        @Schema(description = "Workspace state", example = "active",
                allowableValues = {"no_active_template", "active", "cycle_completed"}) String workspaceState,
        @Schema(description = "Active template id", example = "101", nullable = true) Long templateId,
        @Schema(description = "Active template name", example = "Push Pull Legs", nullable = true) String templateName,
        @Schema(description = "Current cycle run id", example = "31", nullable = true) Long cycleRunId,
        @Schema(description = "Cycle run number", example = "2", nullable = true) Integer runNo,
        @Schema(description = "Cycle length", example = "6", nullable = true) Integer cycleLength,
        @Schema(description = "Actual current day index", example = "3", nullable = true) Integer currentDayIndex,
        @Schema(description = "Default day index when opening the workspace", example = "3", nullable = true)
        Integer defaultDayIndex,
        @Schema(description = "Days in the active cycle") List<WorkoutDayNavigationItemResponse> days) {
}
