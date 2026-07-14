package com.dailyforge.modules.plan.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Activate cycle template request")
public record ActivateCycleTemplateRequest(
        @Schema(description = "Whether user confirmed switching active template", example = "true")
        Boolean confirmSwitch) {
}
