package com.dailyforge.modules.workout.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dailyforge.common.BusinessException;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemMetricEntity;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionExerciseItemSaveRequest;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionExerciseMetricSaveRequest;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionExerciseSaveRequest;
import com.dailyforge.modules.workout.interfaces.dto.WorkoutSessionSaveRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkoutSessionPolicyDomainServiceTest {

    private WorkoutSessionPolicyDomainService policyService;
    private TrainingSessionExerciseEntity sessionExercise;
    private TrainingSessionExerciseItemEntity sessionItem;
    private List<TrainingSessionExerciseItemMetricEntity> sessionMetrics;

    @BeforeEach
    void setUp() {
        policyService = new WorkoutSessionPolicyDomainService();

        sessionExercise = new TrainingSessionExerciseEntity();
        sessionExercise.setId(101L);

        sessionItem = new TrainingSessionExerciseItemEntity();
        sessionItem.setId(201L);
        sessionItem.setItemIndex(1);

        sessionMetrics = List.of(metric(301L, "weight_kg"), metric(302L, "reps"));
    }

    @Test
    void validateFullSaveShouldAllowCompleteSnapshotPayload() {
        WorkoutSessionSaveRequest request = request("completed", List.of(
                metricRequest("weight_kg", "50"),
                metricRequest("reps", "6")));

        assertThatCode(() -> policyService.validateFullSave(
                request,
                "workout",
                List.of(sessionExercise),
                Map.of(sessionExercise.getId(), List.of(sessionItem)),
                Map.of(sessionItem.getId(), sessionMetrics),
                true)).doesNotThrowAnyException();
    }

    @Test
    void validateFullSaveShouldRequireExerciseStatusWhenCompletingWorkout() {
        WorkoutSessionSaveRequest request = request(null, List.of(
                metricRequest("weight_kg", "50"),
                metricRequest("reps", "6")));

        assertThatThrownBy(() -> policyService.validateFullSave(
                request,
                "workout",
                List.of(sessionExercise),
                Map.of(sessionExercise.getId(), List.of(sessionItem)),
                Map.of(sessionItem.getId(), sessionMetrics),
                true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("every workout exercise requires a completion status");
    }

    @Test
    void validateFullSaveShouldRejectPartialMetricPayload() {
        WorkoutSessionSaveRequest request = request("partial_completed", List.of(
                metricRequest("weight_kg", "50")));

        assertThatThrownBy(() -> policyService.validateFullSave(
                request,
                "workout",
                List.of(sessionExercise),
                Map.of(sessionExercise.getId(), List.of(sessionItem)),
                Map.of(sessionItem.getId(), sessionMetrics),
                false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("workout metric payload does not match the session snapshot");
    }

    @Test
    void validateFullSaveShouldRejectNegativeMetricValue() {
        WorkoutSessionSaveRequest request = request("completed", List.of(
                metricRequest("weight_kg", "-1"),
                metricRequest("reps", "6")));

        assertThatThrownBy(() -> policyService.validateFullSave(
                request,
                "workout",
                List.of(sessionExercise),
                Map.of(sessionExercise.getId(), List.of(sessionItem)),
                Map.of(sessionItem.getId(), sessionMetrics),
                true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("workout metric value is invalid");
    }

    @Test
    void validateFullSaveShouldRejectFractionalIntegerMetricValue() {
        WorkoutSessionSaveRequest request = request("completed", List.of(
                metricRequest("weight_kg", "50"),
                metricRequest("reps", "6.5")));

        assertThatThrownBy(() -> policyService.validateFullSave(
                request,
                "workout",
                List.of(sessionExercise),
                Map.of(sessionExercise.getId(), List.of(sessionItem)),
                Map.of(sessionItem.getId(), sessionMetrics),
                true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("workout metric value is invalid");
    }

    @Test
    void validateFullSaveShouldRejectDecimalMetricValueWithMoreThanTwoFractionDigits() {
        WorkoutSessionSaveRequest request = request("completed", List.of(
                metricRequest("weight_kg", "50.111"),
                metricRequest("reps", "6")));

        assertThatThrownBy(() -> policyService.validateFullSave(
                request,
                "workout",
                List.of(sessionExercise),
                Map.of(sessionExercise.getId(), List.of(sessionItem)),
                Map.of(sessionItem.getId(), sessionMetrics),
                true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("workout metric value is invalid");
    }

    @Test
    void validateFullSaveShouldAllowOnlyEmptyExercisesForRestDay() {
        WorkoutSessionSaveRequest emptyRestDayRequest = new WorkoutSessionSaveRequest(null, "恢复良好", List.of());

        assertThatCode(() -> policyService.validateFullSave(
                emptyRestDayRequest,
                "rest_day",
                List.of(),
                Map.of(),
                Map.of(),
                true)).doesNotThrowAnyException();

        assertThatThrownBy(() -> policyService.validateFullSave(
                request("completed", List.of(metricRequest("weight_kg", "50"))),
                "rest_day",
                List.of(),
                Map.of(),
                Map.of(),
                true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("workout session exercise payload does not match the session snapshot");
    }

    @Test
    void validateFullSaveShouldAllowIntegerDurationMinutesValue() {
        List<TrainingSessionExerciseItemMetricEntity> metrics = List.of(metric(301L, "duration_minutes"));
        WorkoutSessionSaveRequest request = request("completed", List.of(
                metricRequest("duration_minutes", "30")));

        assertThatCode(() -> policyService.validateFullSave(
                request,
                "workout",
                List.of(sessionExercise),
                Map.of(sessionExercise.getId(), List.of(sessionItem)),
                Map.of(sessionItem.getId(), metrics),
                true)).doesNotThrowAnyException();
    }

    @Test
    void validateFullSaveShouldRejectFractionalDurationMinutesValue() {
        List<TrainingSessionExerciseItemMetricEntity> metrics = List.of(metric(301L, "duration_minutes"));
        WorkoutSessionSaveRequest request = request("completed", List.of(
                metricRequest("duration_minutes", "30.5")));

        assertThatThrownBy(() -> policyService.validateFullSave(
                request,
                "workout",
                List.of(sessionExercise),
                Map.of(sessionExercise.getId(), List.of(sessionItem)),
                Map.of(sessionItem.getId(), metrics),
                true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("workout metric value is invalid");
    }

    private WorkoutSessionSaveRequest request(
            String exerciseStatus,
            List<WorkoutSessionExerciseMetricSaveRequest> metricRequests) {
        return new WorkoutSessionSaveRequest(
                "状态一般",
                null,
                List.of(new WorkoutSessionExerciseSaveRequest(
                        sessionExercise.getId(),
                        exerciseStatus,
                        null,
                        null,
                        null,
                        null,
                        List.of(new WorkoutSessionExerciseItemSaveRequest(
                                sessionItem.getItemIndex(),
                                metricRequests)))));
    }

    private TrainingSessionExerciseItemMetricEntity metric(Long id, String metricKey) {
        TrainingSessionExerciseItemMetricEntity metric = new TrainingSessionExerciseItemMetricEntity();
        metric.setId(id);
        metric.setMetricKey(metricKey);
        return metric;
    }

    private WorkoutSessionExerciseMetricSaveRequest metricRequest(String metricKey, String value) {
        return new WorkoutSessionExerciseMetricSaveRequest(metricKey, new BigDecimal(value));
    }
}