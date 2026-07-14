package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Formal cycle template summary")
public record FormalCycleTemplateSummary(
        @Schema(description = "Template id", example = "101") Long templateId,
        @Schema(description = "Template name", example = "Push Pull Legs") String templateName,
        @Schema(description = "Cycle length", example = "6") Integer cycleLength,
        @Schema(description = "Goal type", example = "muscle_gain") String goalType,
        @Schema(description = "Template status", example = "active") String status,
        @Schema(description = "Whether this template is active", example = "true") Boolean isActive,
        @Schema(description = "Current day index for active template", example = "3") Integer currentDayIndex,
        @Schema(description = "Updated at", example = "2026-07-14T20:15:30") LocalDateTime updatedAt) {
}
