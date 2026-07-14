package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Activate cycle template response")
public record ActivateCycleTemplateResponse(
        @Schema(description = "Template id", example = "101") Long templateId,
        @Schema(description = "Template status", example = "active") String status,
        @Schema(description = "Current day index", example = "1") Integer currentDayIndex,
        @Schema(description = "Previous active template id", example = "88") Long previousActiveTemplateId) {
}
