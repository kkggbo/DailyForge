package com.dailyforge.modules.workout.interfaces.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Editable actual values for one exercise item")
public record WorkoutSessionExerciseItemSaveRequest(
        @Schema(description = "Item index from the exercise snapshot", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Min(1) Integer itemIndex,

        @ArraySchema(
                schema = @Schema(implementation = WorkoutSessionExerciseMetricSaveRequest.class),
                arraySchema = @Schema(description = "All metric keys in the item snapshot",
                        requiredMode = Schema.RequiredMode.REQUIRED))
        @NotNull List<@Valid WorkoutSessionExerciseMetricSaveRequest> metrics) {
}
