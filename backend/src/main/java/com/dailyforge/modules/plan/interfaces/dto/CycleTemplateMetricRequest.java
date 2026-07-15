package com.dailyforge.modules.plan.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Cycle template exercise item metric request")
public record CycleTemplateMetricRequest(
        @Schema(description = "Metric display order", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Integer sortOrder,

        @Schema(description = "Metric key", example = "weight_kg", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 64) String metricKey,

        @Schema(description = "Numeric metric value", example = "60", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @DecimalMin(value = "0.00") @DecimalMax(value = "99999999.9999")
        BigDecimal metricValueNumber) {
}
