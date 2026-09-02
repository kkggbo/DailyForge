package com.dailyforge.modules.diet.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "一条饮食记录（含营养快照）")
public record DietLogItemVO(
        @Schema(description = "记录id") Long logId,
        @Schema(description = "食物id") Long foodId,
        @Schema(description = "食物名") String foodName,
        @Schema(description = "克数") BigDecimal grams,
        @Schema(description = "热量(kcal)") BigDecimal caloriesKcal,
        @Schema(description = "蛋白质(g)") BigDecimal proteinG,
        @Schema(description = "碳水(g)") BigDecimal carbsG,
        @Schema(description = "脂肪(g)") BigDecimal fatG) {
}
