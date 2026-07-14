package com.dailyforge.modules.plan.application.assembler;

import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleDayExerciseEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateDayEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.interfaces.vo.ActivateCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.CopyCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.CreateDraftCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.CurrentActiveCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.DeleteCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.DraftCycleTemplateSummary;
import com.dailyforge.modules.plan.interfaces.vo.FormalCycleTemplateSummary;
import com.fasterxml.jackson.databind.JsonNode;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CycleTemplateAssembler {

    default FormalCycleTemplateSummary toFormalSummary(CycleTemplateEntity template, Integer currentDayIndex) {
        return new FormalCycleTemplateSummary(
                template.getId(),
                template.getName(),
                template.getCycleLength(),
                template.getGoalType(),
                template.getStatus(),
                "active".equals(template.getStatus()),
                currentDayIndex,
                template.getUpdatedAt());
    }

    default DraftCycleTemplateSummary toDraftSummary(CycleTemplateEntity template, int configuredDayCount) {
        return new DraftCycleTemplateSummary(
                template.getId(),
                template.getName(),
                template.getCycleLength(),
                configuredDayCount,
                template.getCreatedAt(),
                template.getUpdatedAt());
    }

    default CreateDraftCycleTemplateResponse toCreateDraftResponse(CycleTemplateEntity template) {
        return new CreateDraftCycleTemplateResponse(template.getId(), template.getStatus());
    }

    default CopyCycleTemplateResponse toCopyResponse(CycleTemplateEntity template) {
        return new CopyCycleTemplateResponse(template.getId(), template.getStatus());
    }

    default ActivateCycleTemplateResponse toActivateResponse(
            CycleTemplateEntity template,
            Integer currentDayIndex,
            Long previousActiveTemplateId) {
        return new ActivateCycleTemplateResponse(
                template.getId(),
                template.getStatus(),
                currentDayIndex,
                previousActiveTemplateId);
    }

    default DeleteCycleTemplateResponse toDeleteResponse(CycleTemplateEntity template) {
        return new DeleteCycleTemplateResponse(template.getId(), template.getStatus());
    }

    default CurrentActiveCycleTemplateResponse toCurrentActiveResponse(
            CycleTemplateEntity template,
            CycleTemplateDayEntity currentDay,
            Integer currentDayIndex,
            CycleRunEntity currentRun) {
        return new CurrentActiveCycleTemplateResponse(
                template.getId(),
                template.getName(),
                template.getCycleLength(),
                currentDayIndex,
                currentDay == null ? null : currentDay.getDayName(),
                currentDayIndex,
                currentRun == null ? null : currentRun.getStartedAt());
    }

    default com.dailyforge.modules.plan.interfaces.vo.CycleTemplateExerciseResponse toExerciseResponse(
            CycleDayExerciseEntity entity,
            JsonNode targetExtraJson) {
        return new com.dailyforge.modules.plan.interfaces.vo.CycleTemplateExerciseResponse(
                entity.getSortOrder(),
                entity.getExerciseId(),
                entity.getExerciseNameSnapshot(),
                entity.getTargetSets(),
                entity.getTargetRepsMin(),
                entity.getTargetRepsMax(),
                entity.getTargetWeightKg(),
                entity.getTargetDurationSeconds(),
                entity.getTargetRestSeconds(),
                entity.getTargetRpe(),
                entity.getNotes(),
                targetExtraJson);
    }
}
