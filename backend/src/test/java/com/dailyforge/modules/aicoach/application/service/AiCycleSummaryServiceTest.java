package com.dailyforge.modules.aicoach.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemMetricEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMetricMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
    private TrainingSessionExerciseMapper exerciseMapper;
    @Mock
    private TrainingSessionExerciseItemMapper itemMapper;
    @Mock
    private TrainingSessionExerciseItemMetricMapper metricMapper;
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
                exerciseMapper,
                itemMapper,
                metricMapper,
                profileMapper,
                metricsMapper,
                objectMapper);
    }

    @Test
    void processTaskShouldPersistPainAwareSummaryAndHistoricalTemplateContext() throws Exception {
        // Given
        AiTaskRecordEntity task = runningTask();
        CycleRunEntity cycleRun = completedCycleRun();
        CycleTemplateEntity template = template();
        TrainingSessionEntity session = trainingSession();
        TrainingSessionExerciseEntity completedExercise = completedExercise();
        TrainingSessionExerciseEntity failedExercise = failedExercise();
        TrainingSessionExerciseItemEntity item = item();
        TrainingSessionExerciseItemMetricEntity matchedMetric = matchedMetric(item.getId());
        VersionSnapshot versionSnapshot = new VersionSnapshot(List.of(
                new DaySnapshot(1, "Push", List.of()),
                new DaySnapshot(2, "Pull", List.of()),
                new DaySnapshot(3, "Legs", List.of()),
                new DaySnapshot(4, "Recovery", List.of())));

        when(taskMapper.selectByIdForUpdate(TASK_ID)).thenReturn(task);
        when(cycleRunMapper.selectById(CYCLE_RUN_ID)).thenReturn(cycleRun);
        when(templateMapper.selectById(cycleRun.getTemplateId())).thenReturn(template);
        when(versionService.loadVersionSnapshot(TEMPLATE_VERSION_ID)).thenReturn(versionSnapshot);
        when(profileMapper.selectById(USER_ID)).thenReturn(profileMissingGoal());
        when(metricsMapper.selectById(USER_ID)).thenReturn(null);
        when(sessionMapper.selectByCycleRunIdAndUserId(CYCLE_RUN_ID, USER_ID)).thenReturn(List.of(session));
        when(exerciseMapper.selectBySessionIds(List.of(session.getId())))
                .thenReturn(List.of(completedExercise, failedExercise));
        when(itemMapper.selectBySessionExerciseIds(List.of(completedExercise.getId(), failedExercise.getId())))
                .thenReturn(List.of(item));
        when(metricMapper.selectBySessionExerciseItemIds(List.of(item.getId())))
                .thenReturn(List.of(matchedMetric));

        // When
        service.processTask(TASK_ID);

        // Then
        ArgumentCaptor<AiTaskRecordEntity> taskCaptor = ArgumentCaptor.forClass(AiTaskRecordEntity.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        AiTaskRecordEntity updatedTask = taskCaptor.getValue();
        assertThat(updatedTask.getStatus()).isEqualTo("succeeded");
        assertThat(updatedTask.getToolCallCount()).isZero();
        assertThat(updatedTask.getRepairAttemptCount()).isZero();
        assertThat(updatedTask.getOutputPreview()).contains("Run #3 finished");
        assertThat(updatedTask.getInputSummaryJson()).contains("\"cycleRunId\":900");
        assertThat(updatedTask.getInputSummaryJson()).contains("Historical Four Day Split");
        assertThat(updatedTask.getInputSummaryJson()).contains("\"cycleLength\":4");

        CycleSummaryTaskResultResponse result = objectMapper.readValue(
                updatedTask.getResultJson(),
                CycleSummaryTaskResultResponse.class);
        assertThat(result.cycleRunId()).isEqualTo(CYCLE_RUN_ID);
        assertThat(result.templateName()).isEqualTo("Historical Four Day Split");
        assertThat(result.cycleLength()).isEqualTo(4);
        assertThat(result.executionOverview()).contains("1 logged days");
        assertThat(result.executionOverview()).contains("1 exercises that deviated");
        assertThat(result.issues())
                .anySatisfy(value -> assertThat(value).contains("pain or discomfort"))
                .anySatisfy(value -> assertThat(value).contains("1 exercises marked as partial, failed, or skipped"));
        assertThat(result.strengths())
                .anySatisfy(value -> assertThat(value).contains("logged training days"))
                .anySatisfy(value -> assertThat(value).contains("matched the plan"));
        assertThat(result.nextCycleSuggestions())
                .anySatisfy(value -> assertThat(value).contains("small load increase"))
                .anySatisfy(value -> assertThat(value).contains("Replace pain-triggering movements"));
        assertThat(result.risks())
                .anySatisfy(value -> assertThat(value).contains("Do not add load"));
        assertThat(result.dataCompletenessNotice()).contains("missing");
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
        session.setOverallFeeling("Left knee pain after the final set");
        session.setNotes("Session still moved forward.");
        return session;
    }

    private TrainingSessionExerciseEntity completedExercise() {
        TrainingSessionExerciseEntity exercise = new TrainingSessionExerciseEntity();
        exercise.setId(601L);
        exercise.setExerciseStatus("completed");
        exercise.setFeeling("stable");
        return exercise;
    }

    private TrainingSessionExerciseEntity failedExercise() {
        TrainingSessionExerciseEntity exercise = new TrainingSessionExerciseEntity();
        exercise.setId(602L);
        exercise.setExerciseStatus("failed");
        exercise.setFeeling("pain on squat");
        exercise.setFailureReason("knee pain");
        exercise.setAdjustmentNote("hurt when depth increased");
        return exercise;
    }

    private TrainingSessionExerciseItemEntity item() {
        TrainingSessionExerciseItemEntity item = new TrainingSessionExerciseItemEntity();
        item.setId(701L);
        item.setSessionExerciseId(602L);
        return item;
    }

    private TrainingSessionExerciseItemMetricEntity matchedMetric(Long itemId) {
        TrainingSessionExerciseItemMetricEntity metric = new TrainingSessionExerciseItemMetricEntity();
        metric.setId(801L);
        metric.setSessionExerciseItemId(itemId);
        metric.setPlannedValueNumber(new BigDecimal("100.00"));
        metric.setActualValueNumber(new BigDecimal("100.00"));
        return metric;
    }
}
