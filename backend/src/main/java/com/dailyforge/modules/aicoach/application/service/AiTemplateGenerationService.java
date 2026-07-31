package com.dailyforge.modules.aicoach.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskRecordMapper;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationTaskResultResponse;
import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.exercise.application.service.SystemExerciseLookupService;
import com.dailyforge.modules.plan.domain.model.MetricKey;
import com.dailyforge.modules.plan.domain.service.CycleTemplatePolicyService;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.domain.service.ExerciseStructurePolicyService;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateVersionEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateVersionMapper;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateExerciseRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateItemRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateMetricRequest;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserCurrentBodyMetricsMapper;
import com.dailyforge.modules.profile.infrastructure.persistence.mapper.UserProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiTemplateGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AiTemplateGenerationService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AiTaskRecordMapper taskMapper;
    private final UserProfileMapper profileMapper;
    private final UserCurrentBodyMetricsMapper metricsMapper;
    private final SystemExerciseLookupService lookupService;
    private final CycleTemplateMapper templateMapper;
    private final CycleTemplateVersionMapper versionMapper;
    private final CycleTemplateVersionDomainService versionService;
    private final CycleTemplatePolicyService templatePolicyService;
    private final ExerciseStructurePolicyService structurePolicyService;
    private final ObjectMapper objectMapper;

    public AiTemplateGenerationService(
            AiTaskRecordMapper taskMapper,
            UserProfileMapper profileMapper,
            UserCurrentBodyMetricsMapper metricsMapper,
            SystemExerciseLookupService lookupService,
            CycleTemplateMapper templateMapper,
            CycleTemplateVersionMapper versionMapper,
            CycleTemplateVersionDomainService versionService,
            CycleTemplatePolicyService templatePolicyService,
            ExerciseStructurePolicyService structurePolicyService,
            ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.profileMapper = profileMapper;
        this.metricsMapper = metricsMapper;
        this.lookupService = lookupService;
        this.templateMapper = templateMapper;
        this.versionMapper = versionMapper;
        this.versionService = versionService;
        this.templatePolicyService = templatePolicyService;
        this.structurePolicyService = structurePolicyService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processTask(Long taskId) {
        AiTaskRecordEntity task = requireTask(taskId);
        TemplateGenerationRequest request = read(task.getRequestPayloadJson(), TemplateGenerationRequest.class);
        UserProfileEntity profile = profileMapper.selectById(task.getUserId());
        UserCurrentBodyMetricsEntity metrics = metricsMapper.selectById(task.getUserId());
        Map<String, List<SystemExerciseLookupResult>> pools = loadPools(request.sceneType(), request.includeCardio());
        List<CycleTemplateDayRequest> days = buildDays(request, profile, metrics, pools);
        Map<Long, SystemExerciseLookupResult> exerciseMap = loadExerciseMap(days);
        templatePolicyService.validateDraftCycleLength(request.cycleLength());
        templatePolicyService.validateDayRequests(request.cycleLength(), days);
        structurePolicyService.validateDayRequests(days, exerciseMap);

        CycleTemplateEntity template = new CycleTemplateEntity();
        template.setUserId(task.getUserId());
        template.setName("AI Generated " + request.goalType() + " " + LocalDateTime.now().format(FMT));
        template.setCycleLength(request.cycleLength());
        template.setGoalType(request.goalType());
        template.setStatus("draft");
        templateMapper.insert(template);

        CycleTemplateVersionEntity version = versionService.createVersion(template.getId(), "ai_generated", "ai_task_" + taskId);
        version.setSourceTaskId(taskId);
        versionMapper.updateById(version);
        versionService.saveFullVersionContent(version.getId(), days, exerciseMap);
        template.setCurrentVersionId(version.getId());
        templateMapper.updateById(template);

        TemplateGenerationTaskResultResponse result = new TemplateGenerationTaskResultResponse(
                toDraft(template, days, exerciseMap),
                toRationale(request, profile, metrics, days, exerciseMap));
        LocalDateTime completedAt = LocalDateTime.now();
        task.setRelatedEntityType("cycle_template_version");
        task.setRelatedEntityId(version.getId());
        task.setInputSummaryJson(write(inputSummary(request, profile, metrics, pools)));
        task.setResultJson(write(result));
        task.setOutputPreview(limit(result.generationRationale().overallDesignSummary(), 1000));
        task.setToolCallCount(0);
        task.setRepairAttemptCount(0);
        task.setLatencyMs(task.getStartedAt() == null ? 0 : (int) Duration.between(task.getStartedAt(), completedAt).toMillis());
        task.setCompletedAt(completedAt);
        task.setStatus("succeeded");
        task.setErrorCode(null);
        task.setErrorMessage(null);
        taskMapper.updateById(task);
        log.debug("AI template task succeeded. taskId={}, templateId={}, versionId={}", taskId, template.getId(), version.getId());
    }

    private AiTaskRecordEntity requireTask(Long taskId) {
        AiTaskRecordEntity task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null || !"template_generation".equals(task.getTaskType())) {
            throw new BusinessException(ErrorCode.AI_TASK_NOT_FOUND);
        }
        if (!"running".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "ai task is not running");
        }
        return task;
    }

    private Map<String, List<SystemExerciseLookupResult>> loadPools(String sceneType, boolean includeCardio) {
        Map<String, List<Long>> ids = new LinkedHashMap<>();
        if ("home".equals(sceneType)) {
            ids.put("push", List.of(1L, 6L, 3L));
            ids.put("pull", List.of(8L, 13L, 30L));
            ids.put("legs", List.of(17L, 18L, 27L));
            ids.put("core", List.of(20L, 21L));
            ids.put("cardio", List.of());
        } else {
            ids.put("push", List.of(2L, 3L, 5L, 6L, 7L));
            ids.put("pull", List.of(8L, 9L, 10L, 12L, 13L));
            ids.put("legs", List.of(14L, 16L, 17L, 19L));
            ids.put("core", List.of(20L, 21L, 22L));
            ids.put("cardio", List.of(23L, 24L, 25L, 26L));
        }
        Map<String, List<SystemExerciseLookupResult>> pools = new LinkedHashMap<>();
        for (Map.Entry<String, List<Long>> entry : ids.entrySet()) {
            Map<Long, SystemExerciseLookupResult> lookup = lookupService.loadActiveSystemExercisesByIds(entry.getValue());
            List<SystemExerciseLookupResult> values = new ArrayList<>();
            for (Long id : entry.getValue()) {
                if (lookup.containsKey(id)) {
                    values.add(lookup.get(id));
                }
            }
            if (!includeCardio && "cardio".equals(entry.getKey())) {
                values = List.of();
            }
            pools.put(entry.getKey(), values);
        }
        return pools;
    }

    private List<CycleTemplateDayRequest> buildDays(
            TemplateGenerationRequest request,
            UserProfileEntity profile,
            UserCurrentBodyMetricsEntity metrics,
            Map<String, List<SystemExerciseLookupResult>> pools) {
        List<CycleTemplateDayRequest> days = new ArrayList<>();
        if (request.cycleLength() == 1) {
            days.add(new CycleTemplateDayRequest(
                    1,
                    "Full Body",
                    pack(List.of(
                            ex(1, first(pools, "push"), profile, metrics, request.goalType(), true),
                            ex(2, first(pools, "pull"), profile, metrics, request.goalType(), true),
                            ex(3, first(pools, "legs"), profile, metrics, request.goalType(), true),
                            ex(4, pick(pools, "core", 0), profile, metrics, request.goalType(), false)))));
            return days;
        }

        days.add(new CycleTemplateDayRequest(
                1,
                request.cycleLength() == 2 ? "Upper" : "Push",
                pack(List.of(
                        ex(1, first(pools, "push"), profile, metrics, request.goalType(), true),
                        ex(2, pick(pools, "push", 1), profile, metrics, request.goalType(), true),
                        ex(3, pick(pools, "core", 0), profile, metrics, request.goalType(), false)))));
        days.add(new CycleTemplateDayRequest(
                2,
                request.cycleLength() == 2 ? "Lower" : "Pull",
                pack(List.of(
                        ex(1, first(pools, request.cycleLength() == 2 ? "legs" : "pull"), profile, metrics, request.goalType(), true),
                        ex(2, pick(pools, request.cycleLength() == 2 ? "legs" : "pull", 1), profile, metrics, request.goalType(), true),
                        ex(3, pick(pools, "core", 0), profile, metrics, request.goalType(), false)))));
        if (request.cycleLength() == 2) {
            return days;
        }

        days.add(new CycleTemplateDayRequest(
                3,
                "Legs",
                pack(List.of(
                        ex(1, first(pools, "legs"), profile, metrics, request.goalType(), true),
                        ex(2, pick(pools, "legs", 1), profile, metrics, request.goalType(), true),
                        ex(3, pick(pools, "core", 0), profile, metrics, request.goalType(), false)))));
        if (request.cycleLength() >= 4) {
            List<CycleTemplateExerciseRequest> recovery = new ArrayList<>();
            SystemExerciseLookupResult cardio = pick(pools, "cardio", 0);
            if (request.includeCardio() && cardio != null) {
                recovery.add(seg(1, cardio, 1200, BigDecimal.valueOf(6)));
            }
            days.add(new CycleTemplateDayRequest(4, recovery.isEmpty() ? "Recovery" : "Recovery Cardio", recovery));
        }
        if (request.cycleLength() >= 5) {
            days.add(new CycleTemplateDayRequest(
                    5,
                    "Upper Accessory",
                    pack(List.of(
                            ex(1, pick(pools, "push", 1), profile, metrics, request.goalType(), false),
                            ex(2, pick(pools, "pull", 1), profile, metrics, request.goalType(), false),
                            ex(3, pick(pools, "core", 0), profile, metrics, request.goalType(), false)))));
        }
        if (request.cycleLength() >= 6) {
            days.add(new CycleTemplateDayRequest(
                    6,
                    "Lower Accessory",
                    pack(List.of(
                            ex(1, pick(pools, "legs", 1), profile, metrics, request.goalType(), false),
                            ex(2, pick(pools, "core", 1), profile, metrics, request.goalType(), false)))));
        }
        if (request.cycleLength() >= 7) {
            days.add(new CycleTemplateDayRequest(7, "Rest", List.of()));
        }
        return days;
    }

    private Map<Long, SystemExerciseLookupResult> loadExerciseMap(List<CycleTemplateDayRequest> days) {
        Set<Long> ids = new LinkedHashSet<>();
        for (CycleTemplateDayRequest day : days) {
            if (day.exercises() == null) {
                continue;
            }
            for (CycleTemplateExerciseRequest exercise : day.exercises()) {
                ids.add(exercise.exerciseId());
            }
        }
        Map<Long, SystemExerciseLookupResult> map = lookupService.loadActiveSystemExercisesByIds(ids);
        if (map.size() != ids.size()) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_EXERCISE_NOT_FOUND);
        }
        return map;
    }

    private CycleTemplateExerciseRequest ex(
            int sortOrder,
            SystemExerciseLookupResult exercise,
            UserProfileEntity profile,
            UserCurrentBodyMetricsEntity metrics,
            String goalType,
            boolean compound) {
        if (exercise == null) {
            return null;
        }
        if ("single_segment".equals(exercise.defaultStructureType())) {
            return seg(sortOrder, exercise, 900, BigDecimal.valueOf(5));
        }

        int sets = profile != null && "experienced".equals(profile.getTrainingLevel())
                ? (compound ? 4 : 3)
                : (compound ? 3 : 2);
        int reps = switch (goalType) {
            case "fat_loss" -> 12;
            case "health_maintenance" -> 10;
            default -> 8;
        };
        int rest = compound ? 120 : 75;
        BigDecimal weight = "kg".equalsIgnoreCase(exercise.defaultUnit())
                ? baseWeight(exercise, profile, metrics, compound)
                : null;

        List<CycleTemplateItemRequest> items = new ArrayList<>();
        for (int i = 1; i <= sets; i++) {
            List<CycleTemplateMetricRequest> metricRequests = new ArrayList<>();
            int order = 1;
            if (weight != null) {
                metricRequests.add(metric(order++, MetricKey.WEIGHT_KG.getValue(),
                        i <= 2 ? weight : round(weight.multiply(BigDecimal.valueOf(0.95)), BigDecimal.valueOf(2.5))));
                metricRequests.add(metric(order++, MetricKey.REPS.getValue(), BigDecimal.valueOf(reps)));
            } else if ("seconds".equalsIgnoreCase(exercise.defaultUnit())) {
                metricRequests.add(metric(order++, MetricKey.DURATION_SECONDS.getValue(), BigDecimal.valueOf(compound ? 45 : 30)));
            } else {
                metricRequests.add(metric(order++, MetricKey.REPS.getValue(), BigDecimal.valueOf(reps)));
            }
            metricRequests.add(metric(order++, MetricKey.REST_SECONDS.getValue(), BigDecimal.valueOf(rest)));
            metricRequests.add(metric(
                    order,
                    weight == null && "seconds".equalsIgnoreCase(exercise.defaultUnit())
                            ? MetricKey.INTENSITY_LEVEL.getValue()
                            : MetricKey.RPE.getValue(),
                    compound ? BigDecimal.valueOf(7) : BigDecimal.valueOf(6)));
            items.add(new CycleTemplateItemRequest(i, "set", "Set " + i, null, metricRequests));
        }
        return new CycleTemplateExerciseRequest(sortOrder, exercise.id(), "set_based", null, items);
    }

    private CycleTemplateExerciseRequest seg(
            int sortOrder,
            SystemExerciseLookupResult exercise,
            int durationSeconds,
            BigDecimal intensity) {
        return new CycleTemplateExerciseRequest(
                sortOrder,
                exercise.id(),
                "single_segment",
                null,
                List.of(new CycleTemplateItemRequest(
                        1,
                        "segment",
                        "Main Segment",
                        null,
                        List.of(
                                metric(1, MetricKey.DURATION_SECONDS.getValue(), BigDecimal.valueOf(durationSeconds)),
                                metric(2, MetricKey.INTENSITY_LEVEL.getValue(), intensity)))));
    }

    private CycleTemplateMetricRequest metric(int sortOrder, String key, BigDecimal value) {
        return new CycleTemplateMetricRequest(sortOrder, key, value.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal baseWeight(
            SystemExerciseLookupResult exercise,
            UserProfileEntity profile,
            UserCurrentBodyMetricsEntity metrics,
            boolean compound) {
        BigDecimal bodyWeight = metrics != null && metrics.getCurrentWeightKg() != null
                ? metrics.getCurrentWeightKg()
                : BigDecimal.valueOf(60);
        BigDecimal factor = switch (String.valueOf(exercise.movementType()).toLowerCase(Locale.ROOT)) {
            case "legs" -> BigDecimal.valueOf(0.55);
            case "hinge" -> BigDecimal.valueOf(0.50);
            case "push" -> BigDecimal.valueOf(0.38);
            case "pull" -> BigDecimal.valueOf(0.35);
            default -> compound ? BigDecimal.valueOf(0.25) : BigDecimal.valueOf(0.18);
        };
        if (profile != null && "experienced".equals(profile.getTrainingLevel())) {
            factor = factor.multiply(BigDecimal.valueOf(1.15));
        }
        BigDecimal raw = bodyWeight.multiply(compound ? factor : factor.multiply(BigDecimal.valueOf(0.8)));
        return round(raw.max(BigDecimal.valueOf(5)), BigDecimal.valueOf(2.5));
    }

    private BigDecimal round(BigDecimal value, BigDecimal step) {
        return value.divide(step, 0, RoundingMode.HALF_UP)
                .multiply(step)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private TemplateGenerationTaskResultResponse.DraftTemplate toDraft(
            CycleTemplateEntity template,
            List<CycleTemplateDayRequest> days,
            Map<Long, SystemExerciseLookupResult> exerciseMap) {
        List<TemplateGenerationTaskResultResponse.Day> dayResponses = new ArrayList<>();
        for (CycleTemplateDayRequest day : days) {
            List<TemplateGenerationTaskResultResponse.Exercise> exercises = new ArrayList<>();
            if (day.exercises() != null) {
                for (CycleTemplateExerciseRequest exercise : day.exercises()) {
                    List<TemplateGenerationTaskResultResponse.Item> items = new ArrayList<>();
                    for (CycleTemplateItemRequest item : exercise.items()) {
                        List<TemplateGenerationTaskResultResponse.Metric> metrics = new ArrayList<>();
                        for (CycleTemplateMetricRequest metric : item.metrics()) {
                            metrics.add(new TemplateGenerationTaskResultResponse.Metric(
                                    metric.sortOrder(),
                                    metric.metricKey(),
                                    metric.metricValueNumber(),
                                    unit(metric.metricKey())));
                        }
                        items.add(new TemplateGenerationTaskResultResponse.Item(
                                item.itemIndex(),
                                item.itemType(),
                                item.itemName(),
                                item.note(),
                                metrics));
                    }
                    exercises.add(new TemplateGenerationTaskResultResponse.Exercise(
                            exercise.sortOrder(),
                            exercise.exerciseId(),
                            exerciseMap.get(exercise.exerciseId()).name(),
                            exercise.structureType(),
                            exercise.note(),
                            items));
                }
            }
            dayResponses.add(new TemplateGenerationTaskResultResponse.Day(
                    day.dayIndex(),
                    day.dayName(),
                    exercises.isEmpty(),
                    exercises));
        }
        return new TemplateGenerationTaskResultResponse.DraftTemplate(
                template.getId(),
                template.getName(),
                template.getStatus(),
                template.getCycleLength(),
                dayResponses);
    }

    private TemplateGenerationTaskResultResponse.GenerationRationale toRationale(
            TemplateGenerationRequest request,
            UserProfileEntity profile,
            UserCurrentBodyMetricsEntity metrics,
            List<CycleTemplateDayRequest> days,
            Map<Long, SystemExerciseLookupResult> exerciseMap) {
        List<TemplateGenerationTaskResultResponse.DayRationale> dayRationales = new ArrayList<>();
        List<TemplateGenerationTaskResultResponse.KeyExerciseRationale> keyRationales = new ArrayList<>();
        for (CycleTemplateDayRequest day : days) {
            dayRationales.add(new TemplateGenerationTaskResultResponse.DayRationale(
                    day.dayIndex(),
                    day.dayName(),
                    focus(day.dayName()),
                    day.exercises() == null || day.exercises().isEmpty()
                            ? "This day is left open for recovery and easier adherence."
                            : "This day keeps a single movement focus so the next cycle can adjust it with cleaner feedback."));
            if (day.exercises() != null && !day.exercises().isEmpty()) {
                CycleTemplateExerciseRequest first = day.exercises().get(0);
                keyRationales.add(new TemplateGenerationTaskResultResponse.KeyExerciseRationale(
                        day.dayIndex(),
                        first.exerciseId(),
                        exerciseMap.get(first.exerciseId()).name(),
                        "This movement acts as the main reference lift for this day."));
            }
        }
        List<String> warnings = new ArrayList<>();
        if (profile != null && profile.getInjuryNotes() != null && !profile.getInjuryNotes().isBlank()) {
            warnings.add("Current injury notes were included in the planning context. Replace any movement that triggers discomfort.");
        }
        if (metrics == null || metrics.getCurrentWeightKg() == null) {
            warnings.add("Current weight is missing, so loading guidance stays conservative.");
        }
        return new TemplateGenerationTaskResultResponse.GenerationRationale(
                "This draft uses a " + request.cycleLength()
                        + "-day cycle with one clear training focus per day. The first run should be treated as a calibration round.",
                dayRationales,
                keyRationales,
                new TemplateGenerationTaskResultResponse.IntensityRationale(
                        "starting_recommendation",
                        "Loads are starting recommendations based on current body metrics, movement pattern, and training level."),
                warnings);
    }

    private Map<String, Object> inputSummary(
            TemplateGenerationRequest request,
            UserProfileEntity profile,
            UserCurrentBodyMetricsEntity metrics,
            Map<String, List<SystemExerciseLookupResult>> pools) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("request", request);
        summary.put("profileGoalType", profile == null ? null : profile.getGoalType());
        summary.put("trainingLevel", profile == null ? null : profile.getTrainingLevel());
        summary.put("currentWeightKg", metrics == null ? null : metrics.getCurrentWeightKg());
        summary.put(
                "selectedExerciseIds",
                pools.entrySet().stream().collect(
                        LinkedHashMap::new,
                        (map, entry) -> map.put(
                                entry.getKey(),
                                entry.getValue().stream().map(SystemExerciseLookupResult::id).toList()),
                        LinkedHashMap::putAll));
        return summary;
    }

    private String unit(String metricKey) {
        MetricKey key = MetricKey.fromValue(metricKey);
        return key == null ? null : key.getUnit();
    }

    private SystemExerciseLookupResult first(Map<String, List<SystemExerciseLookupResult>> pools, String key) {
        SystemExerciseLookupResult result = pick(pools, key, 0);
        if (result == null) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "missing exercise pool: " + key);
        }
        return result;
    }

    private SystemExerciseLookupResult pick(Map<String, List<SystemExerciseLookupResult>> pools, String key, int index) {
        List<SystemExerciseLookupResult> list = pools.getOrDefault(key, List.of());
        if (list.isEmpty()) {
            return null;
        }
        return list.get(Math.min(index, list.size() - 1));
    }

    private List<CycleTemplateExerciseRequest> pack(List<CycleTemplateExerciseRequest> source) {
        List<CycleTemplateExerciseRequest> result = new ArrayList<>();
        int sortOrder = 1;
        for (CycleTemplateExerciseRequest exercise : source) {
            if (exercise != null) {
                result.add(new CycleTemplateExerciseRequest(
                        sortOrder++,
                        exercise.exerciseId(),
                        exercise.structureType(),
                        exercise.note(),
                        exercise.items()));
            }
        }
        return result;
    }

    private String focus(String dayName) {
        String value = dayName == null ? "" : dayName.toLowerCase(Locale.ROOT);
        if (value.contains("push")) {
            return "Chest, shoulders, and triceps";
        }
        if (value.contains("pull")) {
            return "Back and biceps";
        }
        if (value.contains("leg")) {
            return "Quads, glutes, and posterior chain";
        }
        if (value.contains("recovery")) {
            return "Cardio recovery and low fatigue work";
        }
        return "Whole-body balance";
    }

    private String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "failed to serialize ai payload");
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "failed to parse ai task request payload");
        }
    }
}
