package com.dailyforge.modules.exercise.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Selectable muscle option inside one exercise selector category")
public record ExerciseFilterMuscleResponse(
        @Schema(description = "Muscle id", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        Long muscleId,
        @Schema(description = "Muscle name", example = "胸大肌上沿", requiredMode = Schema.RequiredMode.REQUIRED)
        String muscleName,
        @Schema(description = "Muscle code", example = "pectoralis_major_upper",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String muscleCode,
        @Schema(description = "Parent muscle id", example = "1")
        Long parentMuscleId,
        @Schema(description = "Parent muscle name", example = "胸大肌")
        String parentMuscleName,
        @Schema(description = "Sort order", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer sortOrder) {
}
