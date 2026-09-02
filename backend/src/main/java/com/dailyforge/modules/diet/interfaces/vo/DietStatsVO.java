package com.dailyforge.modules.diet.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "饮食摄入统计")
public record DietStatsVO(
        @Schema(description = "每日热量") List<DailyCalories> dailyCalories,
        @Schema(description = "宏量供能占比") MacroShare macroShare,
        @Schema(description = "周均值") List<WeeklyAverage> weeklyAverage,
        @Schema(description = "目标符合度(无目标时为 null)") GoalAdherence goalAdherence) {

    @Schema(description = "每日热量")
    public record DailyCalories(String date, BigDecimal caloriesKcal) {
    }

    @Schema(description = "宏量供能占比(%)")
    public record MacroShare(Integer proteinPct, Integer carbsPct, Integer fatPct) {
    }

    @Schema(description = "周均值：按该周有记录的天数平均（日均摄入），天数为该周有饮食记录的天数")
    public record WeeklyAverage(
            String weekStart, BigDecimal caloriesKcal,
            BigDecimal proteinG, BigDecimal carbsG, BigDecimal fatG) {
    }

    @Schema(description = "目标符合度")
    public record GoalAdherence(
            Integer daysWithinTarget, Integer daysLogged, Integer ratePct) {
    }
}
