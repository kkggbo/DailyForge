package com.dailyforge.modules.diet.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dailyforge.modules.diet.domain.service.DietTargetDomainService.ComputedTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DietTargetDomainServiceTest {

    private DietTargetDomainService service;

    @BeforeEach
    void setUp() {
        service = new DietTargetDomainService();
    }

    @Test
    void shouldComputeMaleMaintenanceTarget() {
        ComputedTarget t = service.compute(
                "male", LocalDate.of(1996, 1, 1), new BigDecimal("178"),
                new BigDecimal("80"), "health_maintenance", "moderate");
        assertThat(t.complete()).isTrue();
        assertThat(t.missingFields()).isEmpty();
        // BMR=1767.5, TDEE=1767.5*1.55=2739.625
        assertThat(t.caloriesKcal()).isEqualByComparingTo("2739.625");
        assertThat(t.proteinG()).isEqualByComparingTo("128");
        assertThat(t.fatG()).isEqualByComparingTo("76");
        assertThat(t.carbsG()).isEqualByComparingTo("386");
    }

    @Test
    void shouldApplyGoalAdjustmentFatLossAndMuscleGain() {
        ComputedTarget maintain = service.compute(
                "female", LocalDate.of(1996, 1, 1), new BigDecimal("160"),
                new BigDecimal("60"), "health_maintenance", "light");
        ComputedTarget fat = service.compute(
                "female", LocalDate.of(1996, 1, 1), new BigDecimal("160"),
                new BigDecimal("60"), "fat_loss", "light");
        ComputedTarget gain = service.compute(
                "female", LocalDate.of(1996, 1, 1), new BigDecimal("160"),
                new BigDecimal("60"), "muscle_gain", "light");
        assertThat(fat.caloriesKcal()).isLessThan(maintain.caloriesKcal());
        assertThat(gain.caloriesKcal()).isGreaterThan(maintain.caloriesKcal());
    }

    @Test
    void shouldEnforceKcalFloor() {
        ComputedTarget t = service.compute(
                "female", LocalDate.of(1970, 1, 1), new BigDecimal("150"),
                new BigDecimal("40"), "fat_loss", "sedentary");
        // raw target ~893 < 1200 -> floored to 1200
        assertThat(t.caloriesKcal()).isEqualByComparingTo("1200");
    }

    @Test
    void shouldReportMissingFieldsWhenIncomplete() {
        ComputedTarget t = service.compute(
                null, null, null, null, null, null);
        assertThat(t.complete()).isFalse();
        assertThat(t.missingFields())
                .containsExactlyInAnyOrder("gender", "birthDate", "heightCm", "currentWeightKg", "goalType", "activityLevel");
        assertThat(t.caloriesKcal()).isNull();
    }

    @Test
    void shouldReportOnlyMissingActivityLevelWhenOthersPresent() {
        ComputedTarget t = service.compute(
                "male", LocalDate.of(1996, 1, 1), new BigDecimal("178"),
                new BigDecimal("80"), "health_maintenance", null);
        assertThat(t.complete()).isFalse();
        assertThat(t.missingFields()).containsExactly("activityLevel");
    }
}
