package com.dailyforge.modules.workout.application.service;

import com.dailyforge.modules.workout.application.model.PerformanceSummary;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Shared aggregation of a user's recent completed workout performance.
 *
 * <p>Consumed by the AI template-generation context (so the model can reference actual training
 * history) and reused later by the statistics module; the aggregation itself is written once.
 */
@Service
public class TrainingPerformanceAggregationService {

    private static final String WEIGHT = "weight_kg";
    private static final String REPS = "reps";
    private static final String RPE = "rpe";
    private static final String REST = "rest_seconds";

    private final TrainingSessionMapper trainingSessionMapper;
    private final TrainingSessionExerciseMapper exerciseMapper;
    private final TrainingSessionExerciseItemMapper itemMapper;
    private final TrainingSessionExerciseItemMetricMapper metricMapper;

    public TrainingPerformanceAggregationService(
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
     * Aggregate the user's most recent {@code sessionCount} completed workout sessions.
     *
     * @return a summary, or an empty summary when the user has no completed workout history.
     */
    public PerformanceSummary aggregateRecentCompletedWorkout(Long userId, int sessionCount) {
        List<TrainingSessionEntity> sessions =
                trainingSessionMapper.selectRecentCompletedWorkoutByUserId(userId, sessionCount);
        if (sessions.isEmpty()) {
            return new PerformanceSummary(0, 0.0, List.of());
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

        Map<Long, List<TrainingSessionExerciseItemEntity>> itemsByExerciseEntity =
                new LinkedHashMap<>();
        Map<Long, List<TrainingSessionExerciseItemMetricEntity>> metricsByItem =
                new LinkedHashMap<>();
        for (TrainingSessionExerciseItemEntity item : items) {
            itemsByExerciseEntity.computeIfAbsent(item.getSessionExerciseId(), k -> new ArrayList<>()).add(item);
            metricsByItem.put(item.getId(), new ArrayList<>());
        }
        for (TrainingSessionExerciseItemMetricEntity metric : metrics) {
            metricsByItem.computeIfAbsent(metric.getSessionExerciseItemId(), k -> new ArrayList<>()).add(metric);
        }

        Map<Long, List<TrainingSessionExerciseEntity>> byExerciseId = new LinkedHashMap<>();
        for (TrainingSessionExerciseEntity ex : exercises) {
            byExerciseId.computeIfAbsent(ex.getExerciseId(), k -> new ArrayList<>()).add(ex);
        }

        List<PerformanceSummary.ExercisePerformance> perExercise = new ArrayList<>();
        int totalSetsDone = 0;
        int totalSetsPlanned = 0;
        for (Map.Entry<Long, List<TrainingSessionExerciseEntity>> entry : byExerciseId.entrySet()) {
            Long exerciseId = entry.getKey();
            List<TrainingSessionExerciseEntity> occurrences = entry.getValue();
            Set<Long> sessionSet = new LinkedHashSet<>();
            for (TrainingSessionExerciseEntity ex : occurrences) {
                sessionSet.add(ex.getSessionId());
            }
            boolean setBased = "set_based".equals(occurrences.get(0).getStructureType());

            int setsDone = 0;
            int setsPlanned = 0;
            BigDecimal weightSum = BigDecimal.ZERO;
            int weightCount = 0;
            BigDecimal repsSum = BigDecimal.ZERO;
            int repsCount = 0;
            BigDecimal rpeSum = BigDecimal.ZERO;
            int rpeCount = 0;
            BigDecimal restSum = BigDecimal.ZERO;
            int restCount = 0;
            BigDecimal totalVolume = BigDecimal.ZERO;
            for (TrainingSessionExerciseEntity ex : occurrences) {
                for (TrainingSessionExerciseItemEntity item :
                        itemsByExerciseEntity.getOrDefault(ex.getId(), List.of())) {
                    setsPlanned++;
                    List<TrainingSessionExerciseItemMetricEntity> itemMetrics =
                            metricsByItem.getOrDefault(item.getId(), List.of());
                    boolean hasActual = itemMetrics.stream()
                            .anyMatch(m -> m.getActualValueNumber() != null);
                    if (hasActual) {
                        setsDone++;
                    }
                    for (TrainingSessionExerciseItemMetricEntity m : itemMetrics) {
                        BigDecimal actual = m.getActualValueNumber();
                        if (actual == null) {
                            continue;
                        }
                        switch (m.getMetricKey()) {
                            case WEIGHT -> {
                                weightSum = weightSum.add(actual);
                                weightCount++;
                            }
                            case REPS -> {
                                repsSum = repsSum.add(actual);
                                repsCount++;
                            }
                            case RPE -> {
                                rpeSum = rpeSum.add(actual);
                                rpeCount++;
                            }
                            case REST -> {
                                restSum = restSum.add(actual);
                                restCount++;
                            }
                            default -> {
                            }
                        }
                    }
                    BigDecimal weight = findValue(itemMetrics, WEIGHT);
                    BigDecimal reps = findValue(itemMetrics, REPS);
                    if (setBased && weight != null && reps != null) {
                        totalVolume = totalVolume.add(weight.multiply(reps));
                    }
                }
            }
            double completionRate = setsPlanned == 0 ? 0.0 : (double) setsDone / setsPlanned;
            perExercise.add(new PerformanceSummary.ExercisePerformance(
                    exerciseId,
                    occurrences.get(0).getExerciseNameSnapshot(),
                    occurrences.get(0).getStructureType(),
                    sessionSet.size(),
                    setsDone,
                    setsPlanned,
                    completionRate,
                    average(weightSum, weightCount),
                    totalVolume,
                    average(repsSum, repsCount),
                    average(rpeSum, rpeCount),
                    average(restSum, restCount)));
            totalSetsDone += setsDone;
            totalSetsPlanned += setsPlanned;
        }
        double avgCompletion = totalSetsPlanned == 0 ? 0.0 : (double) totalSetsDone / totalSetsPlanned;
        return new PerformanceSummary(sessions.size(), avgCompletion, perExercise);
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

    private BigDecimal average(BigDecimal sum, int count) {
        return count == 0
                ? null
                : sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}
