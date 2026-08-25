package com.dailyforge.modules.aicoach.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dailyforge.modules.exercise.application.service.SystemExerciseLookupService;
import com.dailyforge.modules.exercise.infrastructure.persistence.mapper.ExerciseQueryMapper;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.VersionSnapshot;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleRunMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateMapper;
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
import com.dailyforge.modules.workout.application.service.TrainingPerformanceAggregationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiCoachToolSupportServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long CYCLE_RUN_ID = 7L;
    private static final Long TEMPLATE_ID = 11L;
    private static final Long TEMPLATE_VERSION_ID = 13L;
    private static final Long SESSION_ID = 101L;
    private static final Long EXERCISE_ID = 201L;
    private static final Long ITEM_ID = 301L;

    @Mock
    private UserProfileMapper userProfileMapper;
    @Mock
    private UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper;
    @Mock
    private ExerciseQueryMapper exerciseQueryMapper;
    @Mock
    private SystemExerciseLookupService systemExerciseLookupService;
    @Mock
    private CycleRunMapper cycleRunMapper;
    @Mock
    private CycleTemplateMapper cycleTemplateMapper;
    @Mock
    private CycleTemplateVersionDomainService cycleTemplateVersionDomainService;
    @Mock
    private TrainingSessionMapper trainingSessionMapper;
    @Mock
    private TrainingSessionExerciseMapper trainingSessionExerciseMapper;
    @Mock
    private TrainingSessionExerciseItemMapper trainingSessionExerciseItemMapper;
    @Mock
    private TrainingSessionExerciseItemMetricMapper trainingSessionExerciseItemMetricMapper;
    @Mock
    private TrainingPerformanceAggregationService trainingPerformanceAggregationService;

    private AiCoachToolSupportService service;

    @BeforeEach
    void setUp() {
        service = new AiCoachToolSupportService(
                userProfileMapper,
                userCurrentBodyMetricsMapper,
                exerciseQueryMapper,
                systemExerciseLookupService,
                cycleRunMapper,
                cycleTemplateMapper,
                cycleTemplateVersionDomainService,
                trainingSessionMapper,
                trainingSessionExerciseMapper,
                trainingSessionExerciseItemMapper,
                trainingSessionExerciseItemMetricMapper,
                trainingPerformanceAggregationService);
    }

    @Test
    void getTemplateGenerationConstraintsShouldAllowDurationMinutes() {
        Map<String, Object> constraints = service.getTemplateGenerationConstraints();

        List<String> allowedMetricKeys = (List<String>) constraints.get("allowedMetricKeys");
        assertThat(allowedMetricKeys).contains("duration_minutes");
    }

    @Test
    void getTemplateGenerationConstraintsShouldNotAllowRpe() {
        Map<String, Object> constraints = service.getTemplateGenerationConstraints();

        List<String> allowedMetricKeys = (List<String>) constraints.get("allowedMetricKeys");
        assertThat(allowedMetricKeys).doesNotContain("rpe");
    }

    @Test
    void getTemplateGenerationConstraintsShouldExposeMetricKeysByStructureType() {
        Map<String, Object> constraints = service.getTemplateGenerationConstraints();

        Map<String, List<String>> byStructureType =
                (Map<String, List<String>>) constraints.get("allowedMetricKeysByStructureType");
        assertThat(byStructureType.get("set_based"))
                .containsExactly("weight_kg", "reps", "duration_seconds", "duration_minutes", "rest_seconds");
        assertThat(byStructureType.get("single_segment"))
                .containsExactly("duration_seconds", "duration_minutes", "distance_km", "speed_kmh",
                        "pace_seconds_per_km", "incline_percent", "intensity_level");
    }

    @Test
    void getCycleRunSessionsDetailShouldKeepNullActualMetricValue() {
        // Given
        when(cycleRunMapper.selectById(CYCLE_RUN_ID)).thenReturn(cycleRun());
        when(cycleTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template());
        when(cycleTemplateVersionDomainService.loadVersionSnapshot(TEMPLATE_VERSION_ID))
                .thenReturn(new VersionSnapshot(List.of()));
        when(trainingSessionMapper.selectByCycleRunIdAndUserId(CYCLE_RUN_ID, USER_ID))
                .thenReturn(List.of(session()));
        when(trainingSessionExerciseMapper.selectBySessionIds(List.of(SESSION_ID)))
                .thenReturn(List.of(exercise()));
        when(trainingSessionExerciseItemMapper.selectBySessionExerciseIds(List.of(EXERCISE_ID)))
                .thenReturn(List.of(item()));
        when(trainingSessionExerciseItemMetricMapper.selectBySessionExerciseItemIds(List.of(ITEM_ID)))
                .thenReturn(List.of(metricWithNullActual()));

        // When
        Map<String, Object> result = service.getCycleRunSessionsDetail(USER_ID, CYCLE_RUN_ID);

        // Then
        assertThat(result).containsEntry("sessionCount", 1);
        List<?> sessions = (List<?>) result.get("sessions");
        assertThat(sessions).hasSize(1);
        Map<?, ?> sessionValue = (Map<?, ?>) sessions.getFirst();
        List<?> exercises = (List<?>) sessionValue.get("exercises");
        Map<?, ?> exerciseValue = (Map<?, ?>) exercises.getFirst();
        List<?> items = (List<?>) exerciseValue.get("items");
        Map<?, ?> itemValue = (Map<?, ?>) items.getFirst();
        List<?> metrics = (List<?>) itemValue.get("metrics");
        Map<?, ?> metricValue = (Map<?, ?>) metrics.getFirst();
        assertThat(metricValue.get("metricKey")).isEqualTo("weight_kg");
        assertThat(metricValue.get("plannedValueNumber")).isEqualTo(new BigDecimal("60.0000"));
        assertThat(metricValue.get("actualValueNumber")).isNull();
    }

    private CycleRunEntity cycleRun() {
        CycleRunEntity entity = new CycleRunEntity();
        entity.setId(CYCLE_RUN_ID);
        entity.setUserId(USER_ID);
        entity.setTemplateId(TEMPLATE_ID);
        entity.setTemplateVersionId(TEMPLATE_VERSION_ID);
        entity.setRunNo(1);
        entity.setStatus("completed");
        return entity;
    }

    private CycleTemplateEntity template() {
        CycleTemplateEntity entity = new CycleTemplateEntity();
        entity.setId(TEMPLATE_ID);
        entity.setName("Push Pull");
        entity.setCycleLength(2);
        return entity;
    }

    private TrainingSessionEntity session() {
        TrainingSessionEntity entity = new TrainingSessionEntity();
        entity.setId(SESSION_ID);
        entity.setUserId(USER_ID);
        entity.setCycleRunId(CYCLE_RUN_ID);
        entity.setDayIndex(1);
        entity.setStatus("completed");
        entity.setSessionType("training");
        entity.setTemplateNameSnapshot("Push Pull");
        entity.setDayNameSnapshot("Day 1");
        return entity;
    }

    private TrainingSessionExerciseEntity exercise() {
        TrainingSessionExerciseEntity entity = new TrainingSessionExerciseEntity();
        entity.setId(EXERCISE_ID);
        entity.setSessionId(SESSION_ID);
        entity.setExerciseId(501L);
        entity.setExerciseNameSnapshot("Bench Press");
        entity.setStructureType("set_based");
        entity.setExerciseStatus("completed");
        entity.setSortOrder(1);
        return entity;
    }

    private TrainingSessionExerciseItemEntity item() {
        TrainingSessionExerciseItemEntity entity = new TrainingSessionExerciseItemEntity();
        entity.setId(ITEM_ID);
        entity.setSessionExerciseId(EXERCISE_ID);
        entity.setItemIndex(1);
        entity.setItemType("set");
        entity.setItemNameSnapshot("Set 1");
        return entity;
    }

    private TrainingSessionExerciseItemMetricEntity metricWithNullActual() {
        TrainingSessionExerciseItemMetricEntity entity = new TrainingSessionExerciseItemMetricEntity();
        entity.setId(401L);
        entity.setSessionExerciseItemId(ITEM_ID);
        entity.setMetricKey("weight_kg");
        entity.setPlannedValueNumber(new BigDecimal("60.0000"));
        entity.setActualValueNumber(null);
        entity.setSortOrder(1);
        return entity;
    }
}
