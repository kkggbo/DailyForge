package com.dailyforge.modules.plan.domain.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.UserActiveCycleEntity;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateExerciseRequest;
import com.dailyforge.modules.plan.interfaces.dto.UpdateCycleTemplateRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CycleTemplatePolicyService {

    /**
     * Validate nullable draft cycle length.
     */
    public void validateDraftCycleLength(Integer cycleLength) {
        if (cycleLength != null) {
            assertCycleLengthInRange(cycleLength);
        }
    }

    /**
     * Validate non-null cycle length for formal templates.
     */
    public void assertFormalCycleLength(Integer cycleLength) {
        if (cycleLength == null) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_CYCLE_LENGTH_INVALID);
        }
        assertCycleLengthInRange(cycleLength);
    }

    /**
     * Validate day indexes and duplicate keys for one save request.
     */
    public void validateDayRequests(Integer cycleLength, List<CycleTemplateDayRequest> days) {
        validateDayRequests(cycleLength, days, null);
    }

    /**
     * Validate day indexes and editable range for active template editing.
     */
    public void validateDayRequests(Integer cycleLength, List<CycleTemplateDayRequest> days, Integer editableFromDayIndex) {
        if (days == null || days.isEmpty()) {
            return;
        }

        Set<Integer> dayIndexSet = new HashSet<>();
        for (CycleTemplateDayRequest day : days) {
            Integer dayIndex = day.dayIndex();
            if (!dayIndexSet.add(dayIndex)) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "duplicate dayIndex: " + dayIndex);
            }
            if (cycleLength != null && dayIndex > cycleLength) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_DAY_OUT_OF_RANGE);
            }
            if (editableFromDayIndex != null && dayIndex < editableFromDayIndex) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_EDIT_FORBIDDEN,
                        "dayIndex " + dayIndex + " is locked");
            }

            Set<Integer> sortOrders = new HashSet<>();
            List<CycleTemplateExerciseRequest> exercises = day.exercises();
            if (exercises == null || exercises.isEmpty()) {
                continue;
            }
            for (CycleTemplateExerciseRequest exercise : exercises) {
                if (!sortOrders.add(exercise.sortOrder())) {
                    throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                            "duplicate sortOrder in day " + dayIndex + ": " + exercise.sortOrder());
                }
            }
        }
    }

    /**
     * Ensure template status matches one of expected values.
     */
    public void assertTemplateStatus(CycleTemplateEntity template, String... expectedStatuses) {
        for (String expectedStatus : expectedStatuses) {
            if (Objects.equals(template.getStatus(), expectedStatus)) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_STATUS_INVALID);
    }

    /**
     * Validate that target template may be deleted.
     */
    public void assertCanDelete(CycleTemplateEntity template) {
        if (!"draft".equals(template.getStatus()) && !"inactive".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_DELETE_FORBIDDEN);
        }
    }

    /**
     * Validate that target template is activatable with minimum required data.
     */
    public void assertCanActivate(CycleTemplateEntity template) {
        if (!StringUtils.hasText(template.getName()) || template.getCurrentVersionId() == null) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ACTIVATE_INVALID);
        }
        assertFormalCycleLength(template.getCycleLength());
    }

    /**
     * Validate active template editing constraints.
     */
    public void assertActiveUpdateAllowed(
            CycleTemplateEntity template,
            UpdateCycleTemplateRequest request,
            UserActiveCycleEntity activeCycle) {
        Integer currentCycleLength = template.getCycleLength();
        if (request.cycleLength() != null && !Objects.equals(request.cycleLength(), currentCycleLength)) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_EDIT_FORBIDDEN,
                    "active template cycleLength cannot be changed");
        }
        validateDayRequests(currentCycleLength, request.days(), resolveEditableFromDayIndex(activeCycle));
    }

    public Integer resolveEditableFromDayIndex(UserActiveCycleEntity activeCycle) {
        if (activeCycle == null || activeCycle.getCurrentDayIndex() == null || activeCycle.getCurrentDayIndex() < 1) {
            return 1;
        }
        return activeCycle.getCurrentDayIndex();
    }

    public String normalizeDayName(String dayName, int dayIndex) {
        return StringUtils.hasText(dayName) ? dayName.trim() : "Day " + dayIndex;
    }

    public boolean canActivate(CycleTemplateEntity template) {
        try {
            assertCanActivate(template);
            return "draft".equals(template.getStatus()) || "inactive".equals(template.getStatus());
        } catch (BusinessException exception) {
            return false;
        }
    }

    private void assertCycleLengthInRange(Integer cycleLength) {
        if (cycleLength < 1 || cycleLength > 7) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_CYCLE_LENGTH_INVALID);
        }
    }
}
