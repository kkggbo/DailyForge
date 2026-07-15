package com.dailyforge.modules.exercise.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "System exercise list response")
public record ExerciseSystemListResponse(
        @Schema(description = "Page number", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer page,
        @Schema(description = "Page size", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer pageSize,
        @Schema(description = "Total records", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
        Long total,
        @Schema(description = "Exercise records", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ExerciseSystemListItemResponse> records) {
}
