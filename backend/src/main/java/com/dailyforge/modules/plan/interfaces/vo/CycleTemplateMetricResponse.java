package com.dailyforge.modules.plan.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Cycle template exercise item metric response")
public record CycleTemplateMetricResponse(
        @Schema(description = "Metric order", example = "1") Integer sortOrder,
        @Schema(description = "Metric key", example = "weight_kg") String metricKey,
        @Schema(description = "Numeric metric value", example = "60.00") BigDecimal metricValueNumber,
        @Schema(description = "Derived metric unit", example = "kg") String metricUnit) {
}
