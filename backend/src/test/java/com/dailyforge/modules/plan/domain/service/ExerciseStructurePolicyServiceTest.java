package com.dailyforge.modules.plan.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dailyforge.common.BusinessException;
import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateExerciseRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateItemRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateMetricRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExerciseStructurePolicyServiceTest {

    private ExerciseStructurePolicyService exerciseStructurePolicyService;
    private SystemExerciseLookupResult setBasedExercise;
    private SystemExerciseLookupResult singleSegmentExercise;

    @BeforeEach
    void setUp() {
        exerciseStructurePolicyService = new ExerciseStructurePolicyService();
        setBasedExercise = createExercise(1L, "set_based");
        singleSegmentExercise = createExercise(2L, "single_segment");
    }

    @Test
    void validateDayRequestsShouldRejectStructureTypeMismatch() {
        var request = buildDayRequest(new CycleTemplateExerciseRequest(
                1,
                1L,
                "single_segment",
                null,
                List.of(new CycleTemplateItemRequest(
                        1,
                        "segment",
                        null,
                        null,
                        List.of(new CycleTemplateMetricRequest(1, "duration_seconds", new BigDecimal("1200")))))));

        assertThatThrownBy(() -> exerciseStructurePolicyService.validateDayRequests(request, Map.of(1L, setBasedExercise)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cycle template structure type is invalid");
    }

    @Test
    void validateDayRequestsShouldRejectSetBasedSegmentItem() {
        var request = buildDayRequest(new CycleTemplateExerciseRequest(
                1,
                1L,
                "set_based",
                null,
                List.of(new CycleTemplateItemRequest(
                        1,
                        "segment",
                        null,
                        null,
                        List.of(new CycleTemplateMetricRequest(1, "reps", new BigDecimal("8")))))));

        assertThatThrownBy(() -> exerciseStructurePolicyService.validateDayRequests(request, Map.of(1L, setBasedExercise)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cycle template item is invalid");
    }

    @Test
    void validateDayRequestsShouldRejectSingleSegmentWithTwoItems() {
        var request = buildDayRequest(new CycleTemplateExerciseRequest(
                1,
                2L,
                "single_segment",
                null,
                List.of(
                        new CycleTemplateItemRequest(
                                1,
                                "segment",
                                null,
                                null,
                                List.of(new CycleTemplateMetricRequest(1, "duration_seconds", new BigDecimal("1200")))),
                        new CycleTemplateItemRequest(
                                2,
                                "segment",
                                null,
                                null,
                                List.of(new CycleTemplateMetricRequest(1, "distance_km", new BigDecimal("5")))))));

        assertThatThrownBy(() -> exerciseStructurePolicyService.validateDayRequests(request, Map.of(2L, singleSegmentExercise)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cycle template item count is invalid");
    }

    @Test
    void validateDayRequestsShouldRejectDuplicateMetricKey() {
        var request = buildDayRequest(new CycleTemplateExerciseRequest(
                1,
                1L,
                "set_based",
                null,
                List.of(new CycleTemplateItemRequest(
                        1,
                        "set",
                        null,
                        null,
                        List.of(
                                new CycleTemplateMetricRequest(1, "reps", new BigDecimal("8")),
                                new CycleTemplateMetricRequest(2, "reps", new BigDecimal("10")))))));

        assertThatThrownBy(() -> exerciseStructurePolicyService.validateDayRequests(request, Map.of(1L, setBasedExercise)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cycle template metric key is duplicated");
    }

    @Test
    void validateDayRequestsShouldRejectInvalidMetricKey() {
        var request = buildDayRequest(new CycleTemplateExerciseRequest(
                1,
                1L,
                "set_based",
                null,
                List.of(new CycleTemplateItemRequest(
                        1,
                        "set",
                        null,
                        null,
                        List.of(new CycleTemplateMetricRequest(1, "unknown_metric", new BigDecimal("8")))))));

        assertThatThrownBy(() -> exerciseStructurePolicyService.validateDayRequests(request, Map.of(1L, setBasedExercise)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cycle template metric key is invalid");
    }

    @Test
    void validateDayRequestsShouldRejectNegativeMetricValue() {
        var request = buildDayRequest(new CycleTemplateExerciseRequest(
                1,
                1L,
                "set_based",
                null,
                List.of(new CycleTemplateItemRequest(
                        1,
                        "set",
                        null,
                        null,
                        List.of(new CycleTemplateMetricRequest(1, "weight_kg", new BigDecimal("-1")))))));

        assertThatThrownBy(() -> exerciseStructurePolicyService.validateDayRequests(request, Map.of(1L, setBasedExercise)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cycle template metric value is invalid");
    }

    private List<CycleTemplateDayRequest> buildDayRequest(CycleTemplateExerciseRequest exerciseRequest) {
        return List.of(new CycleTemplateDayRequest(1, "Day 1", List.of(exerciseRequest)));
    }

    private SystemExerciseLookupResult createExercise(Long id, String defaultStructureType) {
        return new SystemExerciseLookupResult(
                id,
                null,
                "Exercise-" + id,
                "strength",
                "push",
                "kg",
                defaultStructureType,
                1);
    }
}
