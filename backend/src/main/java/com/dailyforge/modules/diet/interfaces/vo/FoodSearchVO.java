package com.dailyforge.modules.diet.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "食物搜索结果")
public record FoodSearchVO(
        @Schema(description = "食物列表") List<FoodItemVO> foods) {
}
