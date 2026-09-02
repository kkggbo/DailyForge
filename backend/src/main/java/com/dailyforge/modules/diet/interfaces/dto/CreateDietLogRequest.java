package com.dailyforge.modules.diet.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

@Schema(description = "添加饮食记录请求")
public record CreateDietLogRequest(
        @Schema(description = "记录日期", example = "2026-09-03", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String date,

        @Schema(description = "餐次", example = "lunch", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "breakfast|lunch|dinner|snack") String mealType,

        @Schema(description = "食物ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long foodId,

        @Schema(description = "克数", example = "200", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @DecimalMin("0.01") BigDecimal grams) {
}
