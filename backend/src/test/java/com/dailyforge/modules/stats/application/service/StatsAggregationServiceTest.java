package com.dailyforge.modules.stats.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dailyforge.modules.stats.application.service.StatsAggregationService.AggregatedWorkout;
import com.dailyforge.modules.stats.application.service.StatsAggregationService.ExerciseAggregate;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemMetricEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMetricMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatsAggregationServiceTest {

    @Mock
    private TrainingSessionMapper trainingSessionMapper;
    @Mock
    private TrainingSessionExerciseMapper exerciseMapper;
    @Mock
    private TrainingSessionExerciseItemMapper itemMapper;
    @Mock
    private TrainingSessionExerciseItemMetricMapper metricMapper;

    private StatsAggregationService service;

    @BeforeEach
    void setUp() {
        service = new StatsAggregationService(
                trainingSessionMapper, exerciseMapper, itemMapper, metricMapper);
    }

    @Test
    void aggregateShouldComputeStrengthAndCardioFieldsCorrectly() {
        // Given: session 1 and 2 both have bench press with actual data; session 1 also has running.
        TrainingSessionEntity session1 = session(1L, "completed", LocalDateTime.of(2026, 7, 1, 10, 0));
        TrainingSessionEntity session2 = session(2L, "in_progress", LocalDateTime.of(2026, 7, 5, 9, 0));
        when(trainingSessionMapper.selectWorkoutSessionsForStats(any(), any(), any()))
                .thenReturn(List.of(session1, session2));

        TrainingSessionExerciseEntity bench1 = exercise(10L, 1L, 1001L, "卧推", "set_based");
        TrainingSessionExerciseEntity bench2 = exercise(11L, 2L, 1001L, "卧推", "set_based");
        TrainingSessionExerciseEntity run = exercise(12L, 1L, 2001L, "跑步", "single_segment");
        when(exerciseMapper.selectBySessionIds(any())).thenReturn(List.of(bench1, bench2, run));

        TrainingSessionExerciseItemEntity bench1Item = item(20L, 10L);
        TrainingSessionExerciseItemEntity bench2Item = item(21L, 11L);
        TrainingSessionExerciseItemEntity runItem = item(22L, 12L);
        when(itemMapper.selectBySessionExerciseIds(any()))
                .thenReturn(List.of(bench1Item, bench2Item, runItem));

        TrainingSessionExerciseItemMetricEntity bench1Weight = metric(30L, 20L, "weight_kg", "60");
        TrainingSessionExerciseItemMetricEntity bench1Reps = metric(31L, 20L, "reps", "8");
        TrainingSessionExerciseItemMetricEntity bench2Weight = metric(32L, 21L, "weight_kg", "70");
        TrainingSessionExerciseItemMetricEntity bench2Reps = metric(33L, 21L, "reps", "6");
        TrainingSessionExerciseItemMetricEntity runMinutes = metric(34L, 22L, "duration_minutes", "30");
        TrainingSessionExerciseItemMetricEntity runDistance = metric(35L, 22L, "distance_km", "5");
        when(metricMapper.selectBySessionExerciseItemIds(any())).thenReturn(List.of(
                bench1Weight, bench1Reps, bench2Weight, bench2Reps, runMinutes, runDistance));

        // When
        AggregatedWorkout result = service.aggregate(101L, null, null);

        // Then
        assertThat(result.sessionCountWithData()).isEqualTo(2);
        assertThat(result.exercises()).hasSize(2);

        ExerciseAggregate bench = result.exercises().get(1001L);
        assertThat(bench.getAppearanceCount()).isEqualTo(2);
        assertThat(bench.setCount).isEqualTo(2);
        assertThat(bench.repsSum).isEqualByComparingTo("14");
        assertThat(bench.getAvgReps()).isEqualByComparingTo("7.00");
        assertThat(bench.totalVolumeKg).isEqualByComparingTo("900");
        assertThat(bench.maxWeightKg).isEqualByComparingTo("70");

        ExerciseAggregate running = result.exercises().get(2001L);
        assertThat(running.getAppearanceCount()).isEqualTo(1);
        assertThat(running.totalDurationSeconds).isEqualByComparingTo("1800");
        assertThat(running.totalDistanceKm).isEqualByComparingTo("5");
    }

    @Test
    void aggregateShouldReturnEmptyWhenNoSessions() {
        when(trainingSessionMapper.selectWorkoutSessionsForStats(any(), any(), any()))
                .thenReturn(List.of());
        AggregatedWorkout result = service.aggregate(101L, null, null);
        assertThat(result.sessionCountWithData()).isZero();
        assertThat(result.exercises()).isEmpty();
    }

    @Test
    void aggregateShouldCountAppearanceOncePerSession() {
        // Same exercise twice in the same session with actual data -> appearanceCount 1.
        TrainingSessionEntity session = session(1L, "completed", LocalDateTime.of(2026, 7, 1, 10, 0));
        when(trainingSessionMapper.selectWorkoutSessionsForStats(any(), any(), any()))
                .thenReturn(List.of(session));

        TrainingSessionExerciseEntity bench1 = exercise(10L, 1L, 1001L, "卧推", "set_based");
        TrainingSessionExerciseEntity bench2 = exercise(11L, 1L, 1001L, "卧推", "set_based");
        when(exerciseMapper.selectBySessionIds(any())).thenReturn(List.of(bench1, bench2));

        TrainingSessionExerciseItemEntity item1 = item(20L, 10L);
        TrainingSessionExerciseItemEntity item2 = item(21L, 11L);
        when(itemMapper.selectBySessionExerciseIds(any())).thenReturn(List.of(item1, item2));

        when(metricMapper.selectBySessionExerciseItemIds(any())).thenReturn(List.of(
                metric(30L, 20L, "weight_kg", "60"),
                metric(31L, 20L, "reps", "8"),
                metric(32L, 21L, "weight_kg", "65"),
                metric(33L, 21L, "reps", "6")));

        AggregatedWorkout result = service.aggregate(101L, null, null);
        ExerciseAggregate bench = result.exercises().get(1001L);
        assertThat(bench.getAppearanceCount()).isEqualTo(1);
        assertThat(bench.setCount).isEqualTo(2);
        assertThat(bench.repsSum).isEqualByComparingTo("14");
    }

    @Test
    void aggregateShouldIncludeStrengthExerciseDuration() {
        // Given: a strength (set_based) exercise records a duration_minutes actual value.
        TrainingSessionEntity session = session(1L, "completed", LocalDateTime.of(2026, 7, 1, 10, 0));
        when(trainingSessionMapper.selectWorkoutSessionsForStats(any(), any(), any()))
                .thenReturn(List.of(session));

        TrainingSessionExerciseEntity bench = exercise(10L, 1L, 1001L, "卧推", "set_based");
        when(exerciseMapper.selectBySessionIds(any())).thenReturn(List.of(bench));

        TrainingSessionExerciseItemEntity item = item(20L, 10L);
        when(itemMapper.selectBySessionExerciseIds(any())).thenReturn(List.of(item));

        when(metricMapper.selectBySessionExerciseItemIds(any())).thenReturn(List.of(
                metric(30L, 20L, "weight_kg", "60"),
                metric(31L, 20L, "reps", "8"),
                metric(32L, 20L, "duration_minutes", "45")));

        // When
        AggregatedWorkout result = service.aggregate(101L, null, null);

        // Then: strength duration is accumulated (45 minutes -> 2700 seconds).
        ExerciseAggregate benchAgg = result.exercises().get(1001L);
        assertThat(benchAgg.totalDurationSeconds).isEqualByComparingTo("2700");
        StatsAggregationService.DailyAggregate daily = benchAgg.daily.get(LocalDate.of(2026, 7, 1));
        assertThat(daily.totalDurationSeconds).isEqualByComparingTo("2700");
    }

    @Test
    void aggregateShouldOnlyQuerySessionsForTheGivenUser() {
        // Given
        when(trainingSessionMapper.selectWorkoutSessionsForStats(any(), any(), any()))
                .thenReturn(List.of());

        // When
        service.aggregate(101L, null, null);

        // Then: aggregation is scoped to the authenticated user id; other users' sessions are never loaded.
        ArgumentCaptor<Long> userCaptor = captor();
        verify(trainingSessionMapper).selectWorkoutSessionsForStats(userCaptor.capture(), any(), any());
        assertThat(userCaptor.getValue()).isEqualTo(101L);
    }

    @Test
    void cardioAggregateShouldDeriveDistanceFromDurationAndSpeed() {
        // Given: cardio item with duration_minutes + speed_kmh but no distance_km.
        TrainingSessionEntity session = session(1L, "completed", LocalDateTime.of(2026, 7, 1, 10, 0));
        when(trainingSessionMapper.selectWorkoutSessionsForStats(any(), any(), any()))
                .thenReturn(List.of(session));

        TrainingSessionExerciseEntity run = exercise(12L, 1L, 2001L, "跑步", "single_segment");
        when(exerciseMapper.selectBySessionIds(any())).thenReturn(List.of(run));

        TrainingSessionExerciseItemEntity runItem = item(22L, 12L);
        when(itemMapper.selectBySessionExerciseIds(any())).thenReturn(List.of(runItem));

        // 30 min at 10 km/h -> 5 km.
        when(metricMapper.selectBySessionExerciseItemIds(any())).thenReturn(List.of(
                metric(34L, 22L, "duration_minutes", "30"),
                metric(35L, 22L, "speed_kmh", "10")));

        // When
        AggregatedWorkout result = service.aggregate(101L, null, null);

        // Then: derived distance is accumulated in both overall and daily.
        ExerciseAggregate running = result.exercises().get(2001L);
        assertThat(running.totalDistanceKm).isEqualByComparingTo("5");
        assertThat(running.totalDurationSeconds).isEqualByComparingTo("1800");
        StatsAggregationService.DailyAggregate daily = running.daily.get(LocalDate.of(2026, 7, 1));
        assertThat(daily.totalDistanceKm).isEqualByComparingTo("5");
    }

    @Test
    void cardioAggregateShouldDeriveDistanceFromPaceWhenNoSpeed() {
        // Given: cardio item with duration_seconds + pace_seconds_per_km but no speed_kmh.
        TrainingSessionEntity session = session(1L, "completed", LocalDateTime.of(2026, 7, 1, 10, 0));
        when(trainingSessionMapper.selectWorkoutSessionsForStats(any(), any(), any()))
                .thenReturn(List.of(session));

        TrainingSessionExerciseEntity run = exercise(12L, 1L, 2001L, "跑步", "single_segment");
        when(exerciseMapper.selectBySessionIds(any())).thenReturn(List.of(run));

        TrainingSessionExerciseItemEntity runItem = item(22L, 12L);
        when(itemMapper.selectBySessionExerciseIds(any())).thenReturn(List.of(runItem));

        // 1800 s (30 min) at pace 360 s/km -> speed 10 km/h -> 5 km.
        when(metricMapper.selectBySessionExerciseItemIds(any())).thenReturn(List.of(
                metric(34L, 22L, "duration_seconds", "1800"),
                metric(35L, 22L, "pace_seconds_per_km", "360")));

        // When
        AggregatedWorkout result = service.aggregate(101L, null, null);

        // Then: distance derived from pace is accumulated.
        ExerciseAggregate running = result.exercises().get(2001L);
        assertThat(running.totalDistanceKm).isEqualByComparingTo("5");
        StatsAggregationService.DailyAggregate daily = running.daily.get(LocalDate.of(2026, 7, 1));
        assertThat(daily.totalDistanceKm).isEqualByComparingTo("5");
    }

    @Test
    void cardioAggregateShouldNotProduceDistanceWithoutDurationOrSpeed() {
        // Given: cardio item with duration but no speed -> no distance; and a second with speed but no duration.
        TrainingSessionEntity session = session(1L, "completed", LocalDateTime.of(2026, 7, 1, 10, 0));
        when(trainingSessionMapper.selectWorkoutSessionsForStats(any(), any(), any()))
                .thenReturn(List.of(session));

        TrainingSessionExerciseEntity run = exercise(12L, 1L, 2001L, "跑步", "single_segment");
        when(exerciseMapper.selectBySessionIds(any())).thenReturn(List.of(run));

        TrainingSessionExerciseItemEntity itemA = item(22L, 12L);
        TrainingSessionExerciseItemEntity itemB = item(23L, 12L);
        when(itemMapper.selectBySessionExerciseIds(any())).thenReturn(List.of(itemA, itemB));

        // itemA: duration only. itemB: speed only (no duration).
        when(metricMapper.selectBySessionExerciseItemIds(any())).thenReturn(List.of(
                metric(30L, 22L, "duration_minutes", "30"),
                metric(31L, 23L, "speed_kmh", "10")));

        // When
        AggregatedWorkout result = service.aggregate(101L, null, null);

        // Then: no distance derived (duration without speed, or speed without duration).
        ExerciseAggregate running = result.exercises().get(2001L);
        assertThat(running.totalDistanceKm).isNull();
        assertThat(running.totalDurationSeconds).isEqualByComparingTo("1800");
    }

    private TrainingSessionEntity session(Long id, String status, LocalDateTime startedAt) {
        TrainingSessionEntity s = new TrainingSessionEntity();
        s.setId(id);
        s.setStatus(status);
        s.setSessionType("workout");
        s.setStartedAt(startedAt);
        return s;
    }

    private TrainingSessionExerciseEntity exercise(
            Long id, Long sessionId, Long exerciseId, String name, String structureType) {
        TrainingSessionExerciseEntity e = new TrainingSessionExerciseEntity();
        e.setId(id);
        e.setSessionId(sessionId);
        e.setExerciseId(exerciseId);
        e.setExerciseNameSnapshot(name);
        e.setStructureType(structureType);
        return e;
    }

    private TrainingSessionExerciseItemEntity item(Long id, Long sessionExerciseId) {
        TrainingSessionExerciseItemEntity i = new TrainingSessionExerciseItemEntity();
        i.setId(id);
        i.setSessionExerciseId(sessionExerciseId);
        return i;
    }

    private TrainingSessionExerciseItemMetricEntity metric(
            Long id, Long itemId, String key, String value) {
        TrainingSessionExerciseItemMetricEntity m = new TrainingSessionExerciseItemMetricEntity();
        m.setId(id);
        m.setSessionExerciseItemId(itemId);
        m.setMetricKey(key);
        m.setActualValueNumber(new BigDecimal(value));
        return m;
    }
}
