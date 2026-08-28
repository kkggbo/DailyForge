package com.dailyforge.modules.stats.application.service;

import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemMetricEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMetricMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Loads a user's workout sessions within an optional time range and aggregates them in memory.
 * Pure read aggregation for the stats module; no persistence writes.
 */
@Service
public class StatsAggregationService {

    private static final String WEIGHT = "weight_kg";
    private static final String REPS = "reps";
    private static final String DURATION_SECONDS = "duration_seconds";
    private static final String DURATION_MINUTES = "duration_minutes";
    private static final String DISTANCE_KM = "distance_km";
    private static final String SPEED_KMH = "speed_kmh";
    private static final String PACE_SECONDS_PER_KM = "pace_seconds_per_km";
    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);
    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);

    private final TrainingSessionMapper trainingSessionMapper;
    private final TrainingSessionExerciseMapper exerciseMapper;
    private final TrainingSessionExerciseItemMapper itemMapper;
    private final TrainingSessionExerciseItemMetricMapper metricMapper;

    public StatsAggregationService(
            TrainingSessionMapper trainingSessionMapper,
            TrainingSessionExerciseMapper exerciseMapper,
            TrainingSessionExerciseItemMapper itemMapper,
            TrainingSessionExerciseItemMetricMapper metricMapper) {
        this.trainingSessionMapper = trainingSessionMapper;
        this.exerciseMapper = exerciseMapper;
        this.itemMapper = itemMapper;
        this.metricMapper = metricMapper;
    }

    /**
     * Aggregate all workout sessions (excluding cancelled) for the user, optionally filtered by
     * {@code from}/{@code to} on session started_at.
     */
    public AggregatedWorkout aggregate(Long userId, LocalDateTime from, LocalDateTime to) {
        List<TrainingSessionEntity> sessions =
                trainingSessionMapper.selectWorkoutSessionsForStats(userId, from, to);
        if (sessions.isEmpty()) {
            return new AggregatedWorkout(0, Map.of());
        }
        List<Long> sessionIds = sessions.stream().map(TrainingSessionEntity::getId).toList();
        List<TrainingSessionExerciseEntity> exercises = exerciseMapper.selectBySessionIds(sessionIds);
        List<Long> exerciseEntityIds = exercises.stream().map(TrainingSessionExerciseEntity::getId).toList();
        List<TrainingSessionExerciseItemEntity> items =
                exerciseEntityIds.isEmpty()
                        ? List.of()
                        : itemMapper.selectBySessionExerciseIds(exerciseEntityIds);
        List<Long> itemIds = items.stream().map(TrainingSessionExerciseItemEntity::getId).toList();
        List<TrainingSessionExerciseItemMetricEntity> metrics =
                itemIds.isEmpty()
                        ? List.of()
                        : metricMapper.selectBySessionExerciseItemIds(itemIds);

        // Build in-memory indexes.
        Map<Long, TrainingSessionEntity> sessionById = new LinkedHashMap<>();
        for (TrainingSessionEntity session : sessions) {
            sessionById.put(session.getId(), session);
        }
        Map<Long, List<TrainingSessionExerciseEntity>> exercisesBySession = new LinkedHashMap<>();
        for (TrainingSessionExerciseEntity exercise : exercises) {
            exercisesBySession.computeIfAbsent(exercise.getSessionId(), k -> new ArrayList<>()).add(exercise);
        }
        Map<Long, List<TrainingSessionExerciseItemEntity>> itemsByExerciseEntity = new LinkedHashMap<>();
        for (TrainingSessionExerciseItemEntity item : items) {
            itemsByExerciseEntity.computeIfAbsent(item.getSessionExerciseId(), k -> new ArrayList<>()).add(item);
        }
        Map<Long, List<TrainingSessionExerciseItemMetricEntity>> metricsByItem = new LinkedHashMap<>();
        for (TrainingSessionExerciseItemMetricEntity metric : metrics) {
            metricsByItem.computeIfAbsent(metric.getSessionExerciseItemId(), k -> new ArrayList<>()).add(metric);
        }

        Map<Long, ExerciseAggregate> byExerciseId = new LinkedHashMap<>();
        for (TrainingSessionExerciseEntity exercise : exercises) {
            List<TrainingSessionExerciseItemEntity> itemList =
                    itemsByExerciseEntity.getOrDefault(exercise.getId(), List.of());
            boolean setBased = "set_based".equals(exercise.getStructureType());
            ExerciseAggregate agg =
                    byExerciseId.computeIfAbsent(exercise.getExerciseId(), k -> new ExerciseAggregate(
                            exercise.getExerciseId(),
                            exercise.getExerciseNameSnapshot(),
                            exercise.getStructureType()));
            TrainingSessionEntity session = sessionById.get(exercise.getSessionId());
            LocalDate day = session == null ? null : session.getStartedAt() == null
                    ? null
                    : session.getStartedAt().toLocalDate();

            for (TrainingSessionExerciseItemEntity item : itemList) {
                List<TrainingSessionExerciseItemMetricEntity> itemMetrics =
                        metricsByItem.getOrDefault(item.getId(), List.of());
                boolean hasActual = itemMetrics.stream()
                        .anyMatch(m -> m.getActualValueNumber() != null);
                if (!hasActual) {
                    continue;
                }
                agg.appearSessionIds.add(exercise.getSessionId());
                agg.setCount++;

                BigDecimal weight = findValue(itemMetrics, WEIGHT);
                BigDecimal reps = findValue(itemMetrics, REPS);
                BigDecimal durationSeconds = findValue(itemMetrics, DURATION_SECONDS);
                BigDecimal durationMinutes = findValue(itemMetrics, DURATION_MINUTES);
                if (durationMinutes != null) {
                    durationSeconds = add(durationSeconds, durationMinutes.multiply(SIXTY));
                }
                // Duration applies to both strength and cardio (product confirmed): total duration
                // is the sum across all exercises with any actual duration value.
                if (durationSeconds != null) {
                    agg.totalDurationSeconds = add(agg.totalDurationSeconds, durationSeconds);
                }

                if (setBased) {
                    if (weight != null) {
                        agg.weightSum = add(agg.weightSum, weight);
                        agg.weightCount++;
                        agg.maxWeightKg = max(agg.maxWeightKg, weight);
                    }
                    if (reps != null) {
                        agg.repsSum = add(agg.repsSum, reps);
                        agg.repsCount++;
                    }
                    if (weight != null && reps != null) {
                        agg.totalVolumeKg = add(agg.totalVolumeKg, weight.multiply(reps));
                    }
                } else {
                    CardioPace cardio = resolveCardioPace(itemMetrics, durationSeconds);
                    if (cardio.distanceKm() != null) {
                        agg.totalDistanceKm = add(agg.totalDistanceKm, cardio.distanceKm());
                    }
                    if (cardio.speedKmh() != null) {
                        agg.speedSum = add(agg.speedSum, cardio.speedKmh());
                        agg.speedCount++;
                    }
                }

                if (day != null) {
                    DailyAggregate daily = agg.daily.computeIfAbsent(day, k -> new DailyAggregate());
                    if (durationSeconds != null) {
                        daily.totalDurationSeconds = add(daily.totalDurationSeconds, durationSeconds);
                    }
                    if (setBased) {
                        if (weight != null) {
                            daily.maxWeightKg = max(daily.maxWeightKg, weight);
                        }
                        if (reps != null) {
                            daily.maxReps = max(daily.maxReps, reps.intValue());
                            daily.totalVolumeKg = add(daily.totalVolumeKg, weight == null
                                    ? BigDecimal.ZERO
                                    : weight.multiply(reps));
                        }
                    } else {
                        CardioPace cardio = resolveCardioPace(itemMetrics, durationSeconds);
                        if (cardio.distanceKm() != null) {
                            daily.totalDistanceKm = add(daily.totalDistanceKm, cardio.distanceKm());
                        }
                    }
                }
            }
        }

        int sessionCountWithData = 0;
        Set<Long> overallSessions = new LinkedHashSet<>();
        for (ExerciseAggregate agg : byExerciseId.values()) {
            overallSessions.addAll(agg.appearSessionIds);
        }
        sessionCountWithData = overallSessions.size();
        return new AggregatedWorkout(sessionCountWithData, byExerciseId);
    }

    private CardioPace resolveCardioPace(
            List<TrainingSessionExerciseItemMetricEntity> itemMetrics,
            BigDecimal durationSeconds) {
        BigDecimal speed = findValue(itemMetrics, SPEED_KMH);
        if (speed == null) {
            BigDecimal pace = findValue(itemMetrics, PACE_SECONDS_PER_KM);
            if (pace != null && pace.signum() > 0) {
                speed = SECONDS_PER_HOUR.divide(pace, 4, RoundingMode.HALF_UP);
            }
        }

        BigDecimal distance = findValue(itemMetrics, DISTANCE_KM);
        if (distance == null
                && durationSeconds != null
                && durationSeconds.signum() > 0
                && speed != null) {
            // distance_km missing -> derive from speed and duration: km = speed(km/h) * hours.
            distance = speed.multiply(durationSeconds)
                    .divide(SECONDS_PER_HOUR, 4, RoundingMode.HALF_UP);
        }
        return new CardioPace(speed, distance);
    }

    private BigDecimal findValue(
            List<TrainingSessionExerciseItemMetricEntity> metrics,
            String metricKey) {
        for (TrainingSessionExerciseItemMetricEntity metric : metrics) {
            if (metricKey.equals(metric.getMetricKey()) && metric.getActualValueNumber() != null) {
                return metric.getActualValueNumber();
            }
        }
        return null;
    }

    private BigDecimal add(BigDecimal a, BigDecimal b) {
        if (b == null) {
            return a == null ? null : a;
        }
        return a == null ? b : a.add(b);
    }

    private BigDecimal max(BigDecimal current, BigDecimal candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.compareTo(current) > 0) {
            return candidate;
        }
        return current;
    }

    private int max(int current, int candidate) {
        return Math.max(current, candidate);
    }

    /** Aggregation result for a single exercise id across the whole range. */
    public static class ExerciseAggregate {
        public final Long exerciseId;
        public final String nameSnapshot;
        public final String structureType;
        public final Set<Long> appearSessionIds = new LinkedHashSet<>();
        public int setCount;
        public BigDecimal weightSum;
        public int weightCount;
        public BigDecimal repsSum;
        public int repsCount;
        public BigDecimal totalVolumeKg;
        public BigDecimal maxWeightKg;
        public BigDecimal totalDurationSeconds;
        public BigDecimal totalDistanceKm;
        public BigDecimal speedSum;
        public int speedCount;
        public final Map<LocalDate, DailyAggregate> daily = new LinkedHashMap<>();

        public ExerciseAggregate(Long exerciseId, String nameSnapshot, String structureType) {
            this.exerciseId = exerciseId;
            this.nameSnapshot = nameSnapshot;
            this.structureType = structureType;
        }

        public int getAppearanceCount() {
            return appearSessionIds.size();
        }

        public BigDecimal getAvgWeightKg() {
            return weightCount == 0 ? null : weightSum.divide(BigDecimal.valueOf(weightCount), 2, RoundingMode.HALF_UP);
        }

        public BigDecimal getAvgReps() {
            return repsCount == 0 ? null : repsSum.divide(BigDecimal.valueOf(repsCount), 2, RoundingMode.HALF_UP);
        }

        public BigDecimal getAvgSpeedKmh() {
            if (speedCount > 0) {
                return speedSum.divide(BigDecimal.valueOf(speedCount), 2, RoundingMode.HALF_UP);
            }
            if (totalDistanceKm != null && totalDurationSeconds != null
                    && totalDurationSeconds.signum() > 0) {
                return totalDistanceKm.divide(
                        totalDurationSeconds.divide(SECONDS_PER_HOUR, 4, RoundingMode.HALF_UP),
                        2,
                        RoundingMode.HALF_UP);
            }
            return null;
        }
    }

    /** Per-date progression aggregate for an exercise. */
    public static class DailyAggregate {
        public BigDecimal maxWeightKg;
        public int maxReps;
        public BigDecimal totalVolumeKg;
        public BigDecimal totalDurationSeconds;
        public BigDecimal totalDistanceKm;
    }

    /** Cardio pace resolution result: effective speed and distance (distance may be derived). */
    private record CardioPace(BigDecimal speedKmh, BigDecimal distanceKm) {
    }

    /** Full aggregation result for a user range. */
    public record AggregatedWorkout(
            int sessionCountWithData,
            Map<Long, ExerciseAggregate> exercises) {
    }
}
