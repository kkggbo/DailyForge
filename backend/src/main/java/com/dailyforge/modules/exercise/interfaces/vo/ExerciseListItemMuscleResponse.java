package com.dailyforge.modules.exercise.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lightweight muscle object in system exercise list item")
public record ExerciseListItemMuscleResponse(
        @Schema(description = "Muscle id", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        Long muscleId,
        @Schema(description = "Muscle name", example = "胸大肌中部", requiredMode = Schema.RequiredMode.REQUIRED)
        String muscleName,
        @Schema(description = "Muscle code", example = "pectoralis_major_middle",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String muscleCode) {
}
