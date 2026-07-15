package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Cycle template exercise response")
public record CycleTemplateExerciseResponse(
        @Schema(description = "Exercise order", example = "1") Integer sortOrder,
        @Schema(description = "Exercise id", example = "1001") Long exerciseId,
        @Schema(description = "Exercise name", example = "Barbell Bench Press") String exerciseName,
        @Schema(description = "Structure type", example = "set_based") String structureType,
        @Schema(description = "Exercise note", example = "Keep last set near failure") String note,
        @Schema(description = "Exercise items") List<CycleTemplateItemResponse> items) {
}
