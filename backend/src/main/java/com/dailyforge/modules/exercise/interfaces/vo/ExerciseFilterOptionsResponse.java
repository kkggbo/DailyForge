package com.dailyforge.modules.exercise.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Exercise selector filter options response")
public record ExerciseFilterOptionsResponse(
        @Schema(description = "Top-level categories", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ExerciseCategoryResponse> categories) {
}
