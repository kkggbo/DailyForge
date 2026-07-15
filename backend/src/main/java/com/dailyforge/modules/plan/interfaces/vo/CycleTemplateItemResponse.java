package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Cycle template exercise item response")
public record CycleTemplateItemResponse(
        @Schema(description = "Item index", example = "1") Integer itemIndex,
        @Schema(description = "Item type", example = "set") String itemType,
        @Schema(description = "Item name", example = "Set 1") String itemName,
        @Schema(description = "Item note", example = "Warm-up set") String note,
        @Schema(description = "Metric list") List<CycleTemplateMetricResponse> metrics) {
}
