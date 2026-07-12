package com.dailyforge.modules.profile.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Body metric history item")
public record BodyMetricLogItemResponse(
        @Schema(description = "History record id", example = "12") Long id,
        @Schema(description = "Record date", example = "2026-07-12") LocalDate recordDate,
        @Schema(description = "Weight in kilograms", example = "76.50") BigDecimal weightKg,
        @Schema(description = "Body fat percent", example = "18.20") BigDecimal bodyFatPercent,
        @Schema(description = "BMI", example = "24.10") BigDecimal bmi,
        @Schema(description = "Skeletal muscle percent", example = "39.80") BigDecimal skeletalMusclePercent,
        @Schema(description = "Body water percent", example = "56.10") BigDecimal bodyWaterPercent,
        @Schema(description = "Basal metabolic rate in kcal", example = "1680.00") BigDecimal basalMetabolicRateKcal,
        @Schema(description = "Waist circumference in centimeters", example = "82.00") BigDecimal waistCm,
        @Schema(description = "Hip circumference in centimeters", example = "96.00") BigDecimal hipCm,
        @Schema(description = "Waist hip ratio", example = "0.85") BigDecimal waistHipRatio,
        @Schema(description = "Body age", example = "24") Integer bodyAge,
        @Schema(description = "Body type", example = "healthy") String bodyType,
        @Schema(description = "Optional note", example = "Gym body scan") String note,
        @Schema(description = "Whether this is the latest active record", example = "true") boolean isLatest) {
}
