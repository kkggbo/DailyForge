package com.dailyforge.modules.plan.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dailyforge.common.BusinessException;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.UserActiveCycleEntity;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateExerciseRequest;
import com.dailyforge.modules.plan.interfaces.dto.UpdateCycleTemplateRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CycleTemplatePolicyServiceTest {

    private CycleTemplatePolicyService cycleTemplatePolicyService;

    @BeforeEach
    void setUp() {
        cycleTemplatePolicyService = new CycleTemplatePolicyService();
    }

    @Test
    void validateDraftCycleLengthShouldAllowNull() {
        assertThatCode(() -> cycleTemplatePolicyService.validateDraftCycleLength(null))
                .doesNotThrowAnyException();
    }

    @Test
    void assertFormalCycleLengthShouldRejectNull() {
        assertThatThrownBy(() -> cycleTemplatePolicyService.assertFormalCycleLength(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cycle length is invalid");
    }

    @Test
    void validateDayRequestsShouldRejectOutOfRangeDayIndex() {
        assertThatThrownBy(() -> cycleTemplatePolicyService.validateDayRequests(
                5,
                List.of(new CycleTemplateDayRequest(6, "Legs", List.of()))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cycle template day is out of range");
    }

    @Test
    void validateDayRequestsShouldRejectDuplicateDayIndex() {
        List<CycleTemplateDayRequest> days = List.of(
                new CycleTemplateDayRequest(1, "Push", List.of()),
                new CycleTemplateDayRequest(1, "Pull", List.of()));

        assertThatThrownBy(() -> cycleTemplatePolicyService.validateDayRequests(5, days))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duplicate dayIndex");
    }

    @Test
    void validateDayRequestsShouldRejectDuplicateExerciseSortOrder() {
        List<CycleTemplateExerciseRequest> exercises = List.of(
                new CycleTemplateExerciseRequest(1, 1001L, "set_based", null, List.of()),
                new CycleTemplateExerciseRequest(1, 1002L, "set_based", null, List.of()));

        assertThatThrownBy(() -> cycleTemplatePolicyService.validateDayRequests(
                5,
                List.of(new CycleTemplateDayRequest(1, "Push", exercises))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duplicate sortOrder");
    }

    @Test
    void assertActiveUpdateAllowedShouldRejectCycleLengthChange() {
        CycleTemplateEntity template = new CycleTemplateEntity();
        template.setCycleLength(5);

        UserActiveCycleEntity activeCycle = new UserActiveCycleEntity();
        activeCycle.setCurrentDayIndex(3);

        UpdateCycleTemplateRequest request = new UpdateCycleTemplateRequest(
                "Push Pull Legs",
                "muscle_gain",
                6,
                List.of(new CycleTemplateDayRequest(3, "Legs", List.of())));

        assertThatThrownBy(() -> cycleTemplatePolicyService.assertActiveUpdateAllowed(template, request, activeCycle))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cycleLength cannot be changed");
    }

    @Test
    void validateDayRequestsShouldRejectLockedDayUpdate() {
        assertThatThrownBy(() -> cycleTemplatePolicyService.validateDayRequests(
                5,
                List.of(new CycleTemplateDayRequest(2, "Pull", List.of())),
                3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("locked");
    }
}
