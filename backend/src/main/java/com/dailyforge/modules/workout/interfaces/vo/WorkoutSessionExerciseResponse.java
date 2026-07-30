package com.dailyforge.modules.workout.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "One exercise in a workout session or future-day preview")
public record WorkoutSessionExerciseResponse(
        @Schema(description = "Session exercise id. Null for a future-day preview.", example = "7001", nullable = true)
        Long sessionExerciseId,
        @Schema(description = "Exercise display order", example = "1") Integer sortOrder,
        @Schema(description = "System exercise id", example = "1001") Long exerciseId,
        @Schema(description = "Exercise name snapshot", example = "Barbell Bench Press") String exerciseName,
        @Schema(description = "Exercise structure type", example = "set_based",
                allowableValues = {"set_based", "single_segment"}) String structureType,
        @Schema(description = "Exercise completion status", example = "partial_completed", nullable = true,
                allowableValues = {"completed", "partial_completed", "skipped", "failed"}) String exerciseStatus,
        @Schema(description = "Failure or skip reason", example = "too_tired", nullable = true,
                allowableValues = {"too_tired", "equipment_unavailable", "pain_or_discomfort",
                        "time_not_enough", "plan_too_hard", "other"}) String failureReason,
        @Schema(description = "Combined exercise feedback or note. Legacy feeling and adjustment note are merged for historical records.",
                example = "Triceps fatigued early", nullable = true) String feedback,
        @Schema(description = "Exercise item snapshots") List<WorkoutSessionExerciseItemResponse> items) {
}
