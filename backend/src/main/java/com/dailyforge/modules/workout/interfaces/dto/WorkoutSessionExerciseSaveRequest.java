package com.dailyforge.modules.workout.interfaces.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Editable data for one exercise in a workout session")
public record WorkoutSessionExerciseSaveRequest(
        @Schema(description = "Session exercise id from the initialized session", example = "7001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Positive Long sessionExerciseId,

        @Schema(description = "Exercise completion status. Required only when completing a workout.",
                example = "partial_completed",
                allowableValues = {"completed", "partial_completed", "skipped", "failed"})
        @Pattern(regexp = "completed|partial_completed|skipped|failed")
        String exerciseStatus,

        @Schema(description = "Reason for a skipped, failed, or partially completed exercise", example = "too_tired",
                allowableValues = {"too_tired", "equipment_unavailable", "pain_or_discomfort",
                        "time_not_enough", "plan_too_hard", "other"})
        @Pattern(regexp = "too_tired|equipment_unavailable|pain_or_discomfort|time_not_enough|plan_too_hard|other")
        String failureReason,

        @Schema(description = "Legacy compatibility input. New clients should use feedback.", example = "Triceps fatigued early")
        @Size(max = 255) String feeling,

        @Schema(description = "Legacy compatibility input. New clients should use feedback.", example = "Reduce warm-up push-ups")
        @Size(max = 500) String adjustmentNote,

        @Schema(description = "Combined exercise feedback or note. Preferred by the merged frontend field.",
                example = "Triceps fatigued early; reduce warm-up push-ups", nullable = true)
        @Size(max = 500) String feedback,

        @ArraySchema(
                schema = @Schema(implementation = WorkoutSessionExerciseItemSaveRequest.class),
                arraySchema = @Schema(description = "All item snapshots for this exercise",
                        requiredMode = Schema.RequiredMode.REQUIRED))
        @NotNull List<@Valid WorkoutSessionExerciseItemSaveRequest> items) {
}

