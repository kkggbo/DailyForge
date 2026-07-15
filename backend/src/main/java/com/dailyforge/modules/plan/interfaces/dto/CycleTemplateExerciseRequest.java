package com.dailyforge.modules.plan.interfaces.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Cycle template exercise request")
public record CycleTemplateExerciseRequest(
        @Schema(description = "Exercise display order", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) Integer sortOrder,

        @Schema(description = "System exercise id", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) Long exerciseId,

        @Schema(description = "Structure type from system exercise metadata", example = "set_based",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 32) String structureType,

        @Schema(description = "Exercise note", example = "Keep last set near failure")
        @Size(max = 500) String note,

        @ArraySchema(schema = @Schema(implementation = CycleTemplateItemRequest.class))
        List<@Valid CycleTemplateItemRequest> items) {
}
