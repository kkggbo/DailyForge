package com.dailyforge.modules.plan.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "AI generate draft cycle template request")
public record AiGenerateDraftCycleTemplateRequest(
        @Schema(description = "Goal type", example = "muscle_gain")
        @Size(max = 32) String goalType,

        @Schema(description = "Expected cycle length", example = "5")
        @Min(1) @Max(7) Integer cycleLength,

        @Schema(description = "Prompt used for generation", example = "Create a 5-day muscle gain split")
        @NotBlank @Size(max = 2000) String prompt,

        @Schema(description = "Whether to use user profile data", example = "true")
        Boolean useProfileData) {
}
