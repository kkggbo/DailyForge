package com.dailyforge.modules.exercise.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Exercise selector category with its detailed muscles")
public record ExerciseCategoryResponse(
        @Schema(description = "Category code", example = "chest", requiredMode = Schema.RequiredMode.REQUIRED)
        String categoryCode,
        @Schema(description = "Category name", example = "胸", requiredMode = Schema.RequiredMode.REQUIRED)
        String categoryName,
        @Schema(description = "Sort order", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer sortOrder,
        @Schema(description = "Selectable muscle children", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ExerciseFilterMuscleResponse> children) {
}
