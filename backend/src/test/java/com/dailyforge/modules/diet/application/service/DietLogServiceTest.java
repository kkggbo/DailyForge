package com.dailyforge.modules.diet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthUserPrincipal;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.DietFoodLogEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.FoodEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.DietFoodLogMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.FoodMapper;
import com.dailyforge.modules.diet.interfaces.dto.CreateDietLogRequest;
import com.dailyforge.modules.diet.interfaces.vo.DietLogItemVO;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DietLogServiceTest {

    private static final Long USER_ID = 101L;

    @Mock
    private DietFoodLogMapper logMapper;
    @Mock
    private FoodMapper foodMapper;

    private DietLogService service;

    @BeforeEach
    void setUp() {
        service = new DietLogService(logMapper, foodMapper);
        SecurityContextHolder.getContext().setAuthentication(authentication());
    }

    @Test
    void addLogShouldComputeSnapshotFromPer100AndGrams() {
        FoodEntity food = food();
        when(foodMapper.selectActiveById(100L)).thenReturn(food);

        DietLogItemVO item = service.addLog(new CreateDietLogRequest(
                "2026-09-03", "lunch", 100L, new BigDecimal("200")));

        // per100: 165kcal/31/0/3.6 *200/100 = 330/62/0/7.2
        assertThat(item.foodName()).isEqualTo("鸡胸肉");
        assertThat(item.caloriesKcal()).isEqualByComparingTo("330.00");
        assertThat(item.proteinG()).isEqualByComparingTo("62.00");
        assertThat(item.carbsG()).isEqualByComparingTo("0.00");
        assertThat(item.fatG()).isEqualByComparingTo("7.20");
    }

    @Test
    void addLogShouldRejectFoodNotFound() {
        when(foodMapper.selectActiveById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.addLog(new CreateDietLogRequest(
                "2026-09-03", "lunch", 999L, new BigDecimal("100"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FOOD_NOT_FOUND);
    }

    @Test
    void addLogShouldRejectOversizeGrams() {
        assertThatThrownBy(() -> service.addLog(new CreateDietLogRequest(
                "2026-09-03", "lunch", 100L, new BigDecimal("6000"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DIET_LOG_INVALID);
    }

    @Test
    void updateLogShouldRejectWhenLogBelongsToAnotherUser() {
        DietFoodLogEntity otherLog = new DietFoodLogEntity();
        otherLog.setId(55L);
        otherLog.setUserId(999L); // different user
        when(logMapper.selectById(55L)).thenReturn(otherLog);

        assertThatThrownBy(() -> service.updateLog(55L, new CreateDietLogRequest(
                "2026-09-03", "lunch", 100L, new BigDecimal("200"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void deleteLogShouldRejectWhenLogBelongsToAnotherUser() {
        DietFoodLogEntity otherLog = new DietFoodLogEntity();
        otherLog.setId(56L);
        otherLog.setUserId(999L); // different user
        when(logMapper.selectById(56L)).thenReturn(otherLog);

        assertThatThrownBy(() -> service.deleteLog(56L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private FoodEntity food() {
        FoodEntity f = new FoodEntity();
        f.setId(100L);
        f.setName("鸡胸肉");
        f.setCaloriesKcal(new BigDecimal("165"));
        f.setProteinG(new BigDecimal("31"));
        f.setCarbsG(BigDecimal.ZERO);
        f.setFatG(new BigDecimal("3.6"));
        f.setSource("system");
        return f;
    }

    private Authentication authentication() {
        return new Authentication() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getName() {
                return String.valueOf(USER_ID);
            }

            @Override
            public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return java.util.List.of();
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return new AuthUserPrincipal(USER_ID, "u@example.com", "user", "basic");
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }
        };
    }
}
