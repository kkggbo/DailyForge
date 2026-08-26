package com.dailyforge.modules.aicoach.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "AI next-cycle template generation request")
public record NextCycleGenerationRequest(
        @Schema(description = "Client request id for idempotency", example = "1f5c0d6e-a2f9-4d3d-8b11-577a5906b651")
        @Size(max = 64) String clientRequestId,

        @Schema(description = "Previous completed cycle run id", example = "1201", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) Long sourceCycleRunId,

        @Schema(description = "Previous cycle summary task id; if empty the backend picks the latest succeeded summary for the cycle run",
                example = "88")
        @Min(1) Long sourceSummaryTaskId,

        @Schema(description = "Scene type", example = "gym", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 32) String sceneType,

        @Schema(description = "Goal type for this generation", example = "muscle_gain",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 32) String goalType,

        @Schema(description = "Cycle length", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) @Max(7) Integer cycleLength,

        @Schema(description = "Whether cardio is allowed in this generation", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Boolean includeCardio,

        @Schema(description = "One-off additional requirements for this generation only",
                example = "本周工作量大，想适当降低强度。")
        @Size(max = 500) String additionalRequirements) {
}
