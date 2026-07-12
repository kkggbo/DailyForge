package com.dailyforge.modules.profile.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Create a body metric history record")
public record CreateBodyMetricRequest(
        @Schema(description = "Record date", example = "2026-07-12", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDate recordDate,

        @Schema(description = "Weight in kilograms", example = "76.50")
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        @DecimalMax(value = "9999.99", message = "must be less than or equal to 9999.99")
        BigDecimal weightKg,

        @Schema(description = "Body fat percent", example = "18.20")
        @DecimalMin(value = "0.00", message = "must be greater than or equal to 0")
        @DecimalMax(value = "100.00", message = "must be less than or equal to 100")
        BigDecimal bodyFatPercent,

        @Schema(description = "BMI", example = "24.10")
        @DecimalMin(value = "0.00", message = "must be greater than or equal to 0")
        @DecimalMax(value = "999.99", message = "must be less than or equal to 999.99")
        BigDecimal bmi,

        @Schema(description = "Skeletal muscle percent", example = "39.80")
        @DecimalMin(value = "0.00", message = "must be greater than or equal to 0")
        @DecimalMax(value = "100.00", message = "must be less than or equal to 100")
        BigDecimal skeletalMusclePercent,

        @Schema(description = "Body water percent", example = "56.10")
        @DecimalMin(value = "0.00", message = "must be greater than or equal to 0")
        @DecimalMax(value = "100.00", message = "must be less than or equal to 100")
        BigDecimal bodyWaterPercent,

        @Schema(description = "Basal metabolic rate in kcal", example = "1680.00")
        @DecimalMin(value = "0.00", message = "must be greater than or equal to 0")
        @DecimalMax(value = "999999.99", message = "must be less than or equal to 999999.99")
        BigDecimal basalMetabolicRateKcal,

        @Schema(description = "Waist circumference in centimeters", example = "82.00")
        @DecimalMin(value = "0.00", message = "must be greater than or equal to 0")
        @DecimalMax(value = "9999.99", message = "must be less than or equal to 9999.99")
        BigDecimal waistCm,

        @Schema(description = "Hip circumference in centimeters", example = "96.00")
        @DecimalMin(value = "0.00", message = "must be greater than or equal to 0")
        @DecimalMax(value = "9999.99", message = "must be less than or equal to 9999.99")
        BigDecimal hipCm,

        @Schema(description = "Waist hip ratio", example = "0.85")
        @DecimalMin(value = "0.00", message = "must be greater than or equal to 0")
        @DecimalMax(value = "999.99", message = "must be less than or equal to 999.99")
        BigDecimal waistHipRatio,

        @Schema(description = "Body age", example = "24")
        @Max(value = 150, message = "must be less than or equal to 150")
        Integer bodyAge,

        @Schema(description = "Body type", example = "healthy")
        @Size(max = 32, message = "size must be less than or equal to 32")
        String bodyType,

        @Schema(description = "Optional note", example = "Gym body scan")
        @Size(max = 1000, message = "size must be less than or equal to 1000")
        String note) {
}
