package com.dailyforge.modules.diet.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "食物搜索结果（分页）")
public record FoodSearchVO(
        @Schema(description = "当前页食物列表") List<FoodItemVO> foods,
        @Schema(description = "是否还有下一页（用于无限滚动）", example = "true") boolean hasMore) {
}
