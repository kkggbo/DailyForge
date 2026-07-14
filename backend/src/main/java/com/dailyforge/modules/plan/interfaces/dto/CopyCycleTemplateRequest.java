package com.dailyforge.modules.plan.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Copy cycle template request")
public record CopyCycleTemplateRequest(
        @Schema(description = "New template name", example = "Push Pull Legs - Copy",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 128) String templateName) {
}
