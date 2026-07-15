package com.dailyforge.modules.exercise.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Exercise muscle response")
public record ExerciseMuscleResponse(
        @Schema(description = "Muscle id", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
        Long muscleId,
        @Schema(description = "Muscle name", example = "Pectoralis Major",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String muscleName,
        @Schema(description = "Muscle code", example = "pectoralis_major",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String muscleCode,
        @Schema(description = "Relation type", example = "primary",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String relationType) {
}
