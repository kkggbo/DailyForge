package com.dailyforge.modules.diet.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "食物条目（每100g营养）")
public record FoodItemVO(
        @Schema(description = "食物id") Long foodId,
        @Schema(description = "食物名") String name,
        @Schema(description = "分类") String category,
        @Schema(description = "来源(system/user)") String source,
        @Schema(description = "来源标签(官方/用户)") String sourceLabel,
        @Schema(description = "上传者脱敏昵称(system为null)") String ownerNickname,
        @Schema(description = "每100g热量") BigDecimal caloriesKcal,
        @Schema(description = "每100g蛋白质") BigDecimal proteinG,
        @Schema(description = "每100g碳水") BigDecimal carbsG,
        @Schema(description = "每100g脂肪") BigDecimal fatG,
        @Schema(description = "当前用户是否收藏") boolean favorited) {
}
