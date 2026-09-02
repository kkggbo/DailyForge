package com.dailyforge.modules.diet.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "每日饮食总结")
public record DietSummaryVO(
        @Schema(description = "日期") String date,
        @Schema(description = "当日目标（可为 null）") DietTargetVO target,
        @Schema(description = "各餐分组") Map<String, List<DietLogItemVO>> meals,
        @Schema(description = "当日营养素合计") NutritionTotals totals,
        @Schema(description = "进度（target 为 null 时为 null）") DietProgressVO progress) {

    @Schema(description = "营养素合计")
    public record NutritionTotals(
            BigDecimal caloriesKcal, BigDecimal proteinG, BigDecimal carbsG, BigDecimal fatG) {
    }

    @Schema(description = "进度")
    public record DietProgressVO(
            Integer caloriesPct, Integer proteinPct, Integer carbsPct, Integer fatPct) {
    }
}
