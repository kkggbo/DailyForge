package com.dailyforge.modules.aicoach.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.domain.model.CycleSummaryValidatedResult;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskRecordMapper;
import com.dailyforge.modules.aicoach.interfaces.dto.CycleSummaryRequest;
import com.dailyforge.modules.aicoach.interfaces.vo.CycleSummaryTaskResultResponse;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.DaySnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.VersionSnapshot;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleRunMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserCurrentBodyMetricsMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserProfileMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiCycleSummaryService {

    private final AiTaskRecordMapper taskMapper;
    private final CycleRunMapper cycleRunMapper;
    private final CycleTemplateMapper templateMapper;
    private final CycleTemplateVersionDomainService versionService;
    private final TrainingSessionMapper sessionMapper;
    private final UserProfileMapper profileMapper;
    private final UserCurrentBodyMetricsMapper metricsMapper;
    private final ObjectMapper objectMapper;

    public AiCycleSummaryService(
            AiTaskRecordMapper taskMapper,
            CycleRunMapper cycleRunMapper,
            CycleTemplateMapper templateMapper,
            CycleTemplateVersionDomainService versionService,
            TrainingSessionMapper sessionMapper,
            UserProfileMapper profileMapper,
            UserCurrentBodyMetricsMapper metricsMapper,
            ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.cycleRunMapper = cycleRunMapper;
        this.templateMapper = templateMapper;
        this.versionService = versionService;
        this.sessionMapper = sessionMapper;
        this.profileMapper = profileMapper;
        this.metricsMapper = metricsMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void persistSuccessfulResult(
            Long taskId,
            String inputSummaryJson,
            CycleSummaryValidatedResult validatedResult) {
        AiTaskRecordEntity task = requireTask(taskId, "cycle_summary");
        CycleSummaryRequest request = read(task.getRequestPayloadJson(), CycleSummaryRequest.class);
        Long cycleRunId = task.getRelatedEntityId() != null ? task.getRelatedEntityId() : request.cycleRunId();
        CycleRunEntity cycleRun = cycleRunMapper.selectById(cycleRunId);
        if (cycleRun == null || !task.getUserId().equals(cycleRun.getUserId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!"completed".equals(cycleRun.getStatus())) {
            throw new BusinessException(ErrorCode.AI_CYCLE_RUN_NOT_COMPLETED);
        }

        CycleTemplateEntity template = templateMapper.selectById(cycleRun.getTemplateId());
        UserProfileEntity profile = profileMapper.selectById(task.getUserId());
        UserCurrentBodyMetricsEntity metrics = metricsMapper.selectById(task.getUserId());
        List<TrainingSessionEntity> sessions = sessionMapper.selectByCycleRunIdAndUserId(cycleRunId, task.getUserId());
        VersionSnapshot versionSnapshot = versionService.loadVersionSnapshot(cycleRun.getTemplateVersionId());
        CycleSummaryTaskResultResponse result = new CycleSummaryTaskResultResponse(
                cycleRun.getId(),
                cycleRun.getTemplateId(),
                resolveTemplateName(sessions, template),
                cycleRun.getRunNo(),
                resolveCycleLength(cycleRun, template, versionSnapshot),
                validatedResult.output().executionOverview(),
                validatedResult.output().strengths(),
                validatedResult.output().issues(),
                validatedResult.output().causeAnalysis(),
                validatedResult.output().nextCycleSuggestions(),
                validatedResult.output().risks(),
                StringUtils.hasText(validatedResult.output().dataCompletenessNotice())
                        ? validatedResult.output().dataCompletenessNotice()
                        : buildRecommendedNotice(profile, metrics));

        LocalDateTime completedAt = LocalDateTime.now();
        task.setInputSummaryJson(inputSummaryJson);
        task.setResultJson(write(result));
        task.setOutputPreview(limit(result.executionOverview(), 1000));
        task.setLatencyMs(task.getStartedAt() == null ? 0 : (int) Duration.between(task.getStartedAt(), completedAt).toMillis());
        task.setCompletedAt(completedAt);
        task.setStatus("succeeded");
        task.setErrorCode(null);
        task.setErrorMessage(null);
        taskMapper.updateById(task);
    }

    private String buildRecommendedNotice(UserProfileEntity profile, UserCurrentBodyMetricsEntity metrics) {
        List<String> missing = new ArrayList<>();
        if (profile == null || !StringUtils.hasText(profile.getGoalType())) {
            missing.add("goalType");
        }
        if (profile == null || !StringUtils.hasText(profile.getTrainingLevel())) {
            missing.add("trainingLevel");
        }
        if (metrics == null || metrics.getCurrentWeightKg() == null) {
            missing.add("currentWeightKg");
        }
        if (missing.isEmpty()) {
            return null;
        }
        return "Some profile or body-metric fields are still missing: " + String.join(", ", missing)
                + ". Add them before the next AI analysis for more specific recommendations.";
    }

    private String resolveTemplateName(List<TrainingSessionEntity> sessions, CycleTemplateEntity template) {
        for (TrainingSessionEntity session : sessions) {
            if (StringUtils.hasText(session.getTemplateNameSnapshot())) {
                return session.getTemplateNameSnapshot().trim();
            }
        }
        return template == null ? null : template.getName();
    }

    private Integer resolveCycleLength(
            CycleRunEntity cycleRun,
            CycleTemplateEntity template,
            VersionSnapshot versionSnapshot) {
        if (cycleRun.getTemplateVersionId() != null && versionSnapshot != null && !versionSnapshot.days().isEmpty()) {
            int maxDayIndex = 0;
            for (DaySnapshot daySnapshot : versionSnapshot.days()) {
                if (daySnapshot.dayIndex() != null) {
                    maxDayIndex = Math.max(maxDayIndex, daySnapshot.dayIndex());
                }
            }
            if (maxDayIndex > 0) {
                return maxDayIndex;
            }
            return versionSnapshot.days().size();
        }
        return template == null ? null : template.getCycleLength();
    }

    private AiTaskRecordEntity requireTask(Long taskId, String taskType) {
        AiTaskRecordEntity task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null || !taskType.equals(task.getTaskType())) {
            throw new BusinessException(ErrorCode.AI_TASK_NOT_FOUND);
        }
        if (!"running".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "ai task is not running");
        }
        return task;
    }

    private String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "failed to serialize ai payload");
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "failed to parse ai task request payload");
        }
    }
}
