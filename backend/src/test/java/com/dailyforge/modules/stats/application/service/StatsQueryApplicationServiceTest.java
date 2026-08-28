package com.dailyforge.modules.stats.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.exercise.application.service.SystemExerciseLookupService;
import com.dailyforge.modules.plan.application.service.PlanUserSupportService;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.BodyMetricLogEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.BodyMetricLogMapper;
import com.dailyforge.modules.stats.application.service.StatsAggregationService.AggregatedWorkout;
import com.dailyforge.modules.stats.interfaces.vo.BodyMetricSeriesResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatsQueryApplicationServiceTest {

    @Mock
    private PlanUserSupportService planUserSupportService;
    @Mock
    private StatsAggregationService statsAggregationService;
    @Mock
    private BodyMetricLogMapper bodyMetricLogMapper;
    @Mock
    private SystemExerciseLookupService systemExerciseLookupService;

    private StatsQueryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new StatsQueryApplicationService(
                planUserSupportService, statsAggregationService, bodyMetricLogMapper, systemExerciseLookupService);
        when(planUserSupportService.requireActiveUserId()).thenReturn(101L);
    }

    @Test
    void getSummaryShouldReturnEmptyWhenNoData() {
        when(statsAggregationService.aggregate(any(), any(), any()))
                .thenReturn(new AggregatedWorkout(0, Map.of()));
        var response = service.getSummary(null, null);
        assertThat(response.overall().sessionCount()).isZero();
        assertThat(response.exercises()).isEmpty();
    }

    @Test
    void getSummaryShouldFilterOutExercisesWithZeroAppearance() {
        // Given: one exercise with actual data and one with appearanceCount 0 (no check-ins).
        StatsAggregationService.ExerciseAggregate withData =
                new StatsAggregationService.ExerciseAggregate(1001L, "卧推", "set_based");
        withData.appearSessionIds.add(1L);
        withData.setCount = 2;
        withData.repsSum = new BigDecimal("14");

        StatsAggregationService.ExerciseAggregate noData =
                new StatsAggregationService.ExerciseAggregate(2001L, "引体向上", "set_based");

        when(statsAggregationService.aggregate(any(), any(), any()))
                .thenReturn(new AggregatedWorkout(1, Map.of(1001L, withData, 2001L, noData)));
        when(systemExerciseLookupService.loadActiveSystemExercisesByIds(any())).thenReturn(Map.of());

        // When
        var response = service.getSummary(null, null);

        // Then: the zero-appearance exercise is excluded from the exercise list.
        assertThat(response.exercises()).hasSize(1);
        assertThat(response.exercises().getFirst().exerciseId()).isEqualTo(1001L);
    }

    @Test
    void getSummaryShouldMapDateOnlyToToEndOfDay() {
        // Given
        when(statsAggregationService.aggregate(any(), any(), any()))
                .thenReturn(new AggregatedWorkout(0, Map.of()));

        // When
        service.getSummary("2026-07-01", "2026-07-05");

        // Then: from stays at start of day; date-only to extends to end of day so the to-day is included.
        ArgumentCaptor<LocalDateTime> fromCaptor = captor();
        ArgumentCaptor<LocalDateTime> toCaptor = captor();
        verify(statsAggregationService)
                .aggregate(any(), fromCaptor.capture(), toCaptor.capture());
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0, 0));
        assertThat(toCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 7, 5, 23, 59, 59, 999_999_000));
    }

    @Test
    void getSummaryShouldPassThroughDatetimeToUnchanged() {
        // Given
        when(statsAggregationService.aggregate(any(), any(), any()))
                .thenReturn(new AggregatedWorkout(0, Map.of()));

        // When
        service.getSummary("2026-07-01T08:30:00", "2026-07-05T18:00:00");

        // Then
        ArgumentCaptor<LocalDateTime> fromCaptor = captor();
        ArgumentCaptor<LocalDateTime> toCaptor = captor();
        verify(statsAggregationService)
                .aggregate(any(), fromCaptor.capture(), toCaptor.capture());
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 7, 1, 8, 30, 0));
        assertThat(toCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 7, 5, 18, 0, 0));
    }

    @Test
    void getBodyMetricsShouldRejectInvalidMetric() {
        assertThatThrownBy(() -> service.getBodyMetrics("unknown_metric", null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void getBodyMetricsShouldPickNewestRowPerDayAndSkipNullValue() {
        // Rows ordered record_date ASC, id DESC: id 200 (newest) then 100 for same day 2026-07-01.
        BodyMetricLogEntity newer = bodyMetricRow(200L, LocalDate.of(2026, 7, 1), "74.8");
        BodyMetricLogEntity older = bodyMetricRow(100L, LocalDate.of(2026, 7, 1), "75.5");
        BodyMetricLogEntity noValue = bodyMetricRow(300L, LocalDate.of(2026, 7, 3), null);
        when(bodyMetricLogMapper.selectActiveRecordsForStats(any(), any(), any()))
                .thenReturn(List.of(newer, older, noValue));

        BodyMetricSeriesResponse response = service.getBodyMetrics("weight_kg", null, null);

        assertThat(response.metric()).isEqualTo("weight_kg");
        assertThat(response.unit()).isEqualTo("kg");
        assertThat(response.points()).hasSize(1);
        assertThat(response.points().getFirst().value()).isEqualByComparingTo("74.8");
        assertThat(response.points().getFirst().date()).isEqualTo("2026-07-01");
    }

    @Test
    void getSummaryShouldIncludeStrengthDurationInOverallAndExercise() {
        // Given: a strength (set_based) exercise with actual duration recorded.
        StatsAggregationService.ExerciseAggregate agg =
                new StatsAggregationService.ExerciseAggregate(1001L, "卧推", "set_based");
        agg.appearSessionIds.add(1L);
        agg.setCount = 2;
        agg.repsSum = new BigDecimal("14");
        agg.totalVolumeKg = new BigDecimal("900");
        agg.totalDurationSeconds = new BigDecimal("2700");
        when(statsAggregationService.aggregate(any(), any(), any()))
                .thenReturn(new AggregatedWorkout(1, Map.of(1001L, agg)));
        when(systemExerciseLookupService.loadActiveSystemExercisesByIds(any())).thenReturn(Map.of());

        // When
        var response = service.getSummary(null, null);

        // Then: strength duration is exposed on the exercise and included in overall duration.
        assertThat(response.exercises().getFirst().totalDurationSeconds())
                .isEqualByComparingTo("2700");
        assertThat(response.exercises().getFirst().setCount()).isEqualTo(2);
        assertThat(response.overall().totalDurationMinutes())
                .isEqualByComparingTo("45");
        assertThat(response.overall().sessionCount()).isEqualTo(1);
    }

    private BodyMetricLogEntity bodyMetricRow(Long id, LocalDate date, String weight) {
        BodyMetricLogEntity e = new BodyMetricLogEntity();
        e.setId(id);
        e.setRecordDate(date);
        e.setWeightKg(weight == null ? null : new BigDecimal(weight));
        e.setIsDel(false);
        return e;
    }
}
