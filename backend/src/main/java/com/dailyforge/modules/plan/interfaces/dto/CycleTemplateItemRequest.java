package com.dailyforge.modules.plan.interfaces.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Cycle template exercise item request")
public record CycleTemplateItemRequest(
        @Schema(description = "Item index under one exercise", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) Integer itemIndex,

        @Schema(description = "Item type", example = "set", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 32) String itemType,

        @Schema(description = "Item display name", example = "Set 1")
        @Size(max = 64) String itemName,

        @Schema(description = "Item note", example = "Warm-up set")
        @Size(max = 500) String note,

        @ArraySchema(schema = @Schema(implementation = CycleTemplateMetricRequest.class))
        List<@Valid CycleTemplateMetricRequest> metrics) {
}
