package com.dailyforge.modules.diet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthUserPrincipal;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.DietFoodLogEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.DietFoodLogMapper;
import com.dailyforge.modules.diet.interfaces.vo.DietSummaryVO;
import com.dailyforge.modules.diet.interfaces.vo.DietTargetVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DietQueryServiceTest {

    private static final Long USER_ID = 101L;

    @Mock
    private DietFoodLogMapper logMapper;
    @Mock
    private DietTargetService dietTargetService;

    private DietQueryService service;

    @BeforeEach
    void setUp() {
        service = new DietQueryService(logMapper, dietTargetService);
        SecurityContextHolder.getContext().setAuthentication(authentication());
    }

    @Test
    void resolveDateShouldReturnNowWhenBlank() {
        assertThatCode(() -> DietQueryService.resolveDate(null)).doesNotThrowAnyException();
        assertThatCode(() -> DietQueryService.resolveDate("  ")).doesNotThrowAnyException();
    }

    @Test
    void resolveDateShouldParseValidDate() {
        LocalDate parsed = DietQueryService.resolveDate("2026-09-03");
        assertThat(parsed).isEqualTo(LocalDate.of(2026, 9, 3));
    }

    @Test
    void resolveDateShouldRejectIllegalDate() {
        assertThatThrownBy(() -> DietQueryService.resolveDate("not-a-date"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);
        assertThatThrownBy(() -> DietQueryService.resolveDate("2026-13-40"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }

    @Test
    void dailySummaryShouldReturnNullTargetWhenProfileIncomplete() {
        when(logMapper.selectByUserAndDate(USER_ID, LocalDate.of(2026, 9, 3)))
                .thenReturn(List.of());
        when(dietTargetService.getTargetForUser(USER_ID))
                .thenReturn(DietTargetVO.none(List.of("activityLevel")));

        DietSummaryVO summary = service.getDailySummary(LocalDate.of(2026, 9, 3));

        assertThat(summary.target()).isNull();
        assertThat(summary.progress()).isNull();
        assertThat(summary.totals().caloriesKcal()).isEqualByComparingTo("0");
    }

    @Test
    void dailySummaryShouldIncludeTargetAndProgressWhenProfileComplete() {
        when(logMapper.selectByUserAndDate(USER_ID, LocalDate.of(2026, 9, 3)))
                .thenReturn(List.of(logEntity(new BigDecimal("660"))));
        when(dietTargetService.getTargetForUser(USER_ID))
                .thenReturn(DietTargetVO.auto(
                        new BigDecimal("2200"), new BigDecimal("150"), new BigDecimal("250"), new BigDecimal("73")));

        DietSummaryVO summary = service.getDailySummary(LocalDate.of(2026, 9, 3));

        assertThat(summary.target()).isNotNull();
        assertThat(summary.target().basis()).isEqualTo("auto");
        assertThat(summary.target().caloriesKcal()).isEqualTo(2200);
        assertThat(summary.progress()).isNotNull();
        assertThat(summary.progress().caloriesPct()).isEqualTo(30);
    }

    @Test
    void dailySummaryShouldKeepCustomTargetEvenWhenProfileIncomplete() {
        when(logMapper.selectByUserAndDate(USER_ID, LocalDate.of(2026, 9, 3)))
                .thenReturn(List.of());
        // 自定义目标优先于资料完整度：资料不足也返回 custom 目标
        when(dietTargetService.getTargetForUser(USER_ID))
                .thenReturn(DietTargetVO.custom(
                        new BigDecimal("1800"), new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("60")));

        DietSummaryVO summary = service.getDailySummary(LocalDate.of(2026, 9, 3));

        assertThat(summary.target()).isNotNull();
        assertThat(summary.target().basis()).isEqualTo("custom");
        assertThat(summary.progress()).isNotNull();
    }

    private DietFoodLogEntity logEntity(BigDecimal calories) {
        DietFoodLogEntity e = new DietFoodLogEntity();
        e.setId(1L);
        e.setUserId(USER_ID);
        e.setFoodId(100L);
        e.setFoodNameSnapshot("鸡胸肉");
        e.setMealType("lunch");
        e.setRecordDate(LocalDate.of(2026, 9, 3));
        e.setQuantityGrams(new BigDecimal("200"));
        e.setCaloriesKcal(calories);
        e.setProteinG(new BigDecimal("62"));
        e.setCarbsG(BigDecimal.ZERO);
        e.setFatG(new BigDecimal("7"));
        return e;
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
