package com.dailyforge.modules.diet.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "上传食物请求")
public record UploadFoodRequest(
        @Schema(description = "食物名称", example = "自制鸡胸沙拉", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 64) String name,

        @Schema(description = "分类", example = "meat_egg")
        @Pattern(regexp = "staple|meat_egg|vegetable|fruit|dairy|nut_bean|drink|other") String category,

        @Schema(description = "每100g热量(kcal)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @DecimalMin("0") @DecimalMax("999999.99") BigDecimal caloriesKcal,

        @Schema(description = "每100g蛋白质(g)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @DecimalMin("0") @DecimalMax("999999.99") BigDecimal proteinG,

        @Schema(description = "每100g碳水(g)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @DecimalMin("0") @DecimalMax("999999.99") BigDecimal carbsG,

        @Schema(description = "每100g脂肪(g)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @DecimalMin("0") @DecimalMax("999999.99") BigDecimal fatG) {
}
