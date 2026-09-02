package com.dailyforge.modules.diet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dailyforge.infrastructure.security.AuthUserPrincipal;
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
class DietTargetServiceTest {

    private static final Long USER_ID = 101L;

    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private UserCurrentBodyMetricsMapper metricsMapper;
    @Mock
    private UserDietTargetMapper targetMapper;
    @Mock
    private DietTargetDomainService domainService;

    private DietTargetService service;

    @BeforeEach
    void setUp() {
        service = new DietTargetService(userProfileMapper, metricsMapper, targetMapper, domainService);
        SecurityContextHolder.getContext().setAuthentication(authentication());
    }

    @Test
    void getTargetShouldReturnCustomWhenOverrideExists() {
        UserDietTargetEntity custom = new UserDietTargetEntity();
        custom.setUserId(USER_ID);
        custom.setCaloriesKcal(new BigDecimal("2000"));
        custom.setProteinG(new BigDecimal("120"));
        custom.setCarbsG(new BigDecimal("220"));
        custom.setFatG(new BigDecimal("60"));
        when(targetMapper.selectById(USER_ID)).thenReturn(custom);

        DietTargetVO vo = service.getTarget();

        assertThat(vo.basis()).isEqualTo("custom");
        assertThat(vo.caloriesKcal()).isEqualTo(2000);
    }

    @Test
    void getTargetShouldReturnAutoWhenNoOverride() {
        when(targetMapper.selectById(USER_ID)).thenReturn(null);
        UserProfileEntity profile = new UserProfileEntity();
        profile.setGender("male");
        profile.setBirthDate(LocalDate.of(1996, 1, 1));
        profile.setHeightCm(new BigDecimal("178"));
        profile.setGoalType("health_maintenance");
        profile.setActivityLevel("moderate");
        when(userProfileMapper.selectById(USER_ID)).thenReturn(profile);
        UserCurrentBodyMetricsEntity metrics = new UserCurrentBodyMetricsEntity();
        metrics.setCurrentWeightKg(new BigDecimal("80"));
        when(metricsMapper.selectById(USER_ID)).thenReturn(metrics);
        when(domainService.compute(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ComputedTarget(true, List.of(),
                        new BigDecimal("2200"), new BigDecimal("120"), new BigDecimal("250"), new BigDecimal("73")));

        DietTargetVO vo = service.getTarget();

        assertThat(vo.basis()).isEqualTo("auto");
        assertThat(vo.caloriesKcal()).isEqualTo(2200);
    }

    @Test
    void overrideTargetShouldUpsertCustom() {
        when(targetMapper.selectById(USER_ID)).thenReturn(null);

        DietTargetVO vo = service.overrideTarget(new OverrideTargetRequest(
                new BigDecimal("2000"), new BigDecimal("120"), new BigDecimal("220"), new BigDecimal("60"), false));

        assertThat(vo.basis()).isEqualTo("custom");
        assertThat(vo.caloriesKcal()).isEqualTo(2000);
        verify(targetMapper).insert(any(UserDietTargetEntity.class));
    }

    @Test
    void clearTargetShouldDeleteAndReturnAuto() {
        when(targetMapper.selectById(USER_ID)).thenReturn(null).thenReturn(null);
        UserProfileEntity profile = new UserProfileEntity();
        profile.setGender("male");
        when(userProfileMapper.selectById(USER_ID)).thenReturn(profile);
        when(metricsMapper.selectById(USER_ID)).thenReturn(null);
        when(domainService.compute(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ComputedTarget(false, List.of("activityLevel"), null, null, null, null));

        DietTargetVO vo = service.overrideTarget(new OverrideTargetRequest(null, null, null, null, true));

        verify(targetMapper).deleteById(USER_ID);
        assertThat(vo.basis()).isNull();
        assertThat(vo.missingFields()).contains("activityLevel");
    }

    @Test
    void clearTargetWithNoNutritionFieldsShouldReturnAutoWhenProfileComplete() {
        // clear=true and no nutrition fields present (frontend sends only {clear:true}).
        when(targetMapper.selectById(USER_ID)).thenReturn(null).thenReturn(null);
        UserProfileEntity profile = new UserProfileEntity();
        profile.setGender("male");
        profile.setBirthDate(LocalDate.of(1996, 1, 1));
        profile.setHeightCm(new BigDecimal("178"));
        profile.setGoalType("health_maintenance");
        profile.setActivityLevel("moderate");
        when(userProfileMapper.selectById(USER_ID)).thenReturn(profile);
        UserCurrentBodyMetricsEntity metrics = new UserCurrentBodyMetricsEntity();
        metrics.setCurrentWeightKg(new BigDecimal("80"));
        when(metricsMapper.selectById(USER_ID)).thenReturn(metrics);
        when(domainService.compute(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ComputedTarget(true, List.of(),
                        new BigDecimal("2200"), new BigDecimal("120"), new BigDecimal("250"), new BigDecimal("73")));

        DietTargetVO vo = service.overrideTarget(new OverrideTargetRequest(null, null, null, null, true));

        verify(targetMapper).deleteById(USER_ID);
        assertThat(vo.basis()).isEqualTo("auto");
        assertThat(vo.caloriesKcal()).isEqualTo(2200);
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
