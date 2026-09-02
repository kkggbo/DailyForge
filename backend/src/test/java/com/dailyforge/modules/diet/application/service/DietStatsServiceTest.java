package com.dailyforge.modules.diet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dailyforge.infrastructure.security.AuthUserPrincipal;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.DietFoodLogEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.DietFoodLogMapper;
import com.dailyforge.modules.diet.interfaces.vo.DietStatsVO;
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
class DietStatsServiceTest {

    private static final Long USER_ID = 101L;

    @Mock
    private DietFoodLogMapper logMapper;
    @Mock
    private DietTargetService dietTargetService;

    private DietStatsService service;

    @BeforeEach
    void setUp() {
        service = new DietStatsService(logMapper, dietTargetService);
        SecurityContextHolder.getContext().setAuthentication(authentication());
    }

    @Test
    void getStatsShouldComputeDailyWeeklyMacroAndAdherence() {
        // day1 2000 kcal, day2 1800 kcal (same ISO week starting 2026-08-31)
        DietFoodLogEntity log1 = log(1L, LocalDate.of(2026, 9, 1), "2000", "150", "200", "50");
        DietFoodLogEntity log2 = log(2L, LocalDate.of(2026, 9, 2), "1800", "100", "180", "60");
        when(logMapper.selectByUserAndRange(any(), any(), any())).thenReturn(List.of(log1, log2));
        when(dietTargetService.getTargetForUser(USER_ID))
                .thenReturn(DietTargetVO.auto(new BigDecimal("2000"), new BigDecimal("120"),
                        new BigDecimal("200"), new BigDecimal("70")));

        DietStatsVO vo = service.getStats(null, null);

        // daily
        assertThat(vo.dailyCalories()).hasSize(2);
        // weekly avg = (2000+1800)/2 = 1900 by logged-day count
        assertThat(vo.weeklyAverage()).hasSize(1);
        assertThat(vo.weeklyAverage().getFirst().caloriesKcal()).isEqualByComparingTo("1900.00");
        // macro share non-null
        assertThat(vo.macroShare()).isNotNull();
        // adherence: both days within ±10% of 2000 (1800..2200)
        assertThat(vo.goalAdherence()).isNotNull();
        assertThat(vo.goalAdherence().daysLogged()).isEqualTo(2);
        assertThat(vo.goalAdherence().daysWithinTarget()).isEqualTo(2);
        assertThat(vo.goalAdherence().ratePct()).isEqualTo(100);
    }

    @Test
    void getStatsShouldNullGoalAdherenceWhenNoTarget() {
        DietFoodLogEntity log1 = log(1L, LocalDate.of(2026, 9, 1), "2000", "150", "200", "50");
        when(logMapper.selectByUserAndRange(any(), any(), any())).thenReturn(List.of(log1));
        when(dietTargetService.getTargetForUser(USER_ID))
                .thenReturn(DietTargetVO.none(List.of("gender")));

        DietStatsVO vo = service.getStats(null, null);

        assertThat(vo.goalAdherence()).isNull();
    }

    private DietFoodLogEntity log(Long id, LocalDate date, String cal, String pro, String carb, String fat) {
        DietFoodLogEntity e = new DietFoodLogEntity();
        e.setId(id);
        e.setUserId(USER_ID);
        e.setRecordDate(date);
        e.setCaloriesKcal(new BigDecimal(cal));
        e.setProteinG(new BigDecimal(pro));
        e.setCarbsG(new BigDecimal(carb));
        e.setFatG(new BigDecimal(fat));
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
