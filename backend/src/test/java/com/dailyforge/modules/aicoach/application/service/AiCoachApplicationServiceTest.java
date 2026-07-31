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
import com.dailyforge.modules.aicoach.infrastructure.ai.AiCoachProperties;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiTaskExecutor;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskRecordMapper;
import com.dailyforge.modules.aicoach.interfaces.dto.CycleSummaryRequest;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import com.dailyforge.modules.aicoach.interfaces.vo.AiAsyncTaskAcceptedResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.AiCoachCapabilitiesResponse;
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
    private UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper;
    @Mock
    private CycleRunMapper cycleRunMapper;
    @Mock
    private AiTaskRecordMapper aiTaskRecordMapper;
    @Mock
    private AiTaskExecutor aiTaskExecutor;
    @Mock
    private Executor aiCoachTaskExecutor;

    private AiCoachApplicationService service;
    private AiCoachProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiCoachProperties();
        properties.setEnabled(true);
        properties.setProvider("deepseek");
        properties.setModel("deepseek-chat");
        properties.setTemplateGenerationPromptVersion("template_generation_v1");
        properties.setCycleSummaryPromptVersion("cycle_summary_v1");

        service = new AiCoachApplicationService(
                planUserSupportService,
                userMapper,
                userProfileMapper,
                userCurrentBodyMetricsMapper,
                cycleRunMapper,
                aiTaskRecordMapper,
                new AiCoachAssembler(),
                properties,
                aiTaskExecutor,
                aiCoachTaskExecutor,
                new ObjectMapper());
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
                new TemplateGenerationRequest("req-1", "gym", "muscle_gain", 4, true);
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
                new TemplateGenerationRequest("req-2", "gym", "muscle_gain", 4, true);
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
                new TemplateGenerationRequest(" req-3 ", "gym", "muscle_gain", 4, true);
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
