package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Current active cycle template response")
public record CurrentActiveCycleTemplateResponse(
        @Schema(description = "Template id", example = "101") Long templateId,
        @Schema(description = "Template name", example = "Push Pull Legs") String templateName,
        @Schema(description = "Cycle length", example = "6") Integer cycleLength,
        @Schema(description = "Current day index", example = "3") Integer currentDayIndex,
        @Schema(description = "Current day name", example = "Legs") String currentDayName,
        @Schema(description = "Editable from day index", example = "3") Integer editableFromDayIndex,
        @Schema(description = "Run started at", example = "2026-07-10T08:00:00") LocalDateTime startedAt) {
}
