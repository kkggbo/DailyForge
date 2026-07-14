package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Cycle template detail response")
public record CycleTemplateDetailResponse(
        @Schema(description = "Template id", example = "101") Long templateId,
        @Schema(description = "Template name", example = "Push Pull Legs") String templateName,
        @Schema(description = "Goal type", example = "muscle_gain") String goalType,
        @Schema(description = "Template status", example = "active") String status,
        @Schema(description = "Cycle length", example = "6") Integer cycleLength,
        @Schema(description = "Whether active", example = "true") Boolean isActive,
        @Schema(description = "Current day index", example = "3") Integer currentDayIndex,
        @Schema(description = "Editable from day index", example = "3") Integer editableFromDayIndex,
        @Schema(description = "Whether template can be activated", example = "false") Boolean canActivate,
        @Schema(description = "Whether template can be deleted", example = "false") Boolean canDelete,
        @Schema(description = "Created at", example = "2026-07-01T20:00:00") LocalDateTime createdAt,
        @Schema(description = "Updated at", example = "2026-07-14T20:15:30") LocalDateTime updatedAt,
        @Schema(description = "Days") List<CycleTemplateDayResponse> days) {
}
