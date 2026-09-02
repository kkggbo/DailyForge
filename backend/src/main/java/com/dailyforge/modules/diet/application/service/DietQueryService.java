package com.dailyforge.modules.diet.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.DietFoodLogEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.DietFoodLogMapper;
import com.dailyforge.modules.diet.interfaces.vo.DietLogItemVO;
import com.dailyforge.modules.diet.interfaces.vo.DietSummaryVO;
import com.dailyforge.modules.diet.interfaces.vo.DietSummaryVO.DietProgressVO;
import com.dailyforge.modules.diet.interfaces.vo.DietSummaryVO.NutritionTotals;
import com.dailyforge.modules.diet.interfaces.vo.DietTargetVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DietQueryService {

    private static final List<String> MEAL_ORDER = List.of("breakfast", "lunch", "dinner", "snack");

    private final DietFoodLogMapper logMapper;
    private final DietTargetService dietTargetService;

    public DietQueryService(DietFoodLogMapper logMapper, DietTargetService dietTargetService) {
        this.logMapper = logMapper;
        this.dietTargetService = dietTargetService;
    }

    public DietSummaryVO getDailySummary(LocalDate date) {
        Long userId = com.dailyforge.infrastructure.security.AuthSecurityUtils.getCurrentUserId();
        List<DietFoodLogEntity> logs = logMapper.selectByUserAndDate(userId, date);
        Map<String, List<DietLogItemVO>> meals = new LinkedHashMap<>();
        for (String meal : MEAL_ORDER) {
            meals.put(meal, new ArrayList<>());
        }
        BigDecimal cal = BigDecimal.ZERO, pro = BigDecimal.ZERO, carb = BigDecimal.ZERO, fat = BigDecimal.ZERO;
        for (DietFoodLogEntity log : logs) {
            DietLogItemVO item = toItem(log);
            meals.computeIfAbsent(log.getMealType(), k -> new ArrayList<>()).add(item);
            cal = cal.add(nz(item.caloriesKcal()));
            pro = pro.add(nz(item.proteinG()));
            carb = carb.add(nz(item.carbsG()));
            fat = fat.add(nz(item.fatG()));
        }
        NutritionTotals totals = new NutritionTotals(cal, pro, carb, fat);

        DietTargetVO target = dietTargetService.getTargetForUser(userId);
        DietProgressVO progress = buildProgress(target, totals);
        return new DietSummaryVO(date.toString(), target, meals, totals, progress);
    }

    private DietProgressVO buildProgress(DietTargetVO target, NutritionTotals totals) {
        if (target == null || target.basis() == null) {
            return null;
        }
        return new DietProgressVO(
                pct(totals.caloriesKcal(), target.caloriesKcal()),
                pct(totals.proteinG(), target.proteinG()),
                pct(totals.carbsG(), target.carbsG()),
                pct(totals.fatG(), target.fatG()));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Integer pct(BigDecimal actual, Integer target) {
        if (actual == null || target == null || target <= 0) {
            return null;
        }
        BigDecimal denom = BigDecimal.valueOf(target);
        return actual.multiply(new BigDecimal("100"))
                .divide(denom, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private DietLogItemVO toItem(DietFoodLogEntity e) {
        return new DietLogItemVO(
                e.getId(), e.getFoodId(), e.getFoodNameSnapshot(), e.getQuantityGrams(),
                e.getCaloriesKcal(), e.getProteinG(), e.getCarbsG(), e.getFatG());
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "invalid date");
        }
    }

    public static LocalDate resolveDate(String date) {
        return parseDate(date);
    }
}
