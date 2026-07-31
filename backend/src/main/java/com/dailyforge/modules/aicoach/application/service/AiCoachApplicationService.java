package com.dailyforge.modules.aicoach.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.application.assembler.AiCoachAssembler;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiCoachProperties;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiTaskExecutor;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskRecordMapper;
import com.dailyforge.modules.aicoach.interfaces.dto.CycleSummaryRequest;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import com.dailyforge.modules.aicoach.interfaces.vo.AiAsyncTaskAcceptedResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.AiCoachCapabilitiesResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.AiTaskDetailResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.CycleSummaryTaskResultResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationTaskResultResponse;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserMapper;
import com.dailyforge.modules.plan.application.service.PlanUserSupportService;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleRunMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserCurrentBodyMetricsMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class AiCoachApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AiCoachApplicationService.class);
    private static final String TASK_TEMPLATE_GENERATION = "template_generation";
    private static final String TASK_CYCLE_SUMMARY = "cycle_summary";
    private static final String PLATFORM_ROLE_ADMIN = "admin";
    private static final Set<String> SUPPORTED_SCENE_TYPES = Set.of("gym", "home");
    private static final Set<String> SUPPORTED_GOAL_TYPES = Set.of("fat_loss", "muscle_gain", "health_maintenance");
    private static final Set<String> AI_ENABLED_TIERS = Set.of("invited_ai", "premium");

    private final PlanUserSupportService planUserSupportService;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper;
    private final CycleRunMapper cycleRunMapper;
    private final AiTaskRecordMapper aiTaskRecordMapper;
    private final AiCoachAssembler aiCoachAssembler;
    private final AiCoachProperties aiCoachProperties;
    private final AiTaskExecutor aiTaskExecutor;
    private final Executor aiCoachTaskExecutor;
    private final ObjectMapper objectMapper;

    public AiCoachApplicationService(
            PlanUserSupportService planUserSupportService,
            UserMapper userMapper,
            UserProfileMapper userProfileMapper,
            UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper,
            CycleRunMapper cycleRunMapper,
            AiTaskRecordMapper aiTaskRecordMapper,
            AiCoachAssembler aiCoachAssembler,
            AiCoachProperties aiCoachProperties,
            AiTaskExecutor aiTaskExecutor,
            @Qualifier("aiCoachTaskExecutor") Executor aiCoachTaskExecutor,
            ObjectMapper objectMapper) {
        this.planUserSupportService = planUserSupportService;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.userCurrentBodyMetricsMapper = userCurrentBodyMetricsMapper;
        this.cycleRunMapper = cycleRunMapper;
        this.aiTaskRecordMapper = aiTaskRecordMapper;
        this.aiCoachAssembler = aiCoachAssembler;
        this.aiCoachProperties = aiCoachProperties;
        this.aiTaskExecutor = aiTaskExecutor;
        this.aiCoachTaskExecutor = aiCoachTaskExecutor;
        this.objectMapper = objectMapper;
    }

    public AiCoachCapabilitiesResponse getCapabilities() {
        Long userId = planUserSupportService.requireActiveUserId();
        UserEntity user = requireUser(userId);
        UserProfileEntity profile = userProfileMapper.selectById(userId);
        UserCurrentBodyMetricsEntity metrics = userCurrentBodyMetricsMapper.selectById(userId);
        List<String> missingRequiredFields = collectTemplateGenerationMissingFields(profile, metrics);
        List<String> recommendedMissingFields = collectRecommendedFields(profile, metrics);
        CycleRunEntity latestCompletedRun = selectLatestCompletedRun(userId);
        boolean aiEnabled = isAiEnabled(user);
        return new AiCoachCapabilitiesResponse(
                aiEnabled,
                user.getAccountTier(),
                user.getPlatformRole(),
                new AiCoachCapabilitiesResponse.TemplateGenerationCapability(
                        aiEnabled,
                        missingRequiredFields.isEmpty(),
                        missingRequiredFields,
                        List.copyOf(SUPPORTED_SCENE_TYPES),
                        List.copyOf(SUPPORTED_GOAL_TYPES),
                        1,
                        7),
                new AiCoachCapabilitiesResponse.CycleSummaryCapability(
                        aiEnabled,
                        latestCompletedRun != null,
                        latestCompletedRun == null ? null : latestCompletedRun.getId(),
                        latestCompletedRun == null ? null : latestCompletedRun.getCompletedAt(),
                        recommendedMissingFields));
    }

    @Transactional
    public AiAsyncTaskAcceptedResponse submitTemplateGeneration(TemplateGenerationRequest request) {
        Long userId = planUserSupportService.requireActiveUserId();
        UserEntity user = requireUser(userId);
        assertAiEnabled(user);
        validateSceneType(request.sceneType());
        validateGoalType(request.goalType());
        UserProfileEntity profile = userProfileMapper.selectById(userId);
        UserCurrentBodyMetricsEntity metrics = userCurrentBodyMetricsMapper.selectById(userId);
        assertTemplateGenerationReady(profile, metrics);
        AiTaskRecordEntity existing = findExistingTask(userId, TASK_TEMPLATE_GENERATION, request.clientRequestId());
        if (existing != null) {
            return aiCoachAssembler.toAcceptedResponse(existing);
        }

        AiTaskRecordEntity task = buildTaskRecord(
                userId,
                TASK_TEMPLATE_GENERATION,
                request.clientRequestId(),
                aiCoachProperties.getTemplateGenerationPromptVersion(),
                request,
                null,
                null);
        try {
            aiTaskRecordMapper.insert(task);
        } catch (DuplicateKeyException exception) {
            AiTaskRecordEntity duplicated = findExistingTask(userId, TASK_TEMPLATE_GENERATION, request.clientRequestId());
            if (duplicated != null) {
                return aiCoachAssembler.toAcceptedResponse(duplicated);
            }
            throw exception;
        }
        scheduleTaskAfterCommit(task.getId());
        return aiCoachAssembler.toAcceptedResponse(task);
    }

    public AiTaskDetailResponse<TemplateGenerationTaskResultResponse> getTemplateGeneration(Long taskId) {
        Long userId = planUserSupportService.requireActiveUserId();
        AiTaskRecordEntity task = requireTask(taskId, userId, TASK_TEMPLATE_GENERATION);
        TemplateGenerationTaskResultResponse result = null;
        if ("succeeded".equals(task.getStatus())) {
            result = deserializeResult(task.getResultJson(), TemplateGenerationTaskResultResponse.class);
        }
        return aiCoachAssembler.toTaskDetailResponse(task, result);
    }

    @Transactional
    public AiAsyncTaskAcceptedResponse submitCycleSummary(CycleSummaryRequest request) {
        Long userId = planUserSupportService.requireActiveUserId();
        UserEntity user = requireUser(userId);
        assertAiEnabled(user);
        CycleRunEntity run = requireCompletedCycleRun(userId, request.cycleRunId());
        AiTaskRecordEntity existing = findExistingTask(userId, TASK_CYCLE_SUMMARY, request.clientRequestId());
        if (existing != null) {
            return aiCoachAssembler.toAcceptedResponse(existing);
        }

        AiTaskRecordEntity task = buildTaskRecord(
                userId,
                TASK_CYCLE_SUMMARY,
                request.clientRequestId(),
                aiCoachProperties.getCycleSummaryPromptVersion(),
                request,
                "cycle_run",
                run.getId());
        try {
            aiTaskRecordMapper.insert(task);
        } catch (DuplicateKeyException exception) {
            AiTaskRecordEntity duplicated = findExistingTask(userId, TASK_CYCLE_SUMMARY, request.clientRequestId());
            if (duplicated != null) {
                return aiCoachAssembler.toAcceptedResponse(duplicated);
            }
            throw exception;
        }
        scheduleTaskAfterCommit(task.getId());
        return aiCoachAssembler.toAcceptedResponse(task);
    }

    public AiTaskDetailResponse<CycleSummaryTaskResultResponse> getCycleSummary(Long taskId) {
        Long userId = planUserSupportService.requireActiveUserId();
        AiTaskRecordEntity task = requireTask(taskId, userId, TASK_CYCLE_SUMMARY);
        CycleSummaryTaskResultResponse result = null;
        if ("succeeded".equals(task.getStatus())) {
            result = deserializeResult(task.getResultJson(), CycleSummaryTaskResultResponse.class);
        }
        return aiCoachAssembler.toTaskDetailResponse(task, result);
    }

    private void scheduleTaskAfterCommit(Long taskId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchTask(taskId);
                }
            });
            return;
        }
        dispatchTask(taskId);
    }

    private void dispatchTask(Long taskId) {
        try {
            aiCoachTaskExecutor.execute(() -> aiTaskExecutor.execute(taskId));
        } catch (RuntimeException exception) {
            markDispatchFailed(taskId, exception);
        }
    }

    private void markDispatchFailed(Long taskId, RuntimeException exception) {
        AiTaskRecordEntity task = aiTaskRecordMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus("failed");
        task.setErrorCode(ErrorCode.AI_SERVICE_UNAVAILABLE.getCode());
        task.setErrorMessage(ErrorCode.AI_SERVICE_UNAVAILABLE.getDefaultMessage());
        task.setCompletedAt(LocalDateTime.now());
        aiTaskRecordMapper.updateById(task);
        log.warn("AI task dispatch failed. taskId={}, message={}", taskId, exception.getMessage());
    }

    private AiTaskRecordEntity buildTaskRecord(
            Long userId,
            String taskType,
            String clientRequestId,
            String promptVersion,
            Object request,
            String relatedEntityType,
            Long relatedEntityId) {
        AiTaskRecordEntity task = new AiTaskRecordEntity();
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setClientRequestId(normalizeClientRequestId(clientRequestId));
        task.setRelatedEntityType(relatedEntityType);
        task.setRelatedEntityId(relatedEntityId);
        task.setProvider(aiCoachProperties.getProvider());
        task.setModel(aiCoachProperties.getModel());
        task.setPromptVersion(promptVersion);
        task.setRequestPayloadJson(writeJson(request));
        task.setStatus("pending");
        task.setToolCallCount(0);
        task.setRepairAttemptCount(0);
        return task;
    }

    private AiTaskRecordEntity findExistingTask(Long userId, String taskType, String clientRequestId) {
        String normalized = normalizeClientRequestId(clientRequestId);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return aiTaskRecordMapper.selectByUserTaskAndClientRequestId(userId, taskType, normalized);
    }

    private String normalizeClientRequestId(String clientRequestId) {
        return StringUtils.hasText(clientRequestId) ? clientRequestId.trim() : null;
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        return user;
    }

    private void assertAiEnabled(UserEntity user) {
        if (!isAiEnabled(user)) {
            throw new BusinessException(ErrorCode.AI_FEATURE_NOT_AVAILABLE);
        }
    }

    private boolean isAiEnabled(UserEntity user) {
        if (!aiCoachProperties.isEnabled() || user == null) {
            return false;
        }
        return PLATFORM_ROLE_ADMIN.equalsIgnoreCase(user.getPlatformRole())
                || AI_ENABLED_TIERS.contains(user.getAccountTier());
    }

    private void validateSceneType(String sceneType) {
        if (!SUPPORTED_SCENE_TYPES.contains(sceneType)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "unsupported sceneType");
        }
    }

    private void validateGoalType(String goalType) {
        if (!SUPPORTED_GOAL_TYPES.contains(goalType)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "unsupported goalType");
        }
    }

    private void assertTemplateGenerationReady(UserProfileEntity profile, UserCurrentBodyMetricsEntity metrics) {
        List<String> profileMissing = collectProfileMissingFields(profile);
        if (!profileMissing.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_REQUIRED_PROFILE_MISSING, String.join(",", profileMissing));
        }
        List<String> bodyMetricMissing = collectBodyMetricMissingFields(metrics);
        if (!bodyMetricMissing.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_REQUIRED_BODY_METRIC_MISSING, String.join(",", bodyMetricMissing));
        }
    }

    private List<String> collectTemplateGenerationMissingFields(
            UserProfileEntity profile,
            UserCurrentBodyMetricsEntity metrics) {
        java.util.ArrayList<String> fields = new java.util.ArrayList<>();
        fields.addAll(collectProfileMissingFields(profile));
        fields.addAll(collectBodyMetricMissingFields(metrics));
        return fields;
    }

    private List<String> collectRecommendedFields(UserProfileEntity profile, UserCurrentBodyMetricsEntity metrics) {
        return collectTemplateGenerationMissingFields(profile, metrics);
    }

    private List<String> collectProfileMissingFields(UserProfileEntity profile) {
        java.util.ArrayList<String> fields = new java.util.ArrayList<>();
        if (profile == null || !StringUtils.hasText(profile.getGender())) {
            fields.add("gender");
        }
        if (profile == null || profile.getBirthDate() == null) {
            fields.add("birthDate");
        }
        if (profile == null || profile.getHeightCm() == null) {
            fields.add("heightCm");
        }
        if (profile == null || !StringUtils.hasText(profile.getGoalType())) {
            fields.add("goalType");
        }
        if (profile == null || !StringUtils.hasText(profile.getTrainingLevel())) {
            fields.add("trainingLevel");
        }
        return fields;
    }

    private List<String> collectBodyMetricMissingFields(UserCurrentBodyMetricsEntity metrics) {
        if (metrics == null || metrics.getCurrentWeightKg() == null) {
            return List.of("currentWeightKg");
        }
        return List.of();
    }

    private CycleRunEntity selectLatestCompletedRun(Long userId) {
        return cycleRunMapper.selectOne(new LambdaQueryWrapper<CycleRunEntity>()
                .eq(CycleRunEntity::getUserId, userId)
                .eq(CycleRunEntity::getStatus, "completed")
                .orderByDesc(CycleRunEntity::getCompletedAt)
                .orderByDesc(CycleRunEntity::getId)
                .last("LIMIT 1"));
    }

    private CycleRunEntity requireCompletedCycleRun(Long userId, Long cycleRunId) {
        CycleRunEntity run = cycleRunMapper.selectOne(new LambdaQueryWrapper<CycleRunEntity>()
                .eq(CycleRunEntity::getId, cycleRunId)
                .eq(CycleRunEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (run == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!"completed".equals(run.getStatus())) {
            throw new BusinessException(ErrorCode.AI_CYCLE_RUN_NOT_COMPLETED);
        }
        return run;
    }

    private AiTaskRecordEntity requireTask(Long taskId, Long userId, String taskType) {
        AiTaskRecordEntity task = aiTaskRecordMapper.selectByIdAndUserIdAndTaskType(taskId, userId, taskType);
        if (task == null) {
            throw new BusinessException(ErrorCode.AI_TASK_NOT_FOUND);
        }
        return task;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "failed to serialize ai payload");
        }
    }

    private <T> T deserializeResult(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID);
        }
    }
}
