package com.dailyforge.modules.exercise.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Exercise equipment response")
public record ExerciseEquipmentResponse(
        @Schema(description = "Equipment id", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        Long equipmentId,
        @Schema(description = "Equipment name", example = "Barbell", requiredMode = Schema.RequiredMode.REQUIRED)
        String equipmentName,
        @Schema(description = "Equipment scene type", example = "gym", requiredMode = Schema.RequiredMode.REQUIRED)
        String sceneType) {
}
