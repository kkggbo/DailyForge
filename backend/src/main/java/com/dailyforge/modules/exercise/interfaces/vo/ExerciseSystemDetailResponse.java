package com.dailyforge.modules.exercise.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "System exercise detail response")
public record ExerciseSystemDetailResponse(
        @Schema(description = "Exercise id", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
        Long exerciseId,
        @Schema(description = "Exercise name", example = "Barbell Bench Press",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String exerciseName,
        @Schema(description = "Exercise type", example = "strength", requiredMode = Schema.RequiredMode.REQUIRED)
        String exerciseType,
        @Schema(description = "Movement type", example = "push")
        String movementType,
        @Schema(description = "Default unit", example = "kg", requiredMode = Schema.RequiredMode.REQUIRED)
        String defaultUnit,
        @Schema(description = "Default structure type", example = "set_based",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String defaultStructureType,
        @Schema(description = "Demo video url", example = "https://example.com/videos/bench-press")
        String videoUrl,
        @Schema(description = "Calorie burn reference", example = "6.50")
        BigDecimal calorieBurnReference,
        @Schema(description = "Calorie reference unit", example = "kcal/min")
        String calorieReferenceUnit,
        @Schema(description = "Primary muscles", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ExerciseMuscleResponse> primaryMuscles,
        @Schema(description = "Secondary muscles", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ExerciseMuscleResponse> secondaryMuscles,
        @Schema(description = "Equipments", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ExerciseEquipmentResponse> equipments) {
}
