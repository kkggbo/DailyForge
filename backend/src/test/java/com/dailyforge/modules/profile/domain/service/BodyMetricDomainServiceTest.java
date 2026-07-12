package com.dailyforge.modules.profile.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dailyforge.modules.profile.infrastructure.persistence.entity.BodyMetricLogEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.interfaces.dto.CreateBodyMetricRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BodyMetricDomainServiceTest {

    private final BodyMetricDomainService bodyMetricDomainService = new BodyMetricDomainService();

    @Test
    void collectEffectiveMetricFieldsShouldIgnoreNoteOnlyRequest() {
        CreateBodyMetricRequest request = new CreateBodyMetricRequest(
                LocalDate.of(2026, 7, 12),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "note only");

        assertThat(bodyMetricDomainService.collectEffectiveMetricFields(request)).isEmpty();
    }

    @Test
    void collectEffectiveMetricFieldsShouldReturnNonNullMetricNames() {
        CreateBodyMetricRequest request = new CreateBodyMetricRequest(
                LocalDate.of(2026, 7, 12),
                new BigDecimal("76.50"),
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("82.00"),
                null,
                null,
                null,
                "healthy",
                null);

        assertThat(bodyMetricDomainService.collectEffectiveMetricFields(request))
                .containsExactly("weightKg", "waistCm", "bodyType");
    }

    @Test
    void rebuildSnapshotShouldPickLatestNonNullValuePerField() {
        BodyMetricLogEntity latest = record(2L, LocalDate.of(2026, 7, 12), null, new BigDecimal("18.20"), null);
        BodyMetricLogEntity older = record(1L, LocalDate.of(2026, 7, 11), new BigDecimal("80.00"), null, new BigDecimal("82.00"));

        UserCurrentBodyMetricsEntity snapshot =
                bodyMetricDomainService.rebuildSnapshot(99L, List.of(latest, older));

        assertThat(snapshot.getUserId()).isEqualTo(99L);
        assertThat(snapshot.getCurrentWeightKg()).isEqualByComparingTo("80.00");
        assertThat(snapshot.getCurrentBodyFatPercent()).isEqualByComparingTo("18.20");
        assertThat(snapshot.getCurrentWaistCm()).isEqualByComparingTo("82.00");
    }

    @Test
    void mergeIntoSnapshotShouldOnlyOverrideFieldsPresentInRequest() {
        UserCurrentBodyMetricsEntity existing = new UserCurrentBodyMetricsEntity();
        existing.setUserId(99L);
        existing.setCurrentWeightKg(new BigDecimal("80.00"));
        existing.setCurrentWaistCm(new BigDecimal("81.00"));

        CreateBodyMetricRequest request = new CreateBodyMetricRequest(
                LocalDate.of(2026, 7, 12),
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("82.00"),
                null,
                null,
                null,
                null,
                null);

        UserCurrentBodyMetricsEntity merged =
                bodyMetricDomainService.mergeIntoSnapshot(99L, existing, request);

        assertThat(merged.getCurrentWeightKg()).isEqualByComparingTo("80.00");
        assertThat(merged.getCurrentWaistCm()).isEqualByComparingTo("82.00");
    }

    private BodyMetricLogEntity record(
            Long id,
            LocalDate recordDate,
            BigDecimal weightKg,
            BigDecimal bodyFatPercent,
            BigDecimal waistCm) {
        BodyMetricLogEntity entity = new BodyMetricLogEntity();
        entity.setId(id);
        entity.setRecordDate(recordDate);
        entity.setWeightKg(weightKg);
        entity.setBodyFatPercent(bodyFatPercent);
        entity.setWaistCm(waistCm);
        return entity;
    }
}
