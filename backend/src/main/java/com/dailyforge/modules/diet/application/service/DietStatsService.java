package com.dailyforge.modules.diet.application.service;

import com.dailyforge.modules.diet.infrastructure.persistence.entity.DietFoodLogEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.DietFoodLogMapper;
import com.dailyforge.modules.diet.interfaces.vo.DietStatsVO;
import com.dailyforge.modules.diet.interfaces.vo.DietStatsVO.DailyCalories;
import com.dailyforge.modules.diet.interfaces.vo.DietStatsVO.GoalAdherence;
import com.dailyforge.modules.diet.interfaces.vo.DietStatsVO.MacroShare;
import com.dailyforge.modules.diet.interfaces.vo.DietStatsVO.WeeklyAverage;
import com.dailyforge.modules.diet.interfaces.vo.DietTargetVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class DietStatsService {

    private static final BigDecimal FOUR = new BigDecimal("4");
    private static final BigDecimal NINE = new BigDecimal("9");

    private final DietFoodLogMapper logMapper;
    private final DietTargetService dietTargetService;

    public DietStatsService(DietFoodLogMapper logMapper, DietTargetService dietTargetService) {
        this.logMapper = logMapper;
        this.dietTargetService = dietTargetService;
    }

    public DietStatsVO getStats(LocalDate from, LocalDate to) {
        Long userId = com.dailyforge.infrastructure.security.AuthSecurityUtils.getCurrentUserId();
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(6) : from;

        List<DietFoodLogEntity> logs = logMapper.selectByUserAndRange(userId, start, end);

        // Daily per-date totals
        Map<LocalDate, BigDecimal> dailyCal = new TreeMap<>();
        Map<LocalDate, BigDecimal[]> dailyMacro = new TreeMap<>();
        for (DietFoodLogEntity log : logs) {
            LocalDate d = log.getRecordDate();
            dailyCal.merge(d, nz(log.getCaloriesKcal()), BigDecimal::add);
            BigDecimal[] m = dailyMacro.computeIfAbsent(d, k -> new BigDecimal[] {
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            m[0] = m[0].add(nz(log.getProteinG()));
            m[1] = m[1].add(nz(log.getCarbsG()));
            m[2] = m[2].add(nz(log.getFatG()));
            m[3] = m[3].add(nz(log.getCaloriesKcal()));
        }

        List<DailyCalories> dailyCalories = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : dailyCal.entrySet()) {
            dailyCalories.add(new DailyCalories(e.getKey().toString(), e.getValue()));
        }

        MacroShare macroShare = computeMacroShare(dailyMacro);

        List<WeeklyAverage> weekly = computeWeekly(dailyMacro);

        GoalAdherence adherence = computeAdherence(userId, dailyCal);

        return new DietStatsVO(dailyCalories, macroShare, weekly, adherence);
    }

    private MacroShare computeMacroShare(Map<LocalDate, BigDecimal[]> dailyMacro) {
        BigDecimal pro = BigDecimal.ZERO, carb = BigDecimal.ZERO, fat = BigDecimal.ZERO, cal = BigDecimal.ZERO;
        for (BigDecimal[] m : dailyMacro.values()) {
            pro = pro.add(m[0]);
            carb = carb.add(m[1]);
            fat = fat.add(m[2]);
            cal = cal.add(m[3]);
        }
        if (cal.signum() <= 0) {
            return new MacroShare(0, 0, 0);
        }
        int proteinPct = pro.multiply(FOUR).multiply(new BigDecimal("100")).divide(cal, 0, RoundingMode.HALF_UP).intValue();
        int carbsPct = carb.multiply(FOUR).multiply(new BigDecimal("100")).divide(cal, 0, RoundingMode.HALF_UP).intValue();
        int fatPct = fat.multiply(NINE).multiply(new BigDecimal("100")).divide(cal, 0, RoundingMode.HALF_UP).intValue();
        return new MacroShare(proteinPct, carbsPct, fatPct);
    }

    private List<WeeklyAverage> computeWeekly(Map<LocalDate, BigDecimal[]> dailyMacro) {
        Map<LocalDate, List<BigDecimal[]>> byWeek = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, BigDecimal[]> e : dailyMacro.entrySet()) {
            LocalDate weekStart = e.getKey().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            byWeek.computeIfAbsent(weekStart, k -> new ArrayList<>()).add(e.getValue());
        }
        List<WeeklyAverage> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<BigDecimal[]>> e : byWeek.entrySet()) {
            BigDecimal cal = BigDecimal.ZERO, pro = BigDecimal.ZERO, carb = BigDecimal.ZERO, fat = BigDecimal.ZERO;
            int days = e.getValue().size();
            for (BigDecimal[] m : e.getValue()) {
                cal = cal.add(m[3]);
                pro = pro.add(m[0]);
                carb = carb.add(m[1]);
                fat = fat.add(m[2]);
            }
            int denom = Math.max(1, days);
            result.add(new WeeklyAverage(
                    e.getKey().toString(),
                    cal.divide(BigDecimal.valueOf(denom), 2, RoundingMode.HALF_UP),
                    pro.divide(BigDecimal.valueOf(denom), 2, RoundingMode.HALF_UP),
                    carb.divide(BigDecimal.valueOf(denom), 2, RoundingMode.HALF_UP),
                    fat.divide(BigDecimal.valueOf(denom), 2, RoundingMode.HALF_UP)));
        }
        return result;
    }

    private GoalAdherence computeAdherence(Long userId, Map<LocalDate, BigDecimal> dailyCal) {
        DietTargetVO target = dietTargetService.getTargetForUser(userId);
        if (target == null || target.basis() == null || target.caloriesKcal() == null) {
            return null;
        }
        double goal = target.caloriesKcal();
        int within = 0;
        for (BigDecimal cal : dailyCal.values()) {
            double c = cal.doubleValue();
            if (c >= goal * 0.9 && c <= goal * 1.1) {
                within++;
            }
        }
        int logged = dailyCal.size();
        int rate = logged == 0 ? 0 : (int) Math.round(within * 100.0 / logged);
        return new GoalAdherence(within, logged, rate);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
