package com.dailyforge.modules.profile.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProfileCompletionDomainServiceTest {

    private final ProfileCompletionDomainService profileCompletionDomainService = new ProfileCompletionDomainService();

    @Test
    void summarizeShouldMarkEmptyProfileAsNotReady() {
        UserProfileEntity profile = new UserProfileEntity();

        ProfileCompletionDomainService.CompletionSummary summary =
                profileCompletionDomainService.summarize(profile, null);

        assertThat(summary.basicProfileReady()).isFalse();
        assertThat(summary.hasWeightRecord()).isFalse();
        assertThat(summary.missingBasicProfileFields())
                .containsExactly("gender", "birthDate", "heightCm", "goalType");
        assertThat(summary.aiPlanMissingFields()).contains("weightRecord");
    }

    @Test
    void summarizeShouldMarkAiReadyWhenRequiredFieldsAndWeightExist() {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setGender("male");
        profile.setBirthDate(LocalDate.of(1998, 6, 15));
        profile.setHeightCm(new BigDecimal("178.00"));
        profile.setGoalType("fat_loss");

        UserCurrentBodyMetricsEntity snapshot = new UserCurrentBodyMetricsEntity();
        snapshot.setCurrentWeightKg(new BigDecimal("76.50"));

        ProfileCompletionDomainService.CompletionSummary summary =
                profileCompletionDomainService.summarize(profile, snapshot);

        assertThat(summary.basicProfileReady()).isTrue();
        assertThat(summary.hasWeightRecord()).isTrue();
        assertThat(summary.aiPlanReady()).isTrue();
        assertThat(summary.aiNutritionReady()).isTrue();
        assertThat(summary.aiSummaryReady()).isTrue();
        assertThat(summary.aiPlanMissingFields()).isEmpty();
    }
}
