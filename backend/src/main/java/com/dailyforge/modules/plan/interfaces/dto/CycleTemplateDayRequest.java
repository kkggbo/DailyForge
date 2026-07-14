package com.dailyforge.modules.plan.interfaces.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Cycle template day request")
public record CycleTemplateDayRequest(
        @Schema(description = "Day index in cycle", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) @Max(7) Integer dayIndex,

        @Schema(description = "Display name of the day", example = "Push")
        @Size(max = 64) String dayName,

        @ArraySchema(schema = @Schema(implementation = CycleTemplateExerciseRequest.class))
        List<@Valid CycleTemplateExerciseRequest> exercises) {
}
