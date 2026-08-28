package com.dailyforge.modules.stats.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Body metric series response")
public record BodyMetricSeriesResponse(
        @Schema(description = "Metric key", example = "weight_kg") String metric,
        @Schema(description = "Metric unit", example = "kg") String unit,
        @Schema(description = "Time series points") List<BodyMetricPoint> points) {

    @Schema(description = "One body metric point")
    public record BodyMetricPoint(
            @Schema(description = "Record date", example = "2026-07-01") String date,
            @Schema(description = "Metric value", example = "75.5") BigDecimal value) {
    }
}
