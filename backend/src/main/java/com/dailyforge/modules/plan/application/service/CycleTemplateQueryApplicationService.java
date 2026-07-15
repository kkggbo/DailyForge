package com.dailyforge.modules.plan.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.plan.application.assembler.CycleTemplateAssembler;
import com.dailyforge.modules.plan.domain.model.MetricKey;
import com.dailyforge.modules.plan.domain.service.CycleTemplatePolicyService;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.DaySnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.ExerciseSnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.ItemSnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.MetricSnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.VersionSnapshot;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.UserActiveCycleEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleRunMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateDayMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.UserActiveCycleMapper;
import com.dailyforge.modules.plan.interfaces.vo.CycleTemplateDayResponse;
import com.dailyforge.modules.plan.interfaces.vo.CycleTemplateDetailResponse;
import com.dailyforge.modules.plan.interfaces.vo.CycleTemplateExerciseResponse;
import com.dailyforge.modules.plan.interfaces.vo.CycleTemplateItemResponse;
import com.dailyforge.modules.plan.interfaces.vo.CycleTemplateMetricResponse;
import com.dailyforge.modules.plan.interfaces.vo.CurrentActiveCycleTemplateResponse;
import com.dailyforge.modules.plan.interfaces.vo.DraftCycleTemplateListResponse;
import com.dailyforge.modules.plan.interfaces.vo.DraftCycleTemplateSummary;
import com.dailyforge.modules.plan.interfaces.vo.FormalCycleTemplateListResponse;
import com.dailyforge.modules.plan.interfaces.vo.FormalCycleTemplateSummary;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CycleTemplateQueryApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CycleTemplateQueryApplicationService.class);

    private final PlanUserSupportService planUserSupportService;
    private final CycleTemplateMapper cycleTemplateMapper;
    private final UserActiveCycleMapper userActiveCycleMapper;
    private final CycleRunMapper cycleRunMapper;
    private final CycleTemplateDayMapper cycleTemplateDayMapper;
    private final CycleTemplateVersionDomainService cycleTemplateVersionDomainService;
    private final CycleTemplatePolicyService cycleTemplatePolicyService;
    private final CycleTemplateAssembler cycleTemplateAssembler;

    public CycleTemplateQueryApplicationService(
            PlanUserSupportService planUserSupportService,
            CycleTemplateMapper cycleTemplateMapper,
            UserActiveCycleMapper userActiveCycleMapper,
            CycleRunMapper cycleRunMapper,
            CycleTemplateDayMapper cycleTemplateDayMapper,
            CycleTemplateVersionDomainService cycleTemplateVersionDomainService,
            CycleTemplatePolicyService cycleTemplatePolicyService,
            CycleTemplateAssembler cycleTemplateAssembler) {
        this.planUserSupportService = planUserSupportService;
        this.cycleTemplateMapper = cycleTemplateMapper;
        this.userActiveCycleMapper = userActiveCycleMapper;
        this.cycleRunMapper = cycleRunMapper;
        this.cycleTemplateDayMapper = cycleTemplateDayMapper;
        this.cycleTemplateVersionDomainService = cycleTemplateVersionDomainService;
        this.cycleTemplatePolicyService = cycleTemplatePolicyService;
        this.cycleTemplateAssembler = cycleTemplateAssembler;
    }

    /**
     * Return active and inactive templates for the current user.
     */
    public FormalCycleTemplateListResponse getFormalTemplates() {
        Long userId = planUserSupportService.requireActiveUserId();
        UserActiveCycleEntity activeCycle = userActiveCycleMapper.selectById(userId);
        List<CycleTemplateEntity> templates = cycleTemplateMapper.selectList(new LambdaQueryWrapper<CycleTemplateEntity>()
                .eq(CycleTemplateEntity::getUserId, userId)
                .in(CycleTemplateEntity::getStatus, List.of("active", "inactive"))
                .orderByDesc(CycleTemplateEntity::getUpdatedAt));

        List<FormalCycleTemplateSummary> records = new ArrayList<>();
        for (CycleTemplateEntity template : templates) {
            Integer currentDayIndex = null;
            if (activeCycle != null && template.getId().equals(activeCycle.getTemplateId())) {
                currentDayIndex = activeCycle.getCurrentDayIndex();
            }
            records.add(cycleTemplateAssembler.toFormalSummary(template, currentDayIndex));
        }

        log.debug("Formal templates loaded. userId={}, count={}", userId, records.size());
        return new FormalCycleTemplateListResponse(activeCycle == null ? null : activeCycle.getTemplateId(), records);
    }

    /**
     * Return draft templates for the current user.
     */
    public DraftCycleTemplateListResponse getDraftTemplates() {
        Long userId = planUserSupportService.requireActiveUserId();
        List<CycleTemplateEntity> templates = cycleTemplateMapper.selectList(new LambdaQueryWrapper<CycleTemplateEntity>()
                .eq(CycleTemplateEntity::getUserId, userId)
                .eq(CycleTemplateEntity::getStatus, "draft")
                .orderByDesc(CycleTemplateEntity::getUpdatedAt));

        List<DraftCycleTemplateSummary> records = new ArrayList<>();
        for (CycleTemplateEntity template : templates) {
            int configuredDayCount = template.getCurrentVersionId() == null
                    ? 0
                    : cycleTemplateDayMapper.selectByVersionId(template.getCurrentVersionId()).size();
            records.add(cycleTemplateAssembler.toDraftSummary(template, configuredDayCount));
        }

        log.debug("Draft templates loaded. userId={}, count={}", userId, records.size());
        return new DraftCycleTemplateListResponse(records);
    }

    /**
     * Return one template detail with nested v2 structure.
     */
    public CycleTemplateDetailResponse getTemplateDetail(Long templateId) {
        Long userId = planUserSupportService.requireActiveUserId();
        CycleTemplateEntity template = cycleTemplateMapper.selectByIdAndUserId(templateId, userId);
        if (template == null || "deleted".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_NOT_FOUND);
        }

        UserActiveCycleEntity activeCycle = "active".equals(template.getStatus())
                ? userActiveCycleMapper.selectById(userId)
                : null;
        Integer currentDayIndex = activeCycle == null ? null : activeCycle.getCurrentDayIndex();
        Integer editableFromDayIndex = "active".equals(template.getStatus())
                ? cycleTemplatePolicyService.resolveEditableFromDayIndex(activeCycle)
                : 1;

        List<CycleTemplateDayResponse> dayResponses = buildDayResponses(
                cycleTemplateVersionDomainService.loadVersionSnapshot(template.getCurrentVersionId()),
                editableFromDayIndex,
                "active".equals(template.getStatus()));

        boolean canActivate = cycleTemplatePolicyService.canActivate(template);
        boolean canDelete = "draft".equals(template.getStatus()) || "inactive".equals(template.getStatus());
        log.debug("Template detail loaded. userId={}, templateId={}, status={}, editableFromDayIndex={}, dayCount={}",
                userId, templateId, template.getStatus(), editableFromDayIndex, dayResponses.size());
        return new CycleTemplateDetailResponse(
                template.getId(),
                template.getName(),
                template.getGoalType(),
                template.getStatus(),
                template.getCycleLength(),
                "active".equals(template.getStatus()),
                currentDayIndex,
                editableFromDayIndex,
                canActivate,
                canDelete,
                template.getCreatedAt(),
                template.getUpdatedAt(),
                dayResponses);
    }

    /**
     * Return current active template summary for training entry.
     */
    public CurrentActiveCycleTemplateResponse getCurrentActiveTemplate() {
        Long userId = planUserSupportService.requireActiveUserId();
        UserActiveCycleEntity activeCycle = userActiveCycleMapper.selectById(userId);
        if (activeCycle == null) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ACTIVE_NOT_FOUND);
        }

        CycleTemplateEntity template = cycleTemplateMapper.selectByIdAndUserId(activeCycle.getTemplateId(), userId);
        if (template == null || !"active".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ACTIVE_NOT_FOUND);
        }

        var currentDay = cycleTemplateDayMapper.selectByVersionId(activeCycle.getTemplateVersionId())
                .stream()
                .filter(day -> day.getDayIndex().equals(activeCycle.getCurrentDayIndex()))
                .findFirst()
                .orElse(null);
        CycleRunEntity currentRun = cycleRunMapper.selectById(activeCycle.getCurrentRunId());
        log.debug("Current active template loaded. userId={}, templateId={}, currentDayIndex={}",
                userId, template.getId(), activeCycle.getCurrentDayIndex());
        return cycleTemplateAssembler.toCurrentActiveResponse(
                template,
                currentDay,
                activeCycle.getCurrentDayIndex(),
                currentRun);
    }

    private List<CycleTemplateDayResponse> buildDayResponses(
            VersionSnapshot snapshot,
            Integer editableFromDayIndex,
            boolean active) {
        if (snapshot == null || snapshot.days().isEmpty()) {
            return Collections.emptyList();
        }
        List<CycleTemplateDayResponse> responses = new ArrayList<>();
        for (DaySnapshot daySnapshot : snapshot.days()) {
            boolean isLocked = active && daySnapshot.dayIndex() < editableFromDayIndex;
            List<CycleTemplateExerciseResponse> exercises = new ArrayList<>();
            for (ExerciseSnapshot exerciseSnapshot : daySnapshot.exercises()) {
                exercises.add(toExerciseResponse(exerciseSnapshot));
            }
            responses.add(new CycleTemplateDayResponse(
                    daySnapshot.dayIndex(),
                    daySnapshot.dayName(),
                    exercises.isEmpty(),
                    isLocked,
                    exercises));
        }
        return responses;
    }

    private CycleTemplateExerciseResponse toExerciseResponse(ExerciseSnapshot snapshot) {
        List<CycleTemplateItemResponse> items = new ArrayList<>();
        for (ItemSnapshot item : snapshot.items()) {
            items.add(toItemResponse(item));
        }
        return new CycleTemplateExerciseResponse(
                snapshot.sortOrder(),
                snapshot.exerciseId(),
                snapshot.exerciseNameSnapshot(),
                snapshot.structureType(),
                snapshot.note(),
                items);
    }

    private CycleTemplateItemResponse toItemResponse(ItemSnapshot snapshot) {
        List<CycleTemplateMetricResponse> metrics = new ArrayList<>();
        for (MetricSnapshot metric : snapshot.metrics()) {
            metrics.add(new CycleTemplateMetricResponse(
                    metric.sortOrder(),
                    metric.metricKey(),
                    metric.metricValueNumber(),
                    resolveMetricUnit(metric.metricKey())));
        }
        return new CycleTemplateItemResponse(
                snapshot.itemIndex(),
                snapshot.itemType(),
                snapshot.itemName(),
                snapshot.note(),
                metrics);
    }

    private String resolveMetricUnit(String metricKeyValue) {
        MetricKey metricKey = MetricKey.fromValue(metricKeyValue);
        return metricKey == null ? null : metricKey.getUnit();
    }
}
