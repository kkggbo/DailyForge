package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Draft cycle template summary")
public record DraftCycleTemplateSummary(
        @Schema(description = "Template id", example = "201") Long templateId,
        @Schema(description = "Template name", example = "New Draft") String templateName,
        @Schema(description = "Cycle length", example = "5") Integer cycleLength,
        @Schema(description = "Configured day count", example = "3") Integer configuredDayCount,
        @Schema(description = "Created at", example = "2026-07-14T18:00:00") LocalDateTime createdAt,
        @Schema(description = "Updated at", example = "2026-07-14T19:10:00") LocalDateTime updatedAt) {
}
