package com.dailyforge.modules.aicoach.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.application.assembler.AiCoachAssembler;
import com.dailyforge.modules.auth.application.service.AccountTierExpiryService;
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
import com.dailyforge.modules.aicoach.interfaces.vo.CycleSummaryHistoryPageResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.CycleSummaryTaskResultResponse;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiCoachApplicationServiceTest {

    private static final Long USER_ID = 101L;

    @Mock
    private PlanUserSupportService planUserSupportService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private AccountTierExpiryService accountTierExpiryService;
    @Mock
    private UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper;
    @Mock
    private CycleRunMapper cycleRunMapper;
    @Mock
    private AiTaskRecordMapper aiTaskRecordMapper;
    @Mock
    private AiTaskToolCallMapper aiTaskToolCallMapper;
    @Mock
    private AiTaskExecutor aiTaskExecutor;
    @Mock
    private Executor aiCoachTaskExecutor;

    private AiCoachApplicationService service;
    private AiCoachProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new AiCoachProperties();
        properties.setEnabled(true);
        properties.setProvider("deepseek");
        properties.setModel("deepseek-chat");
        properties.setApiKey("test-api-key");
        properties.setTemplateGenerationPromptVersion("template_generation_v1");
        properties.setCycleSummaryPromptVersion("cycle_summary_v1");
        objectMapper = new ObjectMapper();

        service = new AiCoachApplicationService(
                planUserSupportService,
                userMapper,
                accountTierExpiryService,
                userProfileMapper,
                userCurrentBodyMetricsMapper,
                cycleRunMapper,
                aiTaskRecordMapper,
                aiTaskToolCallMapper,
                new AiCoachAssembler(),
                properties,
                aiTaskExecutor,
                aiCoachTaskExecutor,
                objectMapper);
    }

    @Test
    void aiCoachPropertiesShouldUseConfiguredDefaultMaxToolRounds() {
        assertThat(new AiCoachProperties().getMaxToolRounds()).isEqualTo(50);
    }

    @Test
    void getCapabilitiesShouldAllowAdminUserEvenWhenAccountTierIsNotAiEnabled() {
        // Given
        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(activeAdminUser("free"));
        when(userProfileMapper.selectById(USER_ID)).thenReturn(completeProfile());
        when(userCurrentBodyMetricsMapper.selectById(USER_ID)).thenReturn(bodyMetrics("76.50"));
        when(cycleRunMapper.selectOne(any())).thenReturn(completedCycleRun(900L));

        // When
        AiCoachCapabilitiesResponse response = service.getCapabilities();

        // Then
        assertThat(response.aiEnabled()).isTrue();
        assertThat(response.accountTier()).isEqualTo("free");
        assertThat(response.platformRole()).isEqualTo("admin");
        assertThat(response.templateGeneration().available()).isTrue();
        assertThat(response.templateGeneration().ready()).isTrue();
        assertThat(response.templateGeneration().missingRequiredFields()).isEmpty();
        assertThat(response.templateGeneration().allowedSceneTypes()).containsExactlyInAnyOrder("gym", "home");
        assertThat(response.templateGeneration().allowedGoalTypes())
                .containsExactlyInAnyOrder("fat_loss", "muscle_gain", "health_maintenance");
        assertThat(response.cycleSummary().available()).isTrue();
        assertThat(response.cycleSummary().ready()).isTrue();
        assertThat(response.cycleSummary().latestCompletedCycleRunId()).isEqualTo(900L);
        assertThat(response.cycleSummary().recommendedMissingFields()).isEmpty();
    }

    @Test
    void submitTemplateGenerationShouldRejectWhenAiFeatureIsUnavailable() {
        // Given
        TemplateGenerationRequest request =
                new TemplateGenerationRequest("req-1", "gym", "muscle_gain", 4, true, null);
        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(activeAiUser("free"));

        // When / Then
        assertThatThrownBy(() -> service.submitTemplateGeneration(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.AI_FEATURE_NOT_AVAILABLE));

        verify(aiTaskRecordMapper, never()).insert(any());
    }

    @Test
    void submitTemplateGenerationShouldRejectWhenRequiredProfileDataIsMissing() {
        // Given
        TemplateGenerationRequest request =
                new TemplateGenerationRequest("req-2", "gym", "muscle_gain", 4, true, null);
        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(activeAiUser("invited_ai"));
        when(userProfileMapper.selectById(USER_ID)).thenReturn(new UserProfileEntity());
        when(userCurrentBodyMetricsMapper.selectById(USER_ID)).thenReturn(bodyMetrics("76.50"));

        // When / Then
        assertThatThrownBy(() -> service.submitTemplateGeneration(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException businessException = (BusinessException) error;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.AI_REQUIRED_PROFILE_MISSING);
                    assertThat(businessException).hasMessageContaining("gender");
                    assertThat(businessException).hasMessageContaining("birthDate");
                    assertThat(businessException).hasMessageContaining("heightCm");
                    assertThat(businessException).hasMessageContaining("goalType");
                    assertThat(businessException).hasMessageContaining("trainingLevel");
                });

        verify(aiTaskRecordMapper, never()).insert(any());
    }

    @Test
    void submitTemplateGenerationShouldAllowAdminUserAndReuseExistingTaskWhenClientRequestIdMatches() {
        // Given
        TemplateGenerationRequest request =
                new TemplateGenerationRequest(" req-3 ", "gym", "muscle_gain", 4, true, null);
        AiTaskRecordEntity existingTask = new AiTaskRecordEntity();
        existingTask.setId(77L);
        existingTask.setTaskType("template_generation");
        existingTask.setStatus("pending");
        existingTask.setCreatedAt(LocalDateTime.of(2026, 7, 31, 10, 0));

        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(activeAdminUser("free"));
        when(userProfileMapper.selectById(USER_ID)).thenReturn(completeProfile());
        when(userCurrentBodyMetricsMapper.selectById(USER_ID)).thenReturn(bodyMetrics("76.50"));
        when(aiTaskRecordMapper.selectByUserTaskAndClientRequestId(USER_ID, "template_generation", "req-3"))
                .thenReturn(existingTask);

        // When
        AiAsyncTaskAcceptedResponse response = service.submitTemplateGeneration(request);

        // Then
        assertThat(response.taskId()).isEqualTo(77L);
        assertThat(response.taskType()).isEqualTo("template_generation");
        assertThat(response.taskStatus()).isEqualTo("pending");
        verify(aiTaskRecordMapper, never()).insert(any());
        verify(aiCoachTaskExecutor, never()).execute(any());
    }

    @Test
    void submitCycleSummaryShouldRejectWhenCycleRunIsNotCompleted() {
        // Given
        CycleSummaryRequest request = new CycleSummaryRequest("req-4", 501L);
        CycleRunEntity inProgressRun = new CycleRunEntity();
        inProgressRun.setId(501L);
        inProgressRun.setUserId(USER_ID);
        inProgressRun.setStatus("active");

        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(activeAiUser("premium"));
        when(cycleRunMapper.selectOne(any())).thenReturn(inProgressRun);

        // When / Then
        assertThatThrownBy(() -> service.submitCycleSummary(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.AI_CYCLE_RUN_NOT_COMPLETED));

        verify(aiTaskRecordMapper, never()).insert(any());
    }

    @Test
    void getTemplateGenerationShouldReturnProgressFieldsFromBackendSignals() {
        // Given
        AiTaskRecordEntity task = new AiTaskRecordEntity();
        task.setId(901L);
        task.setTaskType("template_generation");
        task.setStatus("running");
        task.setCreatedAt(LocalDateTime.of(2026, 8, 6, 9, 0));
        task.setStartedAt(LocalDateTime.of(2026, 8, 6, 9, 0, 1));
        task.setUpdatedAt(LocalDateTime.of(2026, 8, 6, 9, 0, 5));
        task.setRequestPayloadJson("""
                {"clientRequestId":"req-detail","sceneType":"gym","goalType":"muscle_gain","cycleLength":4,"includeCardio":true,
                "additionalRequirements":"每周至少保留 1 天完整休息。"}
                """);

        AiTaskToolCallEntity latestToolCall = new AiTaskToolCallEntity();
        latestToolCall.setTaskId(901L);
        latestToolCall.setRoundNo(2);
        latestToolCall.setToolName("search_candidate_exercises");
        latestToolCall.setStatus("succeeded");
        latestToolCall.setCreatedAt(LocalDateTime.of(2026, 8, 6, 9, 0, 4));

        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(aiTaskRecordMapper.selectByIdAndUserIdAndTaskType(901L, USER_ID, "template_generation"))
                .thenReturn(task);
        when(aiTaskToolCallMapper.selectLatestByTaskId(901L)).thenReturn(latestToolCall);

        // When
        AiTaskDetailResponse<TemplateGenerationTaskResultResponse> response = service.getTemplateGeneration(901L);

        // Then
        assertThat(response.taskStatus()).isEqualTo("running");
        assertThat(response.progressStage()).isEqualTo("calling_tool");
        assertThat(response.latestToolCall()).isNotNull();
        assertThat(response.latestToolCall().roundNo()).isEqualTo(2);
        assertThat(response.latestToolCall().toolName()).isEqualTo("search_candidate_exercises");
        assertThat(response.latestToolCall().toolDisplayName()).isEqualTo("搜索候选动作");
        assertThat(response.latestToolCall().status()).isEqualTo("succeeded");
        assertThat(response.requestSnapshot()).isNotNull();
        assertThat(response.requestSnapshot().sceneType()).isEqualTo("gym");
        assertThat(response.requestSnapshot().goalType()).isEqualTo("muscle_gain");
        assertThat(response.requestSnapshot().cycleLength()).isEqualTo(4);
        assertThat(response.requestSnapshot().includeCardio()).isTrue();
        assertThat(response.requestSnapshot().additionalRequirements()).isEqualTo("每周至少保留 1 天完整休息。");
        assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 6, 9, 0, 5));
        assertThat(response.result()).isNull();
    }

    @Test
    void getTemplateGenerationHistoryShouldExposeAdditionalRequirementsAndSummary() throws Exception {
        // Given
        AiTaskHistoryQuery query = new AiTaskHistoryQuery();
        query.setPage(1);
        query.setPageSize(20);
        AiTaskRecordEntity task = new AiTaskRecordEntity();
        task.setId(902L);
        task.setTaskType("template_generation");
        task.setStatus("succeeded");
        task.setCreatedAt(LocalDateTime.of(2026, 8, 6, 10, 0));
        task.setCompletedAt(LocalDateTime.of(2026, 8, 6, 10, 0, 12));
        task.setUpdatedAt(LocalDateTime.of(2026, 8, 6, 10, 0, 12));
        task.setRequestPayloadJson(objectMapper.writeValueAsString(new TemplateGenerationRequest(
                "req-history",
                "gym",
                "muscle_gain",
                4,
                true,
                "每周至少保留 1 天完整休息。")));
        task.setResultJson(objectMapper.writeValueAsString(templateGenerationResult()));

        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(aiTaskRecordMapper.countByUserIdAndTaskType(USER_ID, "template_generation")).thenReturn(1L);
        when(aiTaskRecordMapper.selectHistoryPageByUserIdAndTaskType(USER_ID, "template_generation", 0, 20))
                .thenReturn(List.of(task));
        when(aiTaskToolCallMapper.selectLatestByTaskId(902L)).thenReturn(null);

        // When
        TemplateGenerationHistoryPageResponse response = service.getTemplateGenerationHistory(query);

        // Then
        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.records()).hasSize(1);
        assertThat(response.records().getFirst().additionalRequirements()).isEqualTo("每周至少保留 1 天完整休息。");
        assertThat(response.records().getFirst().templateId()).isEqualTo(501L);
        assertThat(response.records().getFirst().templateName()).isEqualTo("AI 生成模板 2026-08-06 10:00");
        assertThat(response.records().getFirst().summaryText()).isEqualTo("采用 4 天循环，兼顾训练与恢复。");
        assertThat(response.records().getFirst().progressStage()).isEqualTo("completed");
    }

    @Test
    void getCycleSummaryHistoryShouldReturnExecutionOverviewSummary() throws Exception {
        // Given
        AiTaskHistoryQuery query = new AiTaskHistoryQuery();
        query.setPage(1);
        query.setPageSize(20);
        AiTaskRecordEntity task = new AiTaskRecordEntity();
        task.setId(903L);
        task.setTaskType("cycle_summary");
        task.setStatus("succeeded");
        task.setRelatedEntityId(7001L);
        task.setCreatedAt(LocalDateTime.of(2026, 8, 6, 11, 0));
        task.setCompletedAt(LocalDateTime.of(2026, 8, 6, 11, 0, 8));
        task.setUpdatedAt(LocalDateTime.of(2026, 8, 6, 11, 0, 8));
        task.setRequestPayloadJson(objectMapper.writeValueAsString(new CycleSummaryRequest("req-cycle-history", 7001L)));
        task.setResultJson(objectMapper.writeValueAsString(cycleSummaryResult(7001L)));

        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(aiTaskRecordMapper.countByUserIdAndTaskType(USER_ID, "cycle_summary")).thenReturn(1L);
        when(aiTaskRecordMapper.selectHistoryPageByUserIdAndTaskType(USER_ID, "cycle_summary", 0, 20))
                .thenReturn(List.of(task));
        when(aiTaskToolCallMapper.selectLatestByTaskId(903L)).thenReturn(null);

        // When
        CycleSummaryHistoryPageResponse response = service.getCycleSummaryHistory(query);

        // Then
        assertThat(response.records()).hasSize(1);
        assertThat(response.records().getFirst().cycleRunId()).isEqualTo(7001L);
        assertThat(response.records().getFirst().summaryText()).isEqualTo("本轮 4 个 Day 均完成打卡，其中 1 个动作出现部分完成。");
    }

    @Test
    void getLatestCycleSummaryByCycleRunShouldReuseLatestTaskDetailShape() throws Exception {
        // Given
        CycleRunEntity completedRun = completedCycleRun(7002L);
        AiTaskRecordEntity task = new AiTaskRecordEntity();
        task.setId(904L);
        task.setTaskType("cycle_summary");
        task.setStatus("succeeded");
        task.setRelatedEntityType("cycle_run");
        task.setRelatedEntityId(7002L);
        task.setCreatedAt(LocalDateTime.of(2026, 8, 6, 12, 0));
        task.setStartedAt(LocalDateTime.of(2026, 8, 6, 12, 0, 1));
        task.setCompletedAt(LocalDateTime.of(2026, 8, 6, 12, 0, 5));
        task.setUpdatedAt(LocalDateTime.of(2026, 8, 6, 12, 0, 5));
        task.setResultJson(objectMapper.writeValueAsString(cycleSummaryResult(7002L)));

        AiTaskToolCallEntity latestToolCall = new AiTaskToolCallEntity();
        latestToolCall.setTaskId(904L);
        latestToolCall.setRoundNo(1);
        latestToolCall.setToolName("get_cycle_run_aggregated_analysis");
        latestToolCall.setStatus("succeeded");
        latestToolCall.setCreatedAt(LocalDateTime.of(2026, 8, 6, 12, 0, 2));

        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(cycleRunMapper.selectOne(any())).thenReturn(completedRun);
        when(aiTaskRecordMapper.selectLatestSucceededByUserIdAndTaskTypeAndRelatedEntity(
                USER_ID,
                "cycle_summary",
                "cycle_run",
                7002L)).thenReturn(task);
        when(aiTaskToolCallMapper.selectLatestByTaskId(904L)).thenReturn(latestToolCall);

        // When
        AiTaskDetailResponse<CycleSummaryTaskResultResponse> response =
                service.getLatestCycleSummaryByCycleRun(7002L);

        // Then
        assertThat(response.taskId()).isEqualTo(904L);
        assertThat(response.progressStage()).isEqualTo("completed");
        assertThat(response.latestToolCall()).isNotNull();
        assertThat(response.latestToolCall().toolName()).isEqualTo("get_cycle_run_aggregated_analysis");
        assertThat(response.latestToolCall().toolDisplayName()).isEqualTo("获取周期执行聚合分析");
        assertThat(response.requestSnapshot()).isNull();
        assertThat(response.result()).isNotNull();
        assertThat(response.result().cycleRunId()).isEqualTo(7002L);
        assertThat(response.result().executionOverview()).isEqualTo("本轮 4 个 Day 均完成打卡，其中 1 个动作出现部分完成。");
    }

    @Test
    void submitNextCycleGenerationShouldRejectWhenNoSucceededCycleSummaryExists() {
        // Given
        NextCycleGenerationRequest request = new NextCycleGenerationRequest(
                "req-nc-1", 1201L, null, "gym", "muscle_gain", 4, true, null);
        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(activeAiUser("invited_ai"));
        when(userProfileMapper.selectById(USER_ID)).thenReturn(completeProfile());
        when(userCurrentBodyMetricsMapper.selectById(USER_ID)).thenReturn(bodyMetrics("76.50"));
        when(cycleRunMapper.selectOne(any())).thenReturn(completedCycleRun(1201L));
        when(aiTaskRecordMapper.selectLatestSucceededByUserIdAndTaskTypeAndRelatedEntity(
                USER_ID, "cycle_summary", "cycle_run", 1201L)).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> service.submitNextCycleGeneration(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.AI_CYCLE_SUMMARY_REQUIRED));

        verify(aiTaskRecordMapper, never()).insert(any());
    }

    @Test
    void submitNextCycleGenerationShouldCreateTaskWhenSummaryIsAvailable() {
        // Given
        NextCycleGenerationRequest request = new NextCycleGenerationRequest(
                "req-nc-2", 1201L, 88L, "gym", "muscle_gain", 4, true, null);
        AiTaskRecordEntity summaryTask = new AiTaskRecordEntity();
        summaryTask.setId(88L);
        summaryTask.setTaskType("cycle_summary");
        summaryTask.setStatus("succeeded");
        summaryTask.setRelatedEntityType("cycle_run");
        summaryTask.setRelatedEntityId(1201L);

        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(activeAiUser("invited_ai"));
        when(userProfileMapper.selectById(USER_ID)).thenReturn(completeProfile());
        when(userCurrentBodyMetricsMapper.selectById(USER_ID)).thenReturn(bodyMetrics("76.50"));
        when(cycleRunMapper.selectOne(any())).thenReturn(completedCycleRun(1201L));
        when(aiTaskRecordMapper.selectByIdAndUserIdAndTaskType(88L, USER_ID, "cycle_summary"))
                .thenReturn(summaryTask);
        when(aiTaskRecordMapper.selectByUserTaskAndClientRequestId(USER_ID, "next_cycle_generation", "req-nc-2"))
                .thenReturn(null);

        // When
        AiAsyncTaskAcceptedResponse response = service.submitNextCycleGeneration(request);

        // Then
        verify(aiTaskRecordMapper).insert(any());
        verify(aiCoachTaskExecutor).execute(any());
        assertThat(response.taskType()).isEqualTo("next_cycle_generation");
    }

    @Test
    void getCapabilitiesShouldExposeNextCycleGenerationCapability() {
        // Given
        when(planUserSupportService.requireActiveUserId()).thenReturn(USER_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(activeAiUser("invited_ai"));
        when(userProfileMapper.selectById(USER_ID)).thenReturn(completeProfile());
        when(userCurrentBodyMetricsMapper.selectById(USER_ID)).thenReturn(bodyMetrics("76.50"));
        when(cycleRunMapper.selectOne(any())).thenReturn(completedCycleRun(900L));
        when(aiTaskRecordMapper.selectLatestSucceededByUserIdAndTaskTypeAndRelatedEntity(
                USER_ID, "cycle_summary", "cycle_run", 900L)).thenReturn(null);

        // When
        AiCoachCapabilitiesResponse response = service.getCapabilities();

        // Then
        assertThat(response.nextCycleGeneration().available()).isTrue();
        assertThat(response.nextCycleGeneration().ready()).isFalse();
        assertThat(response.nextCycleGeneration().latestCompletedCycleRunId()).isEqualTo(900L);
        assertThat(response.nextCycleGeneration().missingReason()).isEqualTo("no_cycle_summary");
    }

    private TemplateGenerationTaskResultResponse templateGenerationResult() {
        return new TemplateGenerationTaskResultResponse(
                new TemplateGenerationTaskResultResponse.DraftTemplate(
                        501L,
                        "AI 生成模板 2026-08-06 10:00",
                        "draft",
                        4,
                        List.of()),
                new TemplateGenerationTaskResultResponse.GenerationRationale(
                        "采用 4 天循环，兼顾训练与恢复。",
                        List.of(),
                        List.of(),
                        new TemplateGenerationTaskResultResponse.IntensityRationale(
                                "starting_recommendation",
                                "当前重量为起始建议。"),
                        List.of()));
    }

    private CycleSummaryTaskResultResponse cycleSummaryResult(Long cycleRunId) {
        return new CycleSummaryTaskResultResponse(
                cycleRunId,
                301L,
                "四天上/下肢分化",
                3,
                4,
                "本轮 4 个 Day 均完成打卡，其中 1 个动作出现部分完成。",
                List.of("整体出勤稳定"),
                List.of("腿部训练后段疲劳明显"),
                List.of("下肢日总量偏高"),
                List.of("下肢日减少 1 个辅助动作"),
                List.of("如膝部不适持续，应优先调整腿部训练动作选择"),
                null);
    }

    private UserEntity activeAiUser(String accountTier) {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setStatus("active");
        user.setAccountTier(accountTier);
        user.setPlatformRole("user");
        return user;
    }

    private UserEntity activeAdminUser(String accountTier) {
        UserEntity user = activeAiUser(accountTier);
        user.setPlatformRole("admin");
        return user;
    }

    private UserProfileEntity completeProfile() {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(USER_ID);
        profile.setGender("male");
        profile.setBirthDate(LocalDate.of(1996, 3, 2));
        profile.setHeightCm(new BigDecimal("178.00"));
        profile.setGoalType("muscle_gain");
        profile.setTrainingLevel("experienced");
        return profile;
    }

    private UserCurrentBodyMetricsEntity bodyMetrics(String weight) {
        UserCurrentBodyMetricsEntity metrics = new UserCurrentBodyMetricsEntity();
        metrics.setUserId(USER_ID);
        metrics.setCurrentWeightKg(new BigDecimal(weight));
        return metrics;
    }

    private CycleRunEntity completedCycleRun(Long cycleRunId) {
        CycleRunEntity run = new CycleRunEntity();
        run.setId(cycleRunId);
        run.setUserId(USER_ID);
        run.setStatus("completed");
        run.setCompletedAt(LocalDateTime.of(2026, 7, 30, 21, 15));
        return run;
    }
}
