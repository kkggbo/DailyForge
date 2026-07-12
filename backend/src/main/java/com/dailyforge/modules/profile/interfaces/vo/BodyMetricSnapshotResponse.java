package com.dailyforge.modules.profile.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Current body metric snapshot response")
public record BodyMetricSnapshotResponse(
        @Schema(description = "Current weight in kilograms", example = "76.50") BigDecimal currentWeightKg,
        @Schema(description = "Current body fat percent", example = "18.20") BigDecimal currentBodyFatPercent,
        @Schema(description = "Current BMI", example = "24.10") BigDecimal currentBmi,
        @Schema(description = "Current skeletal muscle percent", example = "39.80")
        BigDecimal currentSkeletalMusclePercent,
        @Schema(description = "Current body water percent", example = "56.10") BigDecimal currentBodyWaterPercent,
        @Schema(description = "Current basal metabolic rate in kcal", example = "1680.00")
        BigDecimal currentBasalMetabolicRateKcal,
        @Schema(description = "Current waist circumference in centimeters", example = "82.00") BigDecimal currentWaistCm,
        @Schema(description = "Current hip circumference in centimeters", example = "96.00") BigDecimal currentHipCm,
        @Schema(description = "Current waist hip ratio", example = "0.85") BigDecimal currentWaistHipRatio,
        @Schema(description = "Current body age", example = "24") Integer currentBodyAge,
        @Schema(description = "Current body type", example = "healthy") String currentBodyType,
        @Schema(description = "Snapshot updated time", example = "2026-07-12T20:15:30") LocalDateTime updatedAt) {
}
