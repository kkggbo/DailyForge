package com.dailyforge.modules.aicoach.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.exercise.application.service.SystemExerciseLookupService;
import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseEntity;
import com.dailyforge.modules.exercise.infrastructure.persistence.mapper.ExerciseQueryMapper;
import com.dailyforge.modules.exercise.interfaces.dto.ExerciseSystemListQuery;
import com.dailyforge.modules.plan.domain.model.MetricKey;
import com.dailyforge.modules.plan.domain.model.StructureType;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.DaySnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.ExerciseSnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.ItemSnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.MetricSnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.VersionSnapshot;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleRunMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserCurrentBodyMetricsMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserProfileMapper;
import com.dailyforge.modules.workout.application.model.PerformanceSummary;
import com.dailyforge.modules.workout.application.service.TrainingPerformanceAggregationService;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemMetricEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMetricMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionMapper;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiCoachToolSupportService {

    /** Number of most recent completed workout sessions aggregated into the AI context. */
    private static final int RECENT_WORKOUT_WINDOW = 5;

    private static final List<String> PAIN_KEYWORDS = List.of(
            "pain",
            "hurt",
            "sore",
            "injury",
            "discomfort",
            "ache",
            "sharp",
            "strain");

    private final UserProfileMapper userProfileMapper;
    private final UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper;
    private final ExerciseQueryMapper exerciseQueryMapper;
    private final SystemExerciseLookupService systemExerciseLookupService;
    private final CycleRunMapper cycleRunMapper;
    private final CycleTemplateMapper cycleTemplateMapper;
    private final CycleTemplateVersionDomainService cycleTemplateVersionDomainService;
    private final TrainingSessionMapper trainingSessionMapper;
    private final TrainingSessionExerciseMapper trainingSessionExerciseMapper;
    private final TrainingSessionExerciseItemMapper trainingSessionExerciseItemMapper;
    private final TrainingSessionExerciseItemMetricMapper trainingSessionExerciseItemMetricMapper;
    private final TrainingPerformanceAggregationService trainingPerformanceAggregationService;

    public AiCoachToolSupportService(
            UserProfileMapper userProfileMapper,
            UserCurrentBodyMetricsMapper userCurrentBodyMetricsMapper,
            ExerciseQueryMapper exerciseQueryMapper,
            SystemExerciseLookupService systemExerciseLookupService,
            CycleRunMapper cycleRunMapper,
            CycleTemplateMapper cycleTemplateMapper,
            CycleTemplateVersionDomainService cycleTemplateVersionDomainService,
            TrainingSessionMapper trainingSessionMapper,
            TrainingSessionExerciseMapper trainingSessionExerciseMapper,
            TrainingSessionExerciseItemMapper trainingSessionExerciseItemMapper,
            TrainingSessionExerciseItemMetricMapper trainingSessionExerciseItemMetricMapper,
            TrainingPerformanceAggregationService trainingPerformanceAggregationService) {
        this.userProfileMapper = userProfileMapper;
        this.userCurrentBodyMetricsMapper = userCurrentBodyMetricsMapper;
        this.exerciseQueryMapper = exerciseQueryMapper;
        this.systemExerciseLookupService = systemExerciseLookupService;
        this.cycleRunMapper = cycleRunMapper;
        this.cycleTemplateMapper = cycleTemplateMapper;
        this.cycleTemplateVersionDomainService = cycleTemplateVersionDomainService;
        this.trainingSessionMapper = trainingSessionMapper;
        this.trainingSessionExerciseMapper = trainingSessionExerciseMapper;
        this.trainingSessionExerciseItemMapper = trainingSessionExerciseItemMapper;
        this.trainingSessionExerciseItemMetricMapper = trainingSessionExerciseItemMetricMapper;
        this.trainingPerformanceAggregationService = trainingPerformanceAggregationService;
    }

    public Map<String, Object> getUserProfileContext(Long userId) {
        UserProfileEntity profile = userProfileMapper.selectById(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("gender", profile == null ? null : profile.getGender());
        result.put("birthDate", profile == null ? null : profile.getBirthDate());
        result.put("age", profile == null ? null : calculateAge(profile.getBirthDate()));
        result.put("heightCm", profile == null ? null : profile.getHeightCm());
        result.put("goalType", profile == null ? null : profile.getGoalType());
        result.put("trainingLevel", profile == null ? null : profile.getTrainingLevel());
        result.put("injuryNotes", profile == null ? null : profile.getInjuryNotes());
        return result;
    }

    public Map<String, Object> getUserCurrentBodyMetricsContext(Long userId) {
        UserCurrentBodyMetricsEntity metrics = userCurrentBodyMetricsMapper.selectById(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("currentWeightKg", metrics == null ? null : metrics.getCurrentWeightKg());
        result.put("currentBodyFatPercent", metrics == null ? null : metrics.getCurrentBodyFatPercent());
        result.put("currentBmi", metrics == null ? null : metrics.getCurrentBmi());
        result.put("currentSkeletalMusclePercent", metrics == null ? null : metrics.getCurrentSkeletalMusclePercent());
        result.put("currentBodyWaterPercent", metrics == null ? null : metrics.getCurrentBodyWaterPercent());
        result.put("currentBasalMetabolicRateKcal", metrics == null ? null : metrics.getCurrentBasalMetabolicRateKcal());
        result.put("currentWaistCm", metrics == null ? null : metrics.getCurrentWaistCm());
        result.put("currentHipCm", metrics == null ? null : metrics.getCurrentHipCm());
        result.put("currentWaistHipRatio", metrics == null ? null : metrics.getCurrentWaistHipRatio());
        result.put("currentBodyAge", metrics == null ? null : metrics.getCurrentBodyAge());
        result.put("currentBodyType", metrics == null ? null : metrics.getCurrentBodyType());
        result.put("updatedAt", metrics == null ? null : metrics.getUpdatedAt());
        return result;
    }

    /**
     * Compact view of the user's recent completed workout performance, used as AI prompt context.
     * The {@code available} flag is false when the user has no completed workout history; the model
     * should then fall back to a starting recommendation.
     */
    public Map<String, Object> getUserRecentWorkoutContext(Long userId) {
        PerformanceSummary summary =
                trainingPerformanceAggregationService.aggregateRecentCompletedWorkout(
                        userId, RECENT_WORKOUT_WINDOW);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", summary.sessionCount() > 0);
        result.put("window", RECENT_WORKOUT_WINDOW);
        result.put("sessionCount", summary.sessionCount());
        result.put("avgCompletionRate", round(summary.avgCompletionRate()));
        result.put("exercises", summary.exercises().stream()
                .map(this::formatExercisePerformance)
                .toList());
        return result;
    }

    private Map<String, Object> formatExercisePerformance(PerformanceSummary.ExercisePerformance e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exerciseId", e.exerciseId());
        result.put("name", e.name());
        result.put("structureType", e.structureType());
        result.put("timesPerformed", e.timesPerformed());
        result.put("setsDone", e.setsDone());
        result.put("setsPlanned", e.setsPlanned());
        result.put("completionRate", round(e.completionRate()));
        result.put("avgWeightKg", e.avgWeightKg());
        result.put("totalVolume", e.totalVolume());
        result.put("avgReps", e.avgReps());
        result.put("avgRpe", e.avgRpe());
        result.put("avgRestSeconds", e.avgRestSeconds());
        return result;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public Map<String, Object> getTemplateGenerationConstraints() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cycleLength", Map.of("min", 1, "max", 7));
        result.put("allowedStructureTypes", List.of("set_based", "single_segment"));
        result.put("allowedItemTypes", List.of("set", "segment"));
        result.put("allowedMetricKeys", Arrays.stream(MetricKey.values())
                .filter(key -> !key.isHidden())
                .map(MetricKey::getValue)
                .toList());
        result.put("allowedMetricKeysByStructureType", buildAllowedMetricKeysByStructureType());
        result.put("goalTypes", List.of("fat_loss", "muscle_gain", "health_maintenance"));
        result.put("sceneTypes", List.of("gym", "home"));
        return result;
    }

    private Map<String, List<String>> buildAllowedMetricKeysByStructureType() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (StructureType structureType : StructureType.values()) {
            result.put(structureType.getValue(), MetricKey.allowedFor(structureType).stream()
                    .map(MetricKey::getValue)
                    .toList());
        }
        return result;
    }

    public Map<String, Object> searchCandidateExercises(
            String sceneType,
            String keyword,
            String movementType,
            String structureType,
            Integer limit) {
        ExerciseSystemListQuery query = new ExerciseSystemListQuery();
        query.setSceneType(normalize(sceneType));
        query.setKeyword(normalize(keyword));
        query.setMovementType(normalize(movementType));
        query.setStructureType(normalize(structureType));
        query.setPage(1);
        query.setPageSize(normalizeLimit(limit));
        List<Long> ids = exerciseQueryMapper.selectSystemExercisePageIds(query);
        Map<Long, SystemExerciseLookupResult> lookups = systemExerciseLookupService.loadActiveSystemExercisesByIds(ids);
        List<Map<String, Object>> exercises = new ArrayList<>();
        for (Long id : ids) {
            SystemExerciseLookupResult lookup = lookups.get(id);
            if (lookup == null) {
                continue;
            }
            exercises.add(linkedMap(
                    "id", lookup.id(),
                    "name", lookup.name(),
                    "exerciseType", lookup.exerciseType(),
                    "movementType", lookup.movementType(),
                    "defaultUnit", lookup.defaultUnit(),
                    "defaultStructureType", lookup.defaultStructureType()));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sceneType", normalize(sceneType));
        result.put("keyword", normalize(keyword));
        result.put("movementType", normalize(movementType));
        result.put("structureType", normalize(structureType));
        result.put("count", exercises.size());
        result.put("exercises", exercises);
        return result;
    }

    public Map<String, Object> getExerciseDetail(Long exerciseId) {
        if (exerciseId == null || exerciseId < 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "exerciseId must be positive");
        }
        ExerciseEntity exercise = exerciseQueryMapper.selectSystemExerciseDetailById(exerciseId);
        if (exercise == null) {
            throw new BusinessException(ErrorCode.EXERCISE_NOT_FOUND);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", exercise.getId());
        result.put("name", exercise.getName());
        result.put("exerciseType", exercise.getExerciseType());
        result.put("movementType", exercise.getMovementType());
        result.put("defaultUnit", exercise.getDefaultUnit());
        result.put("defaultStructureType", exercise.getDefaultStructureType());
        result.put("videoUrl", exercise.getVideoUrl());
        result.put("calorieBurnReference", exercise.getCalorieBurnReference());
        result.put("calorieReferenceUnit", exercise.getCalorieReferenceUnit());
        return result;
    }

    public Map<String, Object> getCycleRunSummary(Long userId, Long cycleRunId) {
        CycleRunAggregate aggregate = loadCycleRunAggregate(userId, cycleRunId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cycleRunId", aggregate.cycleRun().getId());
        result.put("runNo", aggregate.cycleRun().getRunNo());
        result.put("status", aggregate.cycleRun().getStatus());
        result.put("startedAt", aggregate.cycleRun().getStartedAt());
        result.put("completedAt", aggregate.cycleRun().getCompletedAt());
        result.put("templateId", aggregate.cycleRun().getTemplateId());
        result.put("templateVersionId", aggregate.cycleRun().getTemplateVersionId());
        result.put("templateName", aggregate.templateName());
        result.put("cycleLength", aggregate.cycleLength());
        result.put("sessionCount", aggregate.sessions().size());
        result.put("recommendedMissingFields", recommendedMissingFields(userId));
        return result;
    }

    public Map<String, Object> getCycleRunSessionsDetail(Long userId, Long cycleRunId) {
        return loadCycleRunAggregate(userId, cycleRunId).sessionsDetail();
    }

    public Map<String, Object> getCycleRunAggregatedAnalysis(Long userId, Long cycleRunId) {
        return loadCycleRunAggregate(userId, cycleRunId).aggregatedAnalysis();
    }

    public Map<String, Object> versionSnapshotToSummary(VersionSnapshot snapshot) {
        List<Map<String, Object>> days = new ArrayList<>();
        for (DaySnapshot day : snapshot.days()) {
            List<Map<String, Object>> exercises = new ArrayList<>();
            for (ExerciseSnapshot exercise : day.exercises()) {
                List<Map<String, Object>> items = new ArrayList<>();
                for (ItemSnapshot item : exercise.items()) {
                    List<Map<String, Object>> metrics = new ArrayList<>();
                    for (MetricSnapshot metric : item.metrics()) {
                        metrics.add(linkedMap(
                                "metricKey", metric.metricKey(),
                                "metricValueNumber", metric.metricValueNumber(),
                                "sortOrder", metric.sortOrder()));
                    }
                    items.add(linkedMap(
                            "itemIndex", item.itemIndex(),
                            "itemType", item.itemType(),
                            "itemName", item.itemName(),
                            "note", item.note(),
                            "metrics", metrics));
                }
                exercises.add(linkedMap(
                        "sortOrder", exercise.sortOrder(),
                        "exerciseId", exercise.exerciseId(),
                        "exerciseNameSnapshot", exercise.exerciseNameSnapshot(),
                        "structureType", exercise.structureType(),
                        "note", exercise.note(),
                        "items", items));
            }
            days.add(linkedMap(
                    "dayIndex", day.dayIndex(),
                    "dayName", day.dayName(),
                    "exercises", exercises));
        }
        return Map.of("days", days, "dayCount", days.size());
    }

    private CycleRunAggregate loadCycleRunAggregate(Long userId, Long cycleRunId) {
        CycleRunEntity cycleRun = cycleRunMapper.selectById(cycleRunId);
        if (cycleRun == null || !userId.equals(cycleRun.getUserId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        CycleTemplateEntity template = cycleTemplateMapper.selectById(cycleRun.getTemplateId());
        VersionSnapshot versionSnapshot =
                cycleTemplateVersionDomainService.loadVersionSnapshot(cycleRun.getTemplateVersionId());
        List<TrainingSessionEntity> sessions = trainingSessionMapper.selectByCycleRunIdAndUserId(cycleRunId, userId);
        List<TrainingSessionExerciseEntity> exercises = loadExercises(sessions);
        List<TrainingSessionExerciseItemEntity> items = loadItems(exercises);
        List<TrainingSessionExerciseItemMetricEntity> metrics = loadMetrics(items);
        String templateName = resolveTemplateName(sessions, template);
        Integer cycleLength = resolveCycleLength(cycleRun, template, versionSnapshot);
        Map<String, Object> sessionsDetail = buildSessionsDetail(sessions, exercises, items, metrics);
        Map<String, Object> aggregated = buildAggregatedAnalysis(
                userId,
                cycleRun,
                sessions,
                exercises,
                metrics,
                templateName,
                cycleLength);
        return new CycleRunAggregate(cycleRun, templateName, cycleLength, sessions, sessionsDetail, aggregated);
    }

    private List<TrainingSessionExerciseEntity> loadExercises(List<TrainingSessionEntity> sessions) {
        if (sessions.isEmpty()) {
            return List.of();
        }
        return trainingSessionExerciseMapper.selectBySessionIds(
                sessions.stream().map(TrainingSessionEntity::getId).toList());
    }

    private List<TrainingSessionExerciseItemEntity> loadItems(List<TrainingSessionExerciseEntity> exercises) {
        if (exercises.isEmpty()) {
            return List.of();
        }
        return trainingSessionExerciseItemMapper.selectBySessionExerciseIds(
                exercises.stream().map(TrainingSessionExerciseEntity::getId).toList());
    }

    private List<TrainingSessionExerciseItemMetricEntity> loadMetrics(List<TrainingSessionExerciseItemEntity> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        return trainingSessionExerciseItemMetricMapper.selectBySessionExerciseItemIds(
                items.stream().map(TrainingSessionExerciseItemEntity::getId).toList());
    }

    private Map<String, Object> buildSessionsDetail(
            List<TrainingSessionEntity> sessions,
            List<TrainingSessionExerciseEntity> exercises,
            List<TrainingSessionExerciseItemEntity> items,
            List<TrainingSessionExerciseItemMetricEntity> metrics) {
        Map<Long, List<TrainingSessionExerciseEntity>> exerciseBySessionId = new LinkedHashMap<>();
        for (TrainingSessionExerciseEntity exercise : exercises) {
            exerciseBySessionId.computeIfAbsent(exercise.getSessionId(), key -> new ArrayList<>()).add(exercise);
        }
        Map<Long, List<TrainingSessionExerciseItemEntity>> itemByExerciseId = new LinkedHashMap<>();
        for (TrainingSessionExerciseItemEntity item : items) {
            itemByExerciseId.computeIfAbsent(item.getSessionExerciseId(), key -> new ArrayList<>()).add(item);
        }
        Map<Long, List<TrainingSessionExerciseItemMetricEntity>> metricByItemId = new LinkedHashMap<>();
        for (TrainingSessionExerciseItemMetricEntity metric : metrics) {
            metricByItemId.computeIfAbsent(metric.getSessionExerciseItemId(), key -> new ArrayList<>()).add(metric);
        }

        List<Map<String, Object>> sessionValues = new ArrayList<>();
        for (TrainingSessionEntity session : sessions) {
            List<Map<String, Object>> exerciseValues = new ArrayList<>();
            for (TrainingSessionExerciseEntity exercise : exerciseBySessionId.getOrDefault(session.getId(), List.of())) {
                List<Map<String, Object>> itemValues = new ArrayList<>();
                for (TrainingSessionExerciseItemEntity item : itemByExerciseId.getOrDefault(exercise.getId(), List.of())) {
                    List<Map<String, Object>> metricValues = new ArrayList<>();
                    for (TrainingSessionExerciseItemMetricEntity metric : metricByItemId.getOrDefault(item.getId(), List.of())) {
                        metricValues.add(linkedMap(
                                "metricKey", metric.getMetricKey(),
                                "plannedValueNumber", metric.getPlannedValueNumber(),
                                "actualValueNumber", metric.getActualValueNumber(),
                                "sortOrder", metric.getSortOrder()));
                    }
                    Map<String, Object> itemValue = new LinkedHashMap<>();
                    itemValue.put("itemIndex", item.getItemIndex());
                    itemValue.put("itemType", item.getItemType());
                    itemValue.put("itemNameSnapshot", item.getItemNameSnapshot());
                    itemValue.put("noteSnapshot", item.getNoteSnapshot());
                    itemValue.put("metrics", metricValues);
                    itemValues.add(itemValue);
                }
                Map<String, Object> exerciseValue = new LinkedHashMap<>();
                exerciseValue.put("exerciseId", exercise.getExerciseId());
                exerciseValue.put("exerciseNameSnapshot", exercise.getExerciseNameSnapshot());
                exerciseValue.put("structureType", exercise.getStructureType());
                exerciseValue.put("exerciseStatus", exercise.getExerciseStatus());
                exerciseValue.put("feeling", exercise.getFeeling());
                exerciseValue.put("failureReason", exercise.getFailureReason());
                exerciseValue.put("adjustmentNote", exercise.getAdjustmentNote());
                exerciseValue.put("sortOrder", exercise.getSortOrder());
                exerciseValue.put("items", itemValues);
                exerciseValues.add(exerciseValue);
            }
            Map<String, Object> sessionValue = new LinkedHashMap<>();
            sessionValue.put("sessionId", session.getId());
            sessionValue.put("dayIndex", session.getDayIndex());
            sessionValue.put("status", session.getStatus());
            sessionValue.put("sessionType", session.getSessionType());
            sessionValue.put("templateNameSnapshot", session.getTemplateNameSnapshot());
            sessionValue.put("dayNameSnapshot", session.getDayNameSnapshot());
            sessionValue.put("startedAt", session.getStartedAt());
            sessionValue.put("completedAt", session.getCompletedAt());
            sessionValue.put("overallFeeling", session.getOverallFeeling());
            sessionValue.put("notes", session.getNotes());
            sessionValue.put("exercises", exerciseValues);
            sessionValues.add(sessionValue);
        }
        return Map.of("sessions", sessionValues, "sessionCount", sessionValues.size());
    }

    private Map<String, Object> buildAggregatedAnalysis(
            Long userId,
            CycleRunEntity cycleRun,
            List<TrainingSessionEntity> sessions,
            List<TrainingSessionExerciseEntity> exercises,
            List<TrainingSessionExerciseItemMetricEntity> metrics,
            String templateName,
            Integer cycleLength) {
        List<String> feedbackTexts = new ArrayList<>();
        List<String> failureReasons = new ArrayList<>();
        int nonCompletedExercises = 0;
        int completedExercises = 0;
        int matchedMetrics = 0;
        Map<String, Integer> failureReasonCount = new LinkedHashMap<>();
        Set<Integer> completedDays = new LinkedHashSet<>();

        for (TrainingSessionEntity session : sessions) {
            completedDays.add(session.getDayIndex());
            addText(feedbackTexts, session.getOverallFeeling());
            addText(feedbackTexts, session.getNotes());
        }
        for (TrainingSessionExerciseEntity exercise : exercises) {
            if ("completed".equals(exercise.getExerciseStatus())) {
                completedExercises++;
            } else {
                nonCompletedExercises++;
            }
            addText(feedbackTexts, exercise.getFeeling());
            addText(feedbackTexts, exercise.getAdjustmentNote());
            if (StringUtils.hasText(exercise.getFailureReason())) {
                String value = exercise.getFailureReason().trim();
                failureReasons.add(value);
                failureReasonCount.merge(value, 1, Integer::sum);
            }
        }
        for (TrainingSessionExerciseItemMetricEntity metric : metrics) {
            if (metric.getPlannedValueNumber() != null
                    && metric.getActualValueNumber() != null
                    && metric.getPlannedValueNumber().compareTo(metric.getActualValueNumber()) == 0) {
                matchedMetrics++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cycleRunId", cycleRun.getId());
        result.put("runNo", cycleRun.getRunNo());
        result.put("templateName", templateName);
        result.put("cycleLength", cycleLength);
        result.put("completedDayCount", completedDays.size());
        result.put("sessionCount", sessions.size());
        result.put("exerciseCount", exercises.size());
        result.put("completedExerciseCount", completedExercises);
        result.put("nonCompletedExerciseCount", nonCompletedExercises);
        result.put("matchedActualMetricCount", matchedMetrics);
        result.put("feedbackTexts", feedbackTexts);
        result.put("failureReasons", failureReasons);
        result.put("failureReasonCount", failureReasonCount);
        result.put("containsPainSignal", containsPainSignal(feedbackTexts));
        result.put("recommendedMissingFields", recommendedMissingFields(userId));
        return result;
    }

    private List<String> recommendedMissingFields(Long userId) {
        UserProfileEntity profile = userProfileMapper.selectById(userId);
        UserCurrentBodyMetricsEntity metrics = userCurrentBodyMetricsMapper.selectById(userId);
        List<String> values = new ArrayList<>();
        if (profile == null || !StringUtils.hasText(profile.getGoalType())) {
            values.add("goalType");
        }
        if (profile == null || !StringUtils.hasText(profile.getTrainingLevel())) {
            values.add("trainingLevel");
        }
        if (metrics == null || metrics.getCurrentWeightKg() == null) {
            values.add("currentWeightKg");
        }
        return values;
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

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 10;
        }
        return Math.min(limit, 20);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void addText(List<String> texts, String value) {
        if (StringUtils.hasText(value)) {
            texts.add(value.trim());
        }
    }

    private boolean containsPainSignal(List<String> feedbackTexts) {
        String normalized = String.join(" ", feedbackTexts).toLowerCase(Locale.ROOT);
        for (String keyword : PAIN_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> linkedMap(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues length must be even");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            result.put((String) keyValues[index], keyValues[index + 1]);
        }
        return result;
    }

    private record CycleRunAggregate(
            CycleRunEntity cycleRun,
            String templateName,
            Integer cycleLength,
            List<TrainingSessionEntity> sessions,
            Map<String, Object> sessionsDetail,
            Map<String, Object> aggregatedAnalysis) {
    }
}
