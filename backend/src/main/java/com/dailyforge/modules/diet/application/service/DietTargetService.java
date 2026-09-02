package com.dailyforge.modules.diet.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthSecurityUtils;
import com.dailyforge.modules.diet.domain.service.DietTargetDomainService;
import com.dailyforge.modules.diet.domain.service.DietTargetDomainService.ComputedTarget;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.UserDietTargetEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.UserDietTargetMapper;
import com.dailyforge.modules.diet.interfaces.dto.OverrideTargetRequest;
import com.dailyforge.modules.diet.interfaces.vo.DietTargetVO;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserCurrentBodyMetricsMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserProfileMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DietTargetService {

    private final UserProfileMapper userProfileMapper;
    private final UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper;
    private final UserDietTargetMapper userDietTargetMapper;
    private final DietTargetDomainService dietTargetDomainService;

    public DietTargetService(
            UserProfileMapper userProfileMapper,
            UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper,
            UserDietTargetMapper userDietTargetMapper,
            DietTargetDomainService dietTargetDomainService) {
        this.userProfileMapper = userProfileMapper;
        this.userCurrentBodyMetricsMapper = userCurrentBodyMetricsMapper;
        this.userDietTargetMapper = userDietTargetMapper;
        this.dietTargetDomainService = dietTargetDomainService;
    }

    /**
     * Return the effective daily target: custom override if present, otherwise auto-computed.
     */
    public DietTargetVO getTarget() {
        return getTargetForUser(AuthSecurityUtils.getCurrentUserId());
    }

    /**
     * Effective daily target for a given user: custom override if present, otherwise auto-computed.
     */
    public DietTargetVO getTargetForUser(Long userId) {
        UserDietTargetEntity custom = userDietTargetMapper.selectById(userId);
        if (custom != null) {
            return DietTargetVO.custom(
                    custom.getCaloriesKcal(), custom.getProteinG(), custom.getCarbsG(), custom.getFatG());
        }
        ComputedTarget auto = computeAuto(userId);
        if (!auto.complete()) {
            return DietTargetVO.none(auto.missingFields());
        }
        return DietTargetVO.auto(auto.caloriesKcal(), auto.proteinG(), auto.carbsG(), auto.fatG());
    }

    /**
     * Override (upsert) or clear the custom target.
     */
    @Transactional
    public DietTargetVO overrideTarget(OverrideTargetRequest request) {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        if (Boolean.TRUE.equals(request.clear())) {
            userDietTargetMapper.deleteById(userId);
            return getTarget();
        }
        if (request.caloriesKcal() == null || request.proteinG() == null
                || request.carbsG() == null || request.fatG() == null) {
            throw new BusinessException(ErrorCode.DIET_TARGET_INVALID);
        }
        UserDietTargetEntity entity = new UserDietTargetEntity();
        entity.setUserId(userId);
        entity.setCaloriesKcal(request.caloriesKcal());
        entity.setProteinG(request.proteinG());
        entity.setCarbsG(request.carbsG());
        entity.setFatG(request.fatG());
        if (userDietTargetMapper.selectById(userId) == null) {
            userDietTargetMapper.insert(entity);
        } else {
            userDietTargetMapper.updateById(entity);
        }
        return DietTargetVO.custom(
                entity.getCaloriesKcal(), entity.getProteinG(), entity.getCarbsG(), entity.getFatG());
    }

    private ComputedTarget computeAuto(Long userId) {
        UserProfileEntity profile = userProfileMapper.selectById(userId);
        UserCurrentBodyMetricsEntity metrics = userCurrentBodyMetricsMapper.selectById(userId);
        String gender = profile == null ? null : profile.getGender();
        var birth = profile == null ? null : profile.getBirthDate();
        BigDecimal height = profile == null ? null : profile.getHeightCm();
        String goal = profile == null ? null : profile.getGoalType();
        String activity = profile == null ? null : profile.getActivityLevel();
        BigDecimal weight = metrics == null ? null : metrics.getCurrentWeightKg();
        return dietTargetDomainService.compute(gender, birth, height, weight, goal, activity);
    }
}
