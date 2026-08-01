package com.dailyforge.modules.aicoach.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dailyforge.modules.aicoach.domain.model.CycleSummaryValidatedResult;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.CycleSummaryModelOutput;
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
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserCurrentBodyMetricsMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserProfileMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiCycleSummaryServiceTest {

    private static final Long USER_ID = 88L;
    private static final Long TASK_ID = 3001L;
    private static final Long CYCLE_RUN_ID = 900L;
    private static final Long TEMPLATE_VERSION_ID = 401L;

    @Mock
    private AiTaskRecordMapper taskMapper;
    @Mock
    private CycleRunMapper cycleRunMapper;
    @Mock
    private CycleTemplateMapper templateMapper;
    @Mock
    private CycleTemplateVersionDomainService versionService;
    @Mock
    private TrainingSessionMapper sessionMapper;
    @Mock
    private UserProfileMapper profileMapper;
    @Mock
    private UserCurrentBodyMetricsMapper metricsMapper;

    private ObjectMapper objectMapper;
    private AiCycleSummaryService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AiCycleSummaryService(
                taskMapper,
                cycleRunMapper,
                templateMapper,
                versionService,
                sessionMapper,
                profileMapper,
                metricsMapper,
                objectMapper);
    }

    @Test
    void persistSuccessfulResultShouldAssembleFinalResponseWithoutOverwritingExecutionCounters() throws Exception {
        // Given
        AiTaskRecordEntity task = runningTask();
        CycleRunEntity cycleRun = completedCycleRun();
        CycleTemplateEntity template = template();
        TrainingSessionEntity session = trainingSession();
        VersionSnapshot versionSnapshot = new VersionSnapshot(List.of(
                new DaySnapshot(1, "Push", List.of()),
                new DaySnapshot(2, "Pull", List.of()),
                new DaySnapshot(3, "Legs", List.of()),
                new DaySnapshot(4, "Recovery", List.of())));
        CycleSummaryValidatedResult validatedResult = new CycleSummaryValidatedResult(new CycleSummaryModelOutput(
                "Run #3 finished with stable logging and one lower-body issue.",
                List.of("Attendance stayed stable."),
                List.of("Knee discomfort appeared in the lower-body block."),
                List.of("The lower-body day may be too aggressive for current recovery."),
                List.of("Reduce one accessory movement next cycle."),
                List.of("Do not add load to pain-triggering movements yet."),
                null));

        when(taskMapper.selectByIdForUpdate(TASK_ID)).thenReturn(task);
        when(cycleRunMapper.selectById(CYCLE_RUN_ID)).thenReturn(cycleRun);
        when(templateMapper.selectById(cycleRun.getTemplateId())).thenReturn(template);
        when(versionService.loadVersionSnapshot(TEMPLATE_VERSION_ID)).thenReturn(versionSnapshot);
        when(sessionMapper.selectByCycleRunIdAndUserId(CYCLE_RUN_ID, USER_ID)).thenReturn(List.of(session));
        when(profileMapper.selectById(USER_ID)).thenReturn(profileMissingGoal());
        when(metricsMapper.selectById(USER_ID)).thenReturn(null);

        // When
        service.persistSuccessfulResult(TASK_ID, "{\"cycleRunId\":900}", validatedResult);

        // Then
        ArgumentCaptor<AiTaskRecordEntity> taskCaptor = ArgumentCaptor.forClass(AiTaskRecordEntity.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        AiTaskRecordEntity updatedTask = taskCaptor.getValue();
        assertThat(updatedTask.getStatus()).isEqualTo("succeeded");
        assertThat(updatedTask.getToolCallCount()).isEqualTo(2);
        assertThat(updatedTask.getRepairAttemptCount()).isEqualTo(1);
        assertThat(updatedTask.getInputSummaryJson()).contains("\"cycleRunId\":900");
        assertThat(updatedTask.getOutputPreview()).contains("Run #3 finished");

        CycleSummaryTaskResultResponse result = objectMapper.readValue(
                updatedTask.getResultJson(),
                CycleSummaryTaskResultResponse.class);
        assertThat(result.cycleRunId()).isEqualTo(CYCLE_RUN_ID);
        assertThat(result.templateName()).isEqualTo("Historical Four Day Split");
        assertThat(result.cycleLength()).isEqualTo(4);
        assertThat(result.executionOverview()).contains("Run #3 finished");
        assertThat(result.issues()).containsExactly("Knee discomfort appeared in the lower-body block.");
        assertThat(result.nextCycleSuggestions()).containsExactly("Reduce one accessory movement next cycle.");
        assertThat(result.dataCompletenessNotice()).contains("goalType");
        assertThat(result.dataCompletenessNotice()).contains("currentWeightKg");
    }

    private AiTaskRecordEntity runningTask() throws Exception {
        AiTaskRecordEntity task = new AiTaskRecordEntity();
        task.setId(TASK_ID);
        task.setUserId(USER_ID);
        task.setTaskType("cycle_summary");
        task.setStatus("running");
        task.setRelatedEntityId(CYCLE_RUN_ID);
        task.setRequestPayloadJson(objectMapper.writeValueAsString(new CycleSummaryRequest("req-5", CYCLE_RUN_ID)));
        task.setStartedAt(LocalDateTime.of(2026, 7, 31, 10, 15));
        task.setToolCallCount(2);
        task.setRepairAttemptCount(1);
        return task;
    }

    private CycleRunEntity completedCycleRun() {
        CycleRunEntity cycleRun = new CycleRunEntity();
        cycleRun.setId(CYCLE_RUN_ID);
        cycleRun.setUserId(USER_ID);
        cycleRun.setTemplateId(301L);
        cycleRun.setTemplateVersionId(TEMPLATE_VERSION_ID);
        cycleRun.setRunNo(3);
        cycleRun.setStatus("completed");
        return cycleRun;
    }

    private CycleTemplateEntity template() {
        CycleTemplateEntity template = new CycleTemplateEntity();
        template.setId(301L);
        template.setName("Current Template Name");
        template.setCycleLength(6);
        return template;
    }

    private UserProfileEntity profileMissingGoal() {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(USER_ID);
        profile.setTrainingLevel("beginner");
        return profile;
    }

    private TrainingSessionEntity trainingSession() {
        TrainingSessionEntity session = new TrainingSessionEntity();
        session.setId(501L);
        session.setUserId(USER_ID);
        session.setCycleRunId(CYCLE_RUN_ID);
        session.setTemplateNameSnapshot("Historical Four Day Split");
        return session;
    }
}
