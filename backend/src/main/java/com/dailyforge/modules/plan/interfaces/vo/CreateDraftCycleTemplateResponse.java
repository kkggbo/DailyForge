package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Create draft cycle template response")
public record CreateDraftCycleTemplateResponse(
        @Schema(description = "Template id", example = "201") Long templateId,
        @Schema(description = "Template status", example = "draft") String status) {
}
