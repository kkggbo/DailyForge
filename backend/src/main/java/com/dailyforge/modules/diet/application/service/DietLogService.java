package com.dailyforge.modules.diet.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthSecurityUtils;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.DietFoodLogEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.FoodEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.DietFoodLogMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.FoodMapper;
import com.dailyforge.modules.diet.interfaces.dto.CreateDietLogRequest;
import com.dailyforge.modules.diet.interfaces.vo.DietLogItemVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DietLogService {

    private static final BigDecimal GRAMS_MAX = new BigDecimal("5000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final DietFoodLogMapper logMapper;
    private final FoodMapper foodMapper;

    public DietLogService(DietFoodLogMapper logMapper, FoodMapper foodMapper) {
        this.logMapper = logMapper;
        this.foodMapper = foodMapper;
    }

    @Transactional
    public DietLogItemVO addLog(CreateDietLogRequest request) {
        Long userId = com.dailyforge.infrastructure.security.AuthSecurityUtils.getCurrentUserId();
        validateDate(request.date());
        validateGrams(request.grams());
        FoodEntity food = requireFood(request.foodId());
        DietFoodLogEntity entity = buildLogEntity(userId, request.date(), request.mealType(), food, request.grams());
        logMapper.insert(entity);
        return toItem(entity);
    }

    @Transactional
    public DietLogItemVO updateLog(Long logId, CreateDietLogRequest request) {
        Long userId = com.dailyforge.infrastructure.security.AuthSecurityUtils.getCurrentUserId();
        validateDate(request.date());
        validateGrams(request.grams());
        requireOwnedLog(userId, logId);
        FoodEntity food = requireFood(request.foodId());
        DietFoodLogEntity entity = buildLogEntity(userId, request.date(), request.mealType(), food, request.grams());
        entity.setId(logId);
        logMapper.updateById(entity);
        return toItem(entity);
    }

    @Transactional
    public void deleteLog(Long logId) {
        Long userId = com.dailyforge.infrastructure.security.AuthSecurityUtils.getCurrentUserId();
        DietFoodLogEntity existing = requireOwnedLog(userId, logId);
        logMapper.deleteById(existing.getId());
    }

    private DietFoodLogEntity buildLogEntity(
            Long userId, String date, String mealType, FoodEntity food, BigDecimal grams) {
        DietFoodLogEntity entity = new DietFoodLogEntity();
        entity.setUserId(userId);
        entity.setFoodId(food.getId());
        entity.setFoodNameSnapshot(food.getName());
        entity.setMealType(mealType);
        entity.setRecordDate(LocalDate.parse(date));
        entity.setQuantityGrams(grams);
        // snapshot = per100 * grams / 100
        entity.setCaloriesKcal(nutrition(food.getCaloriesKcal(), grams));
        entity.setProteinG(nutrition(food.getProteinG(), grams));
        entity.setCarbsG(nutrition(food.getCarbsG(), grams));
        entity.setFatG(nutrition(food.getFatG(), grams));
        return entity;
    }

    private BigDecimal nutrition(BigDecimal per100, BigDecimal grams) {
        if (per100 == null) {
            return BigDecimal.ZERO;
        }
        return per100.multiply(grams).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private FoodEntity requireFood(Long foodId) {
        FoodEntity food = foodMapper.selectActiveById(foodId);
        if (food == null) {
            throw new BusinessException(ErrorCode.FOOD_NOT_FOUND);
        }
        return food;
    }

    private DietFoodLogEntity requireOwnedLog(Long userId, Long logId) {
        DietFoodLogEntity log = logMapper.selectById(logId);
        if (log == null || !log.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return log;
    }

    private void validateDate(String date) {
        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new BusinessException(ErrorCode.DIET_LOG_INVALID);
        }
    }

    private void validateGrams(BigDecimal grams) {
        if (grams == null || grams.signum() <= 0 || grams.compareTo(GRAMS_MAX) > 0) {
            throw new BusinessException(ErrorCode.DIET_LOG_INVALID);
        }
    }

    private DietLogItemVO toItem(DietFoodLogEntity e) {
        return new DietLogItemVO(
                e.getId(),
                e.getFoodId(),
                e.getFoodNameSnapshot(),
                e.getQuantityGrams(),
                e.getCaloriesKcal(),
                e.getProteinG(),
                e.getCarbsG(),
                e.getFatG());
    }
}
