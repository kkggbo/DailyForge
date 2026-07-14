package com.dailyforge.modules.plan.interfaces.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Cycle template exercise request")
public record CycleTemplateExerciseRequest(
        @Schema(description = "Exercise display order", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) Integer sortOrder,

        @Schema(description = "System exercise id", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) Long exerciseId,

        @Schema(description = "Target sets", example = "4")
        @Min(1) @Max(100) Integer targetSets,

        @Schema(description = "Target min reps", example = "6")
        @Min(1) @Max(1000) Integer targetRepsMin,

        @Schema(description = "Target max reps", example = "8")
        @Min(1) @Max(1000) Integer targetRepsMax,

        @Schema(description = "Target weight in kilograms", example = "60.00")
        @DecimalMin(value = "0.00") @DecimalMax(value = "9999.99") BigDecimal targetWeightKg,

        @Schema(description = "Target duration in seconds", example = "1800")
        @Min(1) @Max(86400) Integer targetDurationSeconds,

        @Schema(description = "Target rest seconds", example = "180")
        @Min(0) @Max(86400) Integer restSeconds,

        @Schema(description = "Target RPE", example = "8.0")
        @DecimalMin(value = "0.00") @DecimalMax(value = "10.00") BigDecimal targetRpe,

        @Schema(description = "Exercise note", example = "Last set close to failure")
        @Size(max = 500) String note,

        @Schema(description = "Extra target config JSON", example = "{\"pace\":\"5:30\",\"incline\":6}")
        JsonNode targetExtraJson) {
}
