package com.dailyforge.modules.aicoach.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "AI cycle summary request")
public record CycleSummaryRequest(
        @Schema(description = "Client request id for idempotency", example = "59dc7a31-df2f-44b1-a344-2f1cd99f16fc")
        @Size(max = 64) String clientRequestId,

        @Schema(description = "Completed cycle run id", example = "1201",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) Long cycleRunId) {
}