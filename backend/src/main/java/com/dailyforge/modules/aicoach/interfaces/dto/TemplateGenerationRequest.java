package com.dailyforge.modules.aicoach.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "AI template generation request")
public record TemplateGenerationRequest(
        @Schema(description = "Client request id for idempotency", example = "8f1b7665-a2f9-4d3d-a7c8-577a5906b651")
        @Size(max = 64) String clientRequestId,

        @Schema(description = "Scene type", example = "gym", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 32) String sceneType,

        @Schema(description = "Goal type for this generation", example = "muscle_gain",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 32) String goalType,

        @Schema(description = "Cycle length", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) @Max(7) Integer cycleLength,

        @Schema(description = "Whether cardio is allowed in this generation", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Boolean includeCardio) {
}