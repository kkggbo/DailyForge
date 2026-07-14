package com.dailyforge.modules.plan.interfaces.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Update draft cycle template request")
public record UpdateDraftCycleTemplateRequest(
        @Schema(description = "Template name", example = "Push Pull Legs v2",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 128) String templateName,

        @Schema(description = "Cycle length, nullable in draft", example = "6")
        @Min(1) @Max(7) Integer cycleLength,

        @Schema(description = "Goal type", example = "muscle_gain")
        @Size(max = 32) String goalType,

        @ArraySchema(schema = @Schema(implementation = CycleTemplateDayRequest.class))
        List<@Valid CycleTemplateDayRequest> days) {
}
