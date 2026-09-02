package com.dailyforge.modules.diet.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "覆盖每日目标请求（clear=true 时清除自定义回自动）")
public record OverrideTargetRequest(
        @Schema(description = "热量(kcal)", example = "2200")
        @DecimalMin("0.01") BigDecimal caloriesKcal,

        @Schema(description = "蛋白质(g)", example = "150")
        @DecimalMin("0.01") BigDecimal proteinG,

        @Schema(description = "碳水(g)", example = "250")
        @DecimalMin("0.01") BigDecimal carbsG,

        @Schema(description = "脂肪(g)", example = "73")
        @DecimalMin("0.01") BigDecimal fatG,

        @Schema(description = "是否清除自定义回自动", example = "false")
        Boolean clear) {
}
