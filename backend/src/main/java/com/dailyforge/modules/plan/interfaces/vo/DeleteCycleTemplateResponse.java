package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Delete cycle template response")
public record DeleteCycleTemplateResponse(
        @Schema(description = "Template id", example = "102") Long templateId,
        @Schema(description = "Template status", example = "deleted") String status) {
}
