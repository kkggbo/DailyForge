package com.dailyforge.modules.plan.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.exercise.application.service.SystemExerciseLookupService;
import com.dailyforge.modules.plan.application.assembler.CycleTemplateAssembler;
import com.dailyforge.modules.plan.domain.service.CycleActivationDomainService;
import com.dailyforge.modules.plan.domain.service.CycleTemplatePolicyService;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.domain.service.ExerciseStructurePolicyService;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.UserActiveCycleEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleRunMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.UserActiveCycleMapper;
import com.dailyforge.modules.plan.interfaces.dto.ActivateCycleTemplateRequest;
import com.dailyforge.modules.plan.interfaces.vo.ActivateCycleTemplateResponse;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CycleTemplateActivationApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CycleTemplateActivationApplicationService.class);

    private final PlanUserSupportService planUserSupportService;
    private final CycleTemplateMapper cycleTemplateMapper;
    private final UserActiveCycleMapper userActiveCycleMapper;
    private final CycleRunMapper cycleRunMapper;
    private final SystemExerciseLookupService systemExerciseLookupService;
    private final CycleTemplatePolicyService cycleTemplatePolicyService;
    private final ExerciseStructurePolicyService exerciseStructurePolicyService;
    private final CycleTemplateVersionDomainService cycleTemplateVersionDomainService;
    private final CycleActivationDomainService cycleActivationDomainService;
    private final CycleTemplateAssembler cycleTemplateAssembler;

    public CycleTemplateActivationApplicationService(
            PlanUserSupportService planUserSupportService,
            CycleTemplateMapper cycleTemplateMapper,
            UserActiveCycleMapper userActiveCycleMapper,
            CycleRunMapper cycleRunMapper,
            SystemExerciseLookupService systemExerciseLookupService,
            CycleTemplatePolicyService cycleTemplatePolicyService,
            ExerciseStructurePolicyService exerciseStructurePolicyService,
            CycleTemplateVersionDomainService cycleTemplateVersionDomainService,
            CycleActivationDomainService cycleActivationDomainService,
            CycleTemplateAssembler cycleTemplateAssembler) {
        this.planUserSupportService = planUserSupportService;
        this.cycleTemplateMapper = cycleTemplateMapper;
        this.userActiveCycleMapper = userActiveCycleMapper;
        this.cycleRunMapper = cycleRunMapper;
        this.systemExerciseLookupService = systemExerciseLookupService;
        this.cycleTemplatePolicyService = cycleTemplatePolicyService;
        this.exerciseStructurePolicyService = exerciseStructurePolicyService;
        this.cycleTemplateVersionDomainService = cycleTemplateVersionDomainService;
        this.cycleActivationDomainService = cycleActivationDomainService;
        this.cycleTemplateAssembler = cycleTemplateAssembler;
    }

    /**
     * Activate one draft or inactive template and switch current run context.
     */
    @Transactional
    public ActivateCycleTemplateResponse activateTemplate(Long templateId, ActivateCycleTemplateRequest request) {
        Long userId = planUserSupportService.requireActiveUserId();
        CycleTemplateEntity targetTemplate = cycleTemplateMapper.selectByIdAndUserIdForUpdate(templateId, userId);
        if (targetTemplate == null || "deleted".equals(targetTemplate.getStatus())) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_NOT_FOUND);
        }
        if ("active".equals(targetTemplate.getStatus())) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_STATUS_INVALID);
        }
        cycleTemplatePolicyService.assertTemplateStatus(targetTemplate, "draft", "inactive");
        cycleTemplatePolicyService.assertCanActivate(targetTemplate);
        validateCurrentVersionStructure(targetTemplate);

        UserActiveCycleEntity activeCycle = userActiveCycleMapper.selectByUserIdForUpdate(userId);
        Long previousActiveTemplateId = null;
        if (activeCycle != null && !targetTemplate.getId().equals(activeCycle.getTemplateId())) {
            if (!Boolean.TRUE.equals(request.confirmSwitch())) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_SWITCH_CONFIRM_REQUIRED);
            }
            previousActiveTemplateId = activeCycle.getTemplateId();
            closeCurrentActiveContext(userId, activeCycle);
        }

        targetTemplate.setStatus("active");
        cycleTemplateMapper.updateById(targetTemplate);

        CycleRunEntity newRun = cycleActivationDomainService.createNewRun(userId, targetTemplate);
        UserActiveCycleEntity nextActiveCycle =
                cycleActivationDomainService.buildActiveCycleContext(activeCycle, userId, targetTemplate, newRun);
        if (activeCycle == null) {
            userActiveCycleMapper.insert(nextActiveCycle);
        } else {
            userActiveCycleMapper.updateById(nextActiveCycle);
        }

        log.debug("Template activated. userId={}, previousActiveTemplateId={}, targetTemplateId={}, newRunId={}",
                userId, previousActiveTemplateId, targetTemplate.getId(), newRun.getId());
        return cycleTemplateAssembler.toActivateResponse(targetTemplate, 1, previousActiveTemplateId);
    }

    private void validateCurrentVersionStructure(CycleTemplateEntity targetTemplate) {
        var snapshot = cycleTemplateVersionDomainService.loadVersionSnapshot(targetTemplate.getCurrentVersionId());
        Set<Long> exerciseIds = snapshot.days().stream()
                .flatMap(day -> day.exercises().stream())
                .map(CycleTemplateVersionDomainService.ExerciseSnapshot::exerciseId)
                .collect(Collectors.toSet());
        if (exerciseIds.isEmpty()) {
            return;
        }
        Map<Long, SystemExerciseLookupResult> exerciseMap =
                new LinkedHashMap<>(systemExerciseLookupService.loadExercisesByIds(exerciseIds));
        if (exerciseMap.size() != exerciseIds.size()) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_EXERCISE_NOT_FOUND);
        }
        for (SystemExerciseLookupResult exercise : exerciseMap.values()) {
            if (exercise.ownerUserId() != null || exercise.isActive() == null || exercise.isActive() != 1) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_SYSTEM_EXERCISE_REQUIRED);
            }
        }
        exerciseStructurePolicyService.validateVersionSnapshot(snapshot, exerciseMap);
    }

    private void closeCurrentActiveContext(Long userId, UserActiveCycleEntity activeCycle) {
        CycleTemplateEntity oldTemplate =
                cycleTemplateMapper.selectByIdAndUserIdForUpdate(activeCycle.getTemplateId(), userId);
        if (oldTemplate != null) {
            oldTemplate.setStatus("inactive");
            cycleTemplateMapper.updateById(oldTemplate);
        }
        CycleRunEntity oldRun = cycleRunMapper.selectById(activeCycle.getCurrentRunId());
        if (oldRun != null && "active".equals(oldRun.getStatus())) {
            oldRun.setStatus("completed");
            oldRun.setCompletedAt(LocalDateTime.now());
            cycleRunMapper.updateById(oldRun);
        }
    }
}
