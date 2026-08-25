package com.dailyforge.modules.workout.application.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregate summary of a user's recent completed workout performance. Produced by
 * {@link com.dailyforge.modules.workout.application.service.TrainingPerformanceAggregationService}
 * and shared by the AI prompt context and future statistics module.
 *
 * @param sessionCount how many completed workout sessions were included
 * @param avgCompletionRate weighted completion rate across all exercises (setsDone / setsPlanned)
 * @param exercises per-exercise aggregates
 */
public record PerformanceSummary(
        int sessionCount,
        double avgCompletionRate,
        List<ExercisePerformance> exercises) {

    /**
     * Aggregates for one distinct system exercise across the included sessions.
     */
    public record ExercisePerformance(
            Long exerciseId,
            String name,
            String structureType,
            int timesPerformed,
            int setsDone,
            int setsPlanned,
            double completionRate,
            BigDecimal avgWeightKg,
            BigDecimal totalVolume,
            BigDecimal avgReps,
            BigDecimal avgRpe,
            BigDecimal avgRestSeconds) {
    }
}
