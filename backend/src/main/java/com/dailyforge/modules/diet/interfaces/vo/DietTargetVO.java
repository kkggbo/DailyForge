package com.dailyforge.modules.diet.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "每日目标响应（资料不足时 basis=null 且目标字段为 null）")
public record DietTargetVO(
        @Schema(description = "basis", example = "auto") String basis,
        @Schema(description = "热量(kcal)") Integer caloriesKcal,
        @Schema(description = "蛋白质(g)") Integer proteinG,
        @Schema(description = "碳水(g)") Integer carbsG,
        @Schema(description = "脂肪(g)") Integer fatG,
        @Schema(description = "缺失资料字段") List<String> missingFields) {

    public static DietTargetVO none(List<String> missing) {
        return new DietTargetVO(null, null, null, null, null, missing);
    }

    public static DietTargetVO auto(BigDecimal kcal, BigDecimal protein, BigDecimal carbs, BigDecimal fat) {
        return new DietTargetVO(
                "auto", toInt(kcal), toInt(protein), toInt(carbs), toInt(fat), List.of());
    }

    public static DietTargetVO custom(BigDecimal kcal, BigDecimal protein, BigDecimal carbs, BigDecimal fat) {
        return new DietTargetVO(
                "custom", toInt(kcal), toInt(protein), toInt(carbs), toInt(fat), List.of());
    }

    private static Integer toInt(BigDecimal v) {
        return v == null ? null : v.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
    }
}
