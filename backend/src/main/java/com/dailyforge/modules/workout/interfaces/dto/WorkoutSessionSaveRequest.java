package com.dailyforge.modules.workout.interfaces.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Full replacement request for an in-progress workout session")
public record WorkoutSessionSaveRequest(
        @Schema(description = "Legacy compatibility input. New clients should use notes. Null clears the saved value.",
                example = "Overall energy was average")
        @Size(max = 255) String overallFeeling,

        @Schema(description = "Combined session notes. New clients should use this field. Null clears the saved value.",
                example = "Increase leg press weight slightly next time")
        @Size(max = 1000) String notes,

        @ArraySchema(
                schema = @Schema(implementation = WorkoutSessionExerciseSaveRequest.class),
                arraySchema = @Schema(description = "All exercises in the session snapshot. Rest days require an empty array.",
                        requiredMode = Schema.RequiredMode.REQUIRED))
        @NotNull List<@Valid WorkoutSessionExerciseSaveRequest> exercises) {
}
