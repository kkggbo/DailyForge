package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Cycle template day response")
public record CycleTemplateDayResponse(
        @Schema(description = "Day index", example = "1") Integer dayIndex,
        @Schema(description = "Day name", example = "Push") String dayName,
        @Schema(description = "Whether this is a rest day", example = "false") Boolean isRestDay,
        @Schema(description = "Whether this day is locked", example = "true") Boolean isLocked,
        @Schema(description = "Exercises of this day") List<CycleTemplateExerciseResponse> exercises) {
}
