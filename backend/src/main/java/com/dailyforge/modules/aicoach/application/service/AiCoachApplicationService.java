package com.dailyforge.modules.aicoach.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.application.assembler.AiCoachAssembler;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiCoachProperties;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiTaskExecutor;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskToolCallEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskRecordMapper;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskToolCallMapper;
import com.dailyforge.modules.aicoach.interfaces.dto.AiTaskHistoryQuery;
import com.dailyforge.modules.aicoach.interfaces.dto.CycleSummaryRequest;
import com.dailyforge.modules.aicoach.interfaces.dto.NextCycleGenerationRequest;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import com.dailyforge.modules.aicoach.interfaces.vo.AiAsyncTaskAcceptedResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.AiCoachCapabilitiesResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.AiTaskDetailResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.CycleSummaryHistoryItemResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.CycleSummaryHistoryPageResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.CycleSummaryTaskResultResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationHistoryItemResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationHistoryPageResponse;
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
    private static final String TASK_NEXT_CYCLE_GENERATION = "next_cycle_generation";
    private static final String RELATED_ENTITY_CYCLE_RUN = "cycle_run";
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
    private final AiTaskToolCallMapper aiTaskToolCallMapper;
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
            AiTaskToolCallMapper aiTaskToolCallMapper,
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
        this.aiTaskToolCallMapper = aiTaskToolCallMapper;
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
                        recommendedMissingFields),
                buildNextCycleGenerationCapability(userId, aiEnabled, latestCompletedRun));
    }

    private AiCoachCapabilitiesResponse.NextCycleGenerationCapability buildNextCycleGenerationCapability(
            Long userId,
            boolean aiEnabled,
            CycleRunEntity latestCompletedRun) {
        if (!aiEnabled) {
            return new AiCoachCapabilitiesResponse.NextCycleGenerationCapability(
                    false, false, null, null, "ai_not_available");
        }
        if (latestCompletedRun == null) {
            return new AiCoachCapabilitiesResponse.NextCycleGenerationCapability(
                    true, false, null, null, "no_completed_cycle");
        }
        AiTaskRecordEntity summary = aiTaskRecordMapper.selectLatestSucceededByUserIdAndTaskTypeAndRelatedEntity(
                userId,
                TASK_CYCLE_SUMMARY,
                RELATED_ENTITY_CYCLE_RUN,
                latestCompletedRun.getId());
        boolean ready = summary != null;
        return new AiCoachCapabilitiesResponse.NextCycleGenerationCapability(
                true,
                ready,
                latestCompletedRun.getId(),
                latestCompletedRun.getCompletedAt(),
                ready ? null : "no_cycle_summary");
    }

    @Transactional
    public AiAsyncTaskAcceptedResponse submitTemplateGeneration(TemplateGenerationRequest request) {
        TemplateGenerationRequest normalizedRequest = normalizeTemplateGenerationRequest(request);
        Long userId = planUserSupportService.requireActiveUserId();
        UserEntity user = requireUser(userId);
        assertAiEnabled(user);
        validateSceneType(normalizedRequest.sceneType());
        validateGoalType(normalizedRequest.goalType());
        UserProfileEntity profile = userProfileMapper.selectById(userId);
        UserCurrentBodyMetricsEntity metrics = userCurrentBodyMetricsMapper.selectById(userId);
        assertTemplateGenerationReady(profile, metrics);
        AiTaskRecordEntity existing = findExistingTask(userId, TASK_TEMPLATE_GENERATION, normalizedRequest.clientRequestId());
        if (existing != null) {
            return aiCoachAssembler.toAcceptedResponse(existing);
        }

        AiTaskRecordEntity task = buildTaskRecord(
                userId,
                TASK_TEMPLATE_GENERATION,
                normalizedRequest.clientRequestId(),
                aiCoachProperties.getTemplateGenerationPromptVersion(),
                normalizedRequest,
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
        AiTaskToolCallEntity latestToolCall = loadLatestToolCall(task.getId());
        TemplateGenerationRequest requestSnapshot =
                safeDeserialize(task.getRequestPayloadJson(), TemplateGenerationRequest.class);
        TemplateGenerationTaskResultResponse result = null;
        if ("succeeded".equals(task.getStatus())) {
            result = deserializeResult(task.getResultJson(), TemplateGenerationTaskResultResponse.class);
        }
        return aiCoachAssembler.toTaskDetailResponse(task, latestToolCall, requestSnapshot, result);
    }

    @Transactional
    public AiAsyncTaskAcceptedResponse submitCycleSummary(CycleSummaryRequest request) {
        CycleSummaryRequest normalizedRequest = normalizeCycleSummaryRequest(request);
        Long userId = planUserSupportService.requireActiveUserId();
        UserEntity user = requireUser(userId);
        assertAiEnabled(user);
        CycleRunEntity run = requireCompletedCycleRun(userId, normalizedRequest.cycleRunId());
        AiTaskRecordEntity existing = findExistingTask(userId, TASK_CYCLE_SUMMARY, normalizedRequest.clientRequestId());
        if (existing != null) {
            return aiCoachAssembler.toAcceptedResponse(existing);
        }

        AiTaskRecordEntity task = buildTaskRecord(
                userId,
                TASK_CYCLE_SUMMARY,
                normalizedRequest.clientRequestId(),
                aiCoachProperties.getCycleSummaryPromptVersion(),
                normalizedRequest,
                RELATED_ENTITY_CYCLE_RUN,
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
        AiTaskToolCallEntity latestToolCall = loadLatestToolCall(task.getId());
        CycleSummaryTaskResultResponse result = null;
        if ("succeeded".equals(task.getStatus())) {
            result = deserializeResult(task.getResultJson(), CycleSummaryTaskResultResponse.class);
        }
        return aiCoachAssembler.toTaskDetailResponse(task, latestToolCall, null, result);
    }

    @Transactional
    public AiAsyncTaskAcceptedResponse submitNextCycleGeneration(NextCycleGenerationRequest request) {
        NextCycleGenerationRequest normalizedRequest = normalizeNextCycleGenerationRequest(request);
        Long userId = planUserSupportService.requireActiveUserId();
        UserEntity user = requireUser(userId);
        assertAiEnabled(user);
        validateSceneType(normalizedRequest.sceneType());
        validateGoalType(normalizedRequest.goalType());
        UserProfileEntity profile = userProfileMapper.selectById(userId);
        UserCurrentBodyMetricsEntity metrics = userCurrentBodyMetricsMapper.selectById(userId);
        assertTemplateGenerationReady(profile, metrics);
        AiTaskRecordEntity existing = findExistingTask(userId, TASK_NEXT_CYCLE_GENERATION, normalizedRequest.clientRequestId());
        if (existing != null) {
            return aiCoachAssembler.toAcceptedResponse(existing);
        }
        requireCompletedCycleRun(userId, normalizedRequest.sourceCycleRunId());
        assertCycleSummaryAvailable(userId, normalizedRequest.sourceCycleRunId(), normalizedRequest.sourceSummaryTaskId());

        AiTaskRecordEntity task = buildTaskRecord(
                userId,
                TASK_NEXT_CYCLE_GENERATION,
                normalizedRequest.clientRequestId(),
                aiCoachProperties.getNextCycleGenerationPromptVersion(),
                normalizedRequest,
                RELATED_ENTITY_CYCLE_RUN,
                normalizedRequest.sourceCycleRunId());
        try {
            aiTaskRecordMapper.insert(task);
        } catch (DuplicateKeyException exception) {
            AiTaskRecordEntity duplicated = findExistingTask(userId, TASK_NEXT_CYCLE_GENERATION, request.clientRequestId());
            if (duplicated != null) {
                return aiCoachAssembler.toAcceptedResponse(duplicated);
            }
            throw exception;
        }
        scheduleTaskAfterCommit(task.getId());
        return aiCoachAssembler.toAcceptedResponse(task);
    }

    public AiTaskDetailResponse<TemplateGenerationTaskResultResponse> getNextCycleGeneration(Long taskId) {
        Long userId = planUserSupportService.requireActiveUserId();
        AiTaskRecordEntity task = requireTask(taskId, userId, TASK_NEXT_CYCLE_GENERATION);
        AiTaskToolCallEntity latestToolCall = loadLatestToolCall(task.getId());
        NextCycleGenerationRequest requestSnapshot =
                safeDeserialize(task.getRequestPayloadJson(), NextCycleGenerationRequest.class);
        TemplateGenerationTaskResultResponse result = null;
        if ("succeeded".equals(task.getStatus())) {
            result = deserializeResult(task.getResultJson(), TemplateGenerationTaskResultResponse.class);
        }
        return aiCoachAssembler.toNextCycleTaskDetailResponse(task, latestToolCall, requestSnapshot, result);
    }

    public TemplateGenerationHistoryPageResponse getTemplateGenerationHistory(AiTaskHistoryQuery query) {
        Long userId = planUserSupportService.requireActiveUserId();
        long total = aiTaskRecordMapper.countByUserIdAndTaskType(userId, TASK_TEMPLATE_GENERATION);
        List<AiTaskRecordEntity> records = aiTaskRecordMapper.selectHistoryPageByUserIdAndTaskType(
                userId,
                TASK_TEMPLATE_GENERATION,
                offset(query),
                query.getPageSize());
        List<TemplateGenerationHistoryItemResponse> items = records.stream()
                .map(this::toTemplateGenerationHistoryItem)
                .toList();
        return new TemplateGenerationHistoryPageResponse(query.getPage(), query.getPageSize(), total, items);
    }

    public CycleSummaryHistoryPageResponse getCycleSummaryHistory(AiTaskHistoryQuery query) {
        Long userId = planUserSupportService.requireActiveUserId();
        long total = aiTaskRecordMapper.countByUserIdAndTaskType(userId, TASK_CYCLE_SUMMARY);
        List<AiTaskRecordEntity> records = aiTaskRecordMapper.selectHistoryPageByUserIdAndTaskType(
                userId,
                TASK_CYCLE_SUMMARY,
                offset(query),
                query.getPageSize());
        List<CycleSummaryHistoryItemResponse> items = records.stream()
                .map(this::toCycleSummaryHistoryItem)
                .toList();
        return new CycleSummaryHistoryPageResponse(query.getPage(), query.getPageSize(), total, items);
    }

    public AiTaskDetailResponse<CycleSummaryTaskResultResponse> getLatestCycleSummaryByCycleRun(Long cycleRunId) {
        Long userId = planUserSupportService.requireActiveUserId();
        requireCompletedCycleRun(userId, cycleRunId);
        AiTaskRecordEntity task = aiTaskRecordMapper.selectLatestSucceededByUserIdAndTaskTypeAndRelatedEntity(
                userId,
                TASK_CYCLE_SUMMARY,
                RELATED_ENTITY_CYCLE_RUN,
                cycleRunId);
        if (task == null) {
            throw new BusinessException(ErrorCode.AI_TASK_NOT_FOUND);
        }
        AiTaskToolCallEntity latestToolCall = loadLatestToolCall(task.getId());
        CycleSummaryTaskResultResponse result = null;
        if ("succeeded".equals(task.getStatus())) {
            result = deserializeResult(task.getResultJson(), CycleSummaryTaskResultResponse.class);
        }
        return aiCoachAssembler.toTaskDetailResponse(task, latestToolCall, null, result);
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

    private TemplateGenerationRequest normalizeTemplateGenerationRequest(TemplateGenerationRequest request) {
        return new TemplateGenerationRequest(
                normalizeClientRequestId(request.clientRequestId()),
                trimToNull(request.sceneType()),
                trimToNull(request.goalType()),
                request.cycleLength(),
                request.includeCardio(),
                trimToNull(request.additionalRequirements()));
    }

    private CycleSummaryRequest normalizeCycleSummaryRequest(CycleSummaryRequest request) {
        return new CycleSummaryRequest(
                normalizeClientRequestId(request.clientRequestId()),
                request.cycleRunId());
    }

    private NextCycleGenerationRequest normalizeNextCycleGenerationRequest(NextCycleGenerationRequest request) {
        return new NextCycleGenerationRequest(
                normalizeClientRequestId(request.clientRequestId()),
                request.sourceCycleRunId(),
                request.sourceSummaryTaskId(),
                trimToNull(request.sceneType()),
                trimToNull(request.goalType()),
                request.cycleLength(),
                request.includeCardio(),
                trimToNull(request.additionalRequirements()));
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
        if (!aiCoachProperties.isEnabled()
                || user == null
                || !StringUtils.hasText(aiCoachProperties.getApiKey())
                || !StringUtils.hasText(aiCoachProperties.getBaseUrl())
                || !StringUtils.hasText(aiCoachProperties.getModel())) {
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

    private void assertCycleSummaryAvailable(Long userId, Long sourceCycleRunId, Long sourceSummaryTaskId) {
        AiTaskRecordEntity summary = null;
        if (sourceSummaryTaskId != null) {
            summary = aiTaskRecordMapper.selectByIdAndUserIdAndTaskType(sourceSummaryTaskId, userId, TASK_CYCLE_SUMMARY);
            if (summary != null && !sourceCycleRunId.equals(summary.getRelatedEntityId())) {
                summary = null;
            }
        } else {
            summary = aiTaskRecordMapper.selectLatestSucceededByUserIdAndTaskTypeAndRelatedEntity(
                    userId, TASK_CYCLE_SUMMARY, RELATED_ENTITY_CYCLE_RUN, sourceCycleRunId);
        }
        if (summary == null || !"succeeded".equals(summary.getStatus())) {
            throw new BusinessException(ErrorCode.AI_CYCLE_SUMMARY_REQUIRED);
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
        java.util.ArrayList<String> fields = new java.util.ArrayList<>();
        if (profile == null || !StringUtils.hasText(profile.getGoalType())) {
            fields.add("goalType");
        }
        if (profile == null || !StringUtils.hasText(profile.getTrainingLevel())) {
            fields.add("trainingLevel");
        }
        if (metrics == null || metrics.getCurrentWeightKg() == null) {
            fields.add("currentWeightKg");
        }
        return fields;
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

    private TemplateGenerationHistoryItemResponse toTemplateGenerationHistoryItem(AiTaskRecordEntity task) {
        AiTaskToolCallEntity latestToolCall = loadLatestToolCall(task.getId());
        TemplateGenerationRequest request = safeDeserialize(task.getRequestPayloadJson(), TemplateGenerationRequest.class);
        TemplateGenerationTaskResultResponse result =
                safeDeserialize(task.getResultJson(), TemplateGenerationTaskResultResponse.class);
        return aiCoachAssembler.toTemplateGenerationHistoryItem(
                task,
                latestToolCall,
                request,
                result,
                resolveTemplateGenerationSummary(task, result));
    }

    private CycleSummaryHistoryItemResponse toCycleSummaryHistoryItem(AiTaskRecordEntity task) {
        AiTaskToolCallEntity latestToolCall = loadLatestToolCall(task.getId());
        CycleSummaryRequest request = safeDeserialize(task.getRequestPayloadJson(), CycleSummaryRequest.class);
        CycleSummaryTaskResultResponse result =
                safeDeserialize(task.getResultJson(), CycleSummaryTaskResultResponse.class);
        Long cycleRunId = task.getRelatedEntityId() != null
                ? task.getRelatedEntityId()
                : request == null ? null : request.cycleRunId();
        return aiCoachAssembler.toCycleSummaryHistoryItem(
                task,
                latestToolCall,
                cycleRunId,
                result,
                resolveCycleSummarySummary(task, result));
    }

    private String resolveTemplateGenerationSummary(
            AiTaskRecordEntity task,
            TemplateGenerationTaskResultResponse result) {
        if (result != null
                && result.generationRationale() != null
                && StringUtils.hasText(result.generationRationale().overallDesignSummary())) {
            return result.generationRationale().overallDesignSummary().trim();
        }
        return trimToNull(task.getOutputPreview());
    }

    private String resolveCycleSummarySummary(
            AiTaskRecordEntity task,
            CycleSummaryTaskResultResponse result) {
        if (result != null && StringUtils.hasText(result.executionOverview())) {
            return result.executionOverview().trim();
        }
        return trimToNull(task.getOutputPreview());
    }

    private AiTaskToolCallEntity loadLatestToolCall(Long taskId) {
        return aiTaskToolCallMapper.selectLatestByTaskId(taskId);
    }

    private int offset(AiTaskHistoryQuery query) {
        return (query.getPage() - 1) * query.getPageSize();
    }

    private <T> T safeDeserialize(String json, Class<T> type) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            log.warn("Failed to deserialize ai history payload. type={}, message={}", type.getSimpleName(), exception.getMessage());
            return null;
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
