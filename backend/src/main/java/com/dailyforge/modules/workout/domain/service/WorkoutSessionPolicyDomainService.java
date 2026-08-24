package com.dailyforge.modules.workout.domain.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemMetricEntity;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionExerciseItemSaveRequest;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionExerciseMetricSaveRequest;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionExerciseSaveRequest;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionSaveRequest;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class WorkoutSessionPolicyDomainService {

    private static final Set<String> EXERCISE_STATUSES =
            Set.of("completed", "partial_completed", "skipped", "failed");
    private static final Set<String> FAILURE_REASONS =
            Set.of("too_tired", "equipment_unavailable", "pain_or_discomfort", "time_not_enough", "plan_too_hard", "other");
    private static final Set<String> INTEGER_METRIC_KEYS =
            Set.of("reps", "duration_seconds", "duration_minutes", "rest_seconds", "intensity_level");
    private static final Set<String> DECIMAL_METRIC_KEYS =
            Set.of("weight_kg", "distance_km", "speed_kmh", "pace_seconds_per_km", "incline_percent", "rpe");
    private static final BigDecimal MAX_METRIC_VALUE = new BigDecimal("99999999.99");

    public void validateFullSave(
            WorkoutSessionSaveRequest request,
            String sessionType,
            List<TrainingSessionExerciseEntity> sessionExercises,
            Map<Long, List<TrainingSessionExerciseItemEntity>> itemsByExerciseId,
            Map<Long, List<TrainingSessionExerciseItemMetricEntity>> metricsByItemId,
            boolean completing) {
        List<WorkoutSessionExerciseSaveRequest> requestedExercises = request.exercises();
        if ("rest_day".equals(sessionType)) {
            if (requestedExercises == null || !requestedExercises.isEmpty()) {
                throw new BusinessException(ErrorCode.WORKOUT_SESSION_EXERCISE_INVALID);
            }
            return;
        }

        if (requestedExercises == null || requestedExercises.size() != sessionExercises.size()) {
            throw new BusinessException(ErrorCode.WORKOUT_SESSION_EXERCISE_INVALID);
        }

        Map<Long, WorkoutSessionExerciseSaveRequest> requestExerciseMap = toExerciseRequestMap(requestedExercises);
        Set<Long> expectedExerciseIds = sessionExercises.stream()
                .map(TrainingSessionExerciseEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (!expectedExerciseIds.equals(requestExerciseMap.keySet())) {
            throw new BusinessException(ErrorCode.WORKOUT_SESSION_EXERCISE_INVALID);
        }

        for (TrainingSessionExerciseEntity exercise : sessionExercises) {
            WorkoutSessionExerciseSaveRequest exerciseRequest = requestExerciseMap.get(exercise.getId());
            validateExerciseValues(exerciseRequest, completing);
            validateItems(
                    exerciseRequest.items(),
                    itemsByExerciseId.getOrDefault(exercise.getId(), List.of()),
                    metricsByItemId);
        }
    }

    private Map<Long, WorkoutSessionExerciseSaveRequest> toExerciseRequestMap(
            List<WorkoutSessionExerciseSaveRequest> requests) {
        Map<Long, WorkoutSessionExerciseSaveRequest> result = new java.util.LinkedHashMap<>();
        for (WorkoutSessionExerciseSaveRequest request : requests) {
            if (request == null || request.sessionExerciseId() == null
                    || result.putIfAbsent(request.sessionExerciseId(), request) != null) {
                throw new BusinessException(ErrorCode.WORKOUT_SESSION_EXERCISE_INVALID);
            }
        }
        return result;
    }

    private void validateExerciseValues(WorkoutSessionExerciseSaveRequest request, boolean completing) {
        if (completing && request.exerciseStatus() == null) {
            throw new BusinessException(ErrorCode.WORKOUT_EXERCISE_STATUS_REQUIRED);
        }
        if (request.exerciseStatus() != null && !EXERCISE_STATUSES.contains(request.exerciseStatus())) {
            throw new BusinessException(ErrorCode.WORKOUT_EXERCISE_STATUS_INVALID);
        }
        if (request.failureReason() != null && !FAILURE_REASONS.contains(request.failureReason())) {
            throw new BusinessException(ErrorCode.WORKOUT_FAILURE_REASON_INVALID);
        }
    }

    private void validateItems(
            List<WorkoutSessionExerciseItemSaveRequest> requestedItems,
            List<TrainingSessionExerciseItemEntity> sessionItems,
            Map<Long, List<TrainingSessionExerciseItemMetricEntity>> metricsByItemId) {
        if (requestedItems == null || requestedItems.size() != sessionItems.size()) {
            throw new BusinessException(ErrorCode.WORKOUT_ITEM_INVALID);
        }

        Map<Integer, WorkoutSessionExerciseItemSaveRequest> requestItemMap = new java.util.LinkedHashMap<>();
        for (WorkoutSessionExerciseItemSaveRequest request : requestedItems) {
            if (request == null || request.itemIndex() == null
                    || requestItemMap.putIfAbsent(request.itemIndex(), request) != null) {
                throw new BusinessException(ErrorCode.WORKOUT_ITEM_INVALID);
            }
        }
        Set<Integer> expectedItemIndexes = sessionItems.stream()
                .map(TrainingSessionExerciseItemEntity::getItemIndex)
                .collect(java.util.stream.Collectors.toSet());
        if (!expectedItemIndexes.equals(requestItemMap.keySet())) {
            throw new BusinessException(ErrorCode.WORKOUT_ITEM_INVALID);
        }

        for (TrainingSessionExerciseItemEntity item : sessionItems) {
            validateMetrics(
                    requestItemMap.get(item.getItemIndex()).metrics(),
                    metricsByItemId.getOrDefault(item.getId(), List.of()));
        }
    }

    private void validateMetrics(
            List<WorkoutSessionExerciseMetricSaveRequest> requestedMetrics,
            List<TrainingSessionExerciseItemMetricEntity> sessionMetrics) {
        if (requestedMetrics == null || requestedMetrics.size() != sessionMetrics.size()) {
            throw new BusinessException(ErrorCode.WORKOUT_METRIC_INVALID);
        }

        Map<String, WorkoutSessionExerciseMetricSaveRequest> requestMetricMap = new java.util.LinkedHashMap<>();
        for (WorkoutSessionExerciseMetricSaveRequest request : requestedMetrics) {
            if (request == null || request.metricKey() == null
                    || requestMetricMap.putIfAbsent(request.metricKey(), request) != null) {
                throw new BusinessException(ErrorCode.WORKOUT_METRIC_INVALID);
            }
            validateMetricValue(request.metricKey(), request.actualValueNumber());
        }
        Set<String> expectedMetricKeys = new HashSet<>();
        for (TrainingSessionExerciseItemMetricEntity metric : sessionMetrics) {
            expectedMetricKeys.add(metric.getMetricKey());
        }
        if (!expectedMetricKeys.equals(requestMetricMap.keySet())) {
            throw new BusinessException(ErrorCode.WORKOUT_METRIC_INVALID);
        }
    }

    private void validateMetricValue(String metricKey, BigDecimal value) {
        if (value == null) {
            return;
        }
        if (value.signum() < 0 || value.compareTo(MAX_METRIC_VALUE) > 0) {
            throw new BusinessException(ErrorCode.WORKOUT_METRIC_VALUE_INVALID);
        }
        if (INTEGER_METRIC_KEYS.contains(metricKey) && value.stripTrailingZeros().scale() > 0) {
            throw new BusinessException(ErrorCode.WORKOUT_METRIC_VALUE_INVALID);
        }
        if (DECIMAL_METRIC_KEYS.contains(metricKey) && value.stripTrailingZeros().scale() > 2) {
            throw new BusinessException(ErrorCode.WORKOUT_METRIC_VALUE_INVALID);
        }
    }
}

