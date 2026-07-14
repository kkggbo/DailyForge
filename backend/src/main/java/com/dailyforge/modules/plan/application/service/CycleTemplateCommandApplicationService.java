package com.dailyforge.modules.plan.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.plan.application.assembler.CycleTemplateAssembler;
import com.dailyforge.modules.plan.domain.service.CycleTemplatePolicyService;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateVersionEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.ExerciseReadEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.UserActiveCycleEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleRunMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.ExerciseReadMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.UserActiveCycleMapper;
import com.dailyforge.modules.plan.interfaces.dto.CopyCycleTemplateRequest;
import com.dailyforge.modules.plan.interfaces.dto.CreateDraftCycleTemplateRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateExerciseRequest;
import com.dailyforge.modules.plan.interfaces.dto.UpdateCycleTemplateRequest;
import com.dailyforge.modules.plan.interfaces.dto.UpdateDraftCycleTemplateRequest;
import com.dailyforge.modules.plan.interfaces.vo.CopyCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.CreateDraftCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.DeleteCycleTemplateResponse;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CycleTemplateCommandApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CycleTemplateCommandApplicationService.class);

    private final PlanUserSupportService planUserSupportService;
    private final CycleTemplateMapper cycleTemplateMapper;
    private final CycleTemplateVersionDomainService cycleTemplateVersionDomainService;
    private final CycleTemplatePolicyService cycleTemplatePolicyService;
    private final ExerciseReadMapper exerciseReadMapper;
    private final UserActiveCycleMapper userActiveCycleMapper;
    private final CycleRunMapper cycleRunMapper;
    private final CycleTemplateAssembler cycleTemplateAssembler;

    public CycleTemplateCommandApplicationService(
            PlanUserSupportService planUserSupportService,
            CycleTemplateMapper cycleTemplateMapper,
            CycleTemplateVersionDomainService cycleTemplateVersionDomainService,
            CycleTemplatePolicyService cycleTemplatePolicyService,
            ExerciseReadMapper exerciseReadMapper,
            UserActiveCycleMapper userActiveCycleMapper,
            CycleRunMapper cycleRunMapper,
            CycleTemplateAssembler cycleTemplateAssembler) {
        this.planUserSupportService = planUserSupportService;
        this.cycleTemplateMapper = cycleTemplateMapper;
        this.cycleTemplateVersionDomainService = cycleTemplateVersionDomainService;
        this.cycleTemplatePolicyService = cycleTemplatePolicyService;
        this.exerciseReadMapper = exerciseReadMapper;
        this.userActiveCycleMapper = userActiveCycleMapper;
        this.cycleRunMapper = cycleRunMapper;
        this.cycleTemplateAssembler = cycleTemplateAssembler;
    }

    /**
     * Create one new draft template and initialize an empty or populated first version.
     */
    @Transactional
    public CreateDraftCycleTemplateResponse createDraft(CreateDraftCycleTemplateRequest request) {
        Long userId = planUserSupportService.requireActiveUserId();
        cycleTemplatePolicyService.validateDraftCycleLength(request.cycleLength());
        cycleTemplatePolicyService.validateDayRequests(request.cycleLength(), request.days());
        Map<Long, ExerciseReadEntity> exerciseMap = loadAndValidateExercises(request.days());

        CycleTemplateEntity template = new CycleTemplateEntity();
        template.setUserId(userId);
        template.setName(request.templateName().trim());
        template.setCycleLength(request.cycleLength());
        template.setGoalType(request.goalType());
        template.setStatus("draft");
        cycleTemplateMapper.insert(template);

        CycleTemplateVersionEntity version =
                cycleTemplateVersionDomainService.createVersion(template.getId(), "manual", "draft_create");
        cycleTemplateVersionDomainService.saveFullVersionContent(version.getId(), request.days(), exerciseMap);
        template.setCurrentVersionId(version.getId());
        cycleTemplateMapper.updateById(template);

        int configuredDayCount = request.days() == null ? 0 : request.days().size();
        log.debug("Draft template created. userId={}, templateId={}, versionNo={}, cycleLength={}, configuredDayCount={}",
                userId, template.getId(), version.getVersionNo(), template.getCycleLength(), configuredDayCount);
        return cycleTemplateAssembler.toCreateDraftResponse(template);
    }

    /**
     * Save one existing draft template as a new version.
     */
    @Transactional
    public CreateDraftCycleTemplateResponse updateDraft(Long templateId, UpdateDraftCycleTemplateRequest request) {
        Long userId = planUserSupportService.requireActiveUserId();
        CycleTemplateEntity template = cycleTemplateMapper.selectByIdAndUserIdForUpdate(templateId, userId);
        if (template == null || "deleted".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_NOT_FOUND);
        }
        cycleTemplatePolicyService.assertTemplateStatus(template, "draft");
        cycleTemplatePolicyService.validateDraftCycleLength(request.cycleLength());
        cycleTemplatePolicyService.validateDayRequests(request.cycleLength(), request.days());
        Map<Long, ExerciseReadEntity> exerciseMap = loadAndValidateExercises(request.days());

        template.setName(request.templateName().trim());
        template.setCycleLength(request.cycleLength());
        template.setGoalType(request.goalType());
        CycleTemplateVersionEntity version =
                cycleTemplateVersionDomainService.createVersion(template.getId(), "manual", "draft_update");
        cycleTemplateVersionDomainService.saveFullVersionContent(version.getId(), request.days(), exerciseMap);
        template.setCurrentVersionId(version.getId());
        cycleTemplateMapper.updateById(template);

        int configuredDayCount = request.days() == null ? 0 : request.days().size();
        log.debug("Draft template updated. userId={}, templateId={}, versionNo={}, cycleLength={}, configuredDayCount={}",
                userId, template.getId(), version.getVersionNo(), template.getCycleLength(), configuredDayCount);
        return cycleTemplateAssembler.toCreateDraftResponse(template);
    }

    /**
     * Update an inactive or active formal template.
     */
    @Transactional
    public CreateDraftCycleTemplateResponse updateFormal(Long templateId, UpdateCycleTemplateRequest request) {
        Long userId = planUserSupportService.requireActiveUserId();
        CycleTemplateEntity template = cycleTemplateMapper.selectByIdAndUserIdForUpdate(templateId, userId);
        if (template == null || "deleted".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_NOT_FOUND);
        }
        cycleTemplatePolicyService.assertTemplateStatus(template, "inactive", "active");

        if ("inactive".equals(template.getStatus())) {
            return updateInactiveTemplate(template, request, userId);
        }
        return updateActiveTemplate(template, request, userId);
    }

    /**
     * Copy an existing template into a new draft.
     */
    @Transactional
    public CopyCycleTemplateResponse copyTemplate(Long templateId, CopyCycleTemplateRequest request) {
        Long userId = planUserSupportService.requireActiveUserId();
        CycleTemplateEntity sourceTemplate = cycleTemplateMapper.selectByIdAndUserId(templateId, userId);
        if (sourceTemplate == null || "deleted".equals(sourceTemplate.getStatus())) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_NOT_FOUND);
        }

        CycleTemplateEntity targetTemplate = new CycleTemplateEntity();
        targetTemplate.setUserId(userId);
        targetTemplate.setName(request.templateName().trim());
        targetTemplate.setCycleLength(sourceTemplate.getCycleLength());
        targetTemplate.setGoalType(sourceTemplate.getGoalType());
        targetTemplate.setStatus("draft");
        cycleTemplateMapper.insert(targetTemplate);

        CycleTemplateVersionEntity version =
                cycleTemplateVersionDomainService.createVersion(targetTemplate.getId(), "copy", "copy_from_" + templateId);
        if (sourceTemplate.getCurrentVersionId() != null) {
            cycleTemplateVersionDomainService.cloneWholeVersion(sourceTemplate.getCurrentVersionId(), version.getId());
        }
        targetTemplate.setCurrentVersionId(version.getId());
        cycleTemplateMapper.updateById(targetTemplate);

        log.debug("Template copied. userId={}, sourceTemplateId={}, targetTemplateId={}, versionNo={}",
                userId, templateId, targetTemplate.getId(), version.getVersionNo());
        return cycleTemplateAssembler.toCopyResponse(targetTemplate);
    }

    /**
     * Soft delete one draft or inactive template.
     */
    @Transactional
    public DeleteCycleTemplateResponse deleteTemplate(Long templateId) {
        Long userId = planUserSupportService.requireActiveUserId();
        CycleTemplateEntity template = cycleTemplateMapper.selectByIdAndUserIdForUpdate(templateId, userId);
        if (template == null || "deleted".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_NOT_FOUND);
        }
        cycleTemplatePolicyService.assertCanDelete(template);
        template.setStatus("deleted");
        cycleTemplateMapper.updateById(template);
        log.debug("Template deleted. userId={}, templateId={}, previousStatus={}",
                userId, templateId, template.getStatus());
        return cycleTemplateAssembler.toDeleteResponse(template);
    }

    private CreateDraftCycleTemplateResponse updateInactiveTemplate(
            CycleTemplateEntity template,
            UpdateCycleTemplateRequest request,
            Long userId) {
        Integer cycleLength = request.cycleLength() == null ? template.getCycleLength() : request.cycleLength();
        cycleTemplatePolicyService.assertFormalCycleLength(cycleLength);
        cycleTemplatePolicyService.validateDayRequests(cycleLength, request.days());
        Map<Long, ExerciseReadEntity> exerciseMap = loadAndValidateExercises(request.days());

        template.setName(request.templateName().trim());
        template.setGoalType(request.goalType());
        template.setCycleLength(cycleLength);
        CycleTemplateVersionEntity version =
                cycleTemplateVersionDomainService.createVersion(template.getId(), "manual", "formal_update");
        cycleTemplateVersionDomainService.saveFullVersionContent(version.getId(), request.days(), exerciseMap);
        template.setCurrentVersionId(version.getId());
        cycleTemplateMapper.updateById(template);

        log.debug("Inactive formal template updated. userId={}, templateId={}, versionNo={}, cycleLength={}",
                userId, template.getId(), version.getVersionNo(), cycleLength);
        return cycleTemplateAssembler.toCreateDraftResponse(template);
    }

    private CreateDraftCycleTemplateResponse updateActiveTemplate(
            CycleTemplateEntity template,
            UpdateCycleTemplateRequest request,
            Long userId) {
        UserActiveCycleEntity activeCycle = userActiveCycleMapper.selectByUserIdForUpdate(userId);
        if (activeCycle == null || !template.getId().equals(activeCycle.getTemplateId())) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ACTIVE_NOT_FOUND);
        }
        cycleTemplatePolicyService.assertActiveUpdateAllowed(template, request, activeCycle);
        Map<Long, ExerciseReadEntity> exerciseMap = loadAndValidateExercises(request.days());

        template.setName(request.templateName().trim());
        template.setGoalType(request.goalType());
        CycleTemplateVersionEntity version =
                cycleTemplateVersionDomainService.createVersion(template.getId(), "active_patch", "active_future_patch");
        cycleTemplateVersionDomainService.cloneLockedDaysAndReplaceEditableDays(
                template.getCurrentVersionId(),
                version.getId(),
                cycleTemplatePolicyService.resolveEditableFromDayIndex(activeCycle),
                request.days(),
                exerciseMap);
        template.setCurrentVersionId(version.getId());
        cycleTemplateMapper.updateById(template);

        activeCycle.setTemplateVersionId(version.getId());
        userActiveCycleMapper.updateById(activeCycle);

        CycleRunEntity currentRun = cycleRunMapper.selectById(activeCycle.getCurrentRunId());
        if (currentRun != null) {
            currentRun.setTemplateVersionId(version.getId());
            currentRun.setUpdatedAt(LocalDateTime.now());
            cycleRunMapper.updateById(currentRun);
        }

        log.debug("Active formal template updated. userId={}, templateId={}, oldVersionId={}, newVersionId={}, editableFromDayIndex={}",
                userId,
                template.getId(),
                template.getCurrentVersionId(),
                version.getId(),
                cycleTemplatePolicyService.resolveEditableFromDayIndex(activeCycle));
        return cycleTemplateAssembler.toCreateDraftResponse(template);
    }

    private Map<Long, ExerciseReadEntity> loadAndValidateExercises(List<CycleTemplateDayRequest> days) {
        Set<Long> exerciseIds = new LinkedHashSet<>();
        if (days != null) {
            for (CycleTemplateDayRequest day : days) {
                List<CycleTemplateExerciseRequest> exercises = day.exercises();
                if (exercises == null) {
                    continue;
                }
                for (CycleTemplateExerciseRequest exercise : exercises) {
                    exerciseIds.add(exercise.exerciseId());
                }
            }
        }
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ExerciseReadEntity> exerciseEntities = exerciseReadMapper.selectByIds(List.copyOf(exerciseIds));
        if (exerciseEntities.size() != exerciseIds.size()) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_EXERCISE_NOT_FOUND);
        }

        Map<Long, ExerciseReadEntity> exerciseMap = new LinkedHashMap<>();
        for (ExerciseReadEntity exercise : exerciseEntities) {
            if (exercise.getOwnerUserId() != null || exercise.getIsActive() == null || exercise.getIsActive() != 1) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_SYSTEM_EXERCISE_REQUIRED);
            }
            exerciseMap.put(exercise.getId(), exercise);
        }
        return exerciseMap;
    }
}
