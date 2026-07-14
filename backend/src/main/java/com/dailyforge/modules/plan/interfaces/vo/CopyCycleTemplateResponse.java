package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Copy cycle template response")
public record CopyCycleTemplateResponse(
        @Schema(description = "Copied template id", example = "301") Long templateId,
        @Schema(description = "Template status", example = "draft") String status) {
}
