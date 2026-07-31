package com.dailyforge.modules.aicoach.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
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
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemMetricEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMetricMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiCycleSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiCycleSummaryService.class);
    private static final List<String> PAIN_KEYWORDS = List.of(
            "pain",
            "hurt",
            "sore",
            "injury",
            "discomfort",
            "ache",
            "sharp",
            "strain");

    private final AiTaskRecordMapper taskMapper;
    private final CycleRunMapper cycleRunMapper;
    private final CycleTemplateMapper templateMapper;
    private final CycleTemplateVersionDomainService versionService;
    private final TrainingSessionMapper sessionMapper;
    private final TrainingSessionExerciseMapper exerciseMapper;
    private final TrainingSessionExerciseItemMapper itemMapper;
    private final TrainingSessionExerciseItemMetricMapper metricMapper;
    private final UserProfileMapper profileMapper;
    private final UserCurrentBodyMetricsMapper metricsMapper;
    private final ObjectMapper objectMapper;

    public AiCycleSummaryService(
            AiTaskRecordMapper taskMapper,
            CycleRunMapper cycleRunMapper,
            CycleTemplateMapper templateMapper,
            CycleTemplateVersionDomainService versionService,
            TrainingSessionMapper sessionMapper,
            TrainingSessionExerciseMapper exerciseMapper,
            TrainingSessionExerciseItemMapper itemMapper,
            TrainingSessionExerciseItemMetricMapper metricMapper,
            UserProfileMapper profileMapper,
            UserCurrentBodyMetricsMapper metricsMapper,
            ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.cycleRunMapper = cycleRunMapper;
        this.templateMapper = templateMapper;
        this.versionService = versionService;
        this.sessionMapper = sessionMapper;
        this.exerciseMapper = exerciseMapper;
        this.itemMapper = itemMapper;
        this.metricMapper = metricMapper;
        this.profileMapper = profileMapper;
        this.metricsMapper = metricsMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processTask(Long taskId) {
        AiTaskRecordEntity task = requireTask(taskId);
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
        SummaryStats stats = summarize(sessions);
        List<String> recommended = recommended(profile, metrics);
        String historicalTemplateName = resolveTemplateName(sessions, template);
        Integer historicalCycleLength = resolveCycleLength(cycleRun, template, versionSnapshot);
        CycleSummaryTaskResultResponse result = buildResult(
                cycleRun,
                historicalTemplateName,
                historicalCycleLength,
                stats,
                recommended);

        LocalDateTime completedAt = LocalDateTime.now();
        task.setInputSummaryJson(write(inputSummary(
                request,
                cycleRun,
                historicalTemplateName,
                historicalCycleLength,
                profile,
                metrics,
                stats)));
        task.setResultJson(write(result));
        task.setOutputPreview(limit(result.executionOverview(), 1000));
        task.setToolCallCount(0);
        task.setRepairAttemptCount(0);
        task.setLatencyMs(task.getStartedAt() == null ? 0 : (int) Duration.between(task.getStartedAt(), completedAt).toMillis());
        task.setCompletedAt(completedAt);
        task.setStatus("succeeded");
        task.setErrorCode(null);
        task.setErrorMessage(null);
        taskMapper.updateById(task);
        log.debug("AI cycle summary task succeeded. taskId={}, cycleRunId={}, sessionCount={}", taskId, cycleRunId, sessions.size());
    }

    private AiTaskRecordEntity requireTask(Long taskId) {
        AiTaskRecordEntity task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null || !"cycle_summary".equals(task.getTaskType())) {
            throw new BusinessException(ErrorCode.AI_TASK_NOT_FOUND);
        }
        if (!"running".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "ai task is not running");
        }
        return task;
    }

    private SummaryStats summarize(List<TrainingSessionEntity> sessions) {
        if (sessions.isEmpty()) {
            return new SummaryStats(0, 0, 0, 0, List.of(), List.of());
        }
        List<Long> sessionIds = sessions.stream().map(TrainingSessionEntity::getId).toList();
        List<TrainingSessionExerciseEntity> exercises = exerciseMapper.selectBySessionIds(sessionIds);
        List<Long> exerciseIds = exercises.stream().map(TrainingSessionExerciseEntity::getId).toList();
        List<TrainingSessionExerciseItemEntity> items = exerciseIds.isEmpty()
                ? List.of()
                : itemMapper.selectBySessionExerciseIds(exerciseIds);
        List<Long> itemIds = items.stream().map(TrainingSessionExerciseItemEntity::getId).toList();
        List<TrainingSessionExerciseItemMetricEntity> metricEntities = itemIds.isEmpty()
                ? List.of()
                : metricMapper.selectBySessionExerciseItemIds(itemIds);

        List<String> feedback = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        int nonCompleted = 0;
        int matchedActual = 0;
        for (TrainingSessionEntity session : sessions) {
            addText(feedback, session.getOverallFeeling());
            addText(feedback, session.getNotes());
        }
        for (TrainingSessionExerciseEntity exercise : exercises) {
            if (!"completed".equals(exercise.getExerciseStatus())) {
                nonCompleted++;
            }
            addText(feedback, exercise.getFeeling());
            addText(feedback, exercise.getAdjustmentNote());
            if (StringUtils.hasText(exercise.getFailureReason())) {
                reasons.add(exercise.getFailureReason().trim());
            }
        }
        for (TrainingSessionExerciseItemMetricEntity metric : metricEntities) {
            if (metric.getActualValueNumber() != null
                    && metric.getPlannedValueNumber() != null
                    && metric.getActualValueNumber().compareTo(metric.getPlannedValueNumber()) == 0) {
                matchedActual++;
            }
        }
        return new SummaryStats(
                sessions.size(),
                exercises.size(),
                nonCompleted,
                matchedActual,
                feedback,
                reasons);
    }

    private CycleSummaryTaskResultResponse buildResult(
            CycleRunEntity cycleRun,
            String templateName,
            Integer cycleLength,
            SummaryStats stats,
            List<String> recommended) {
        List<String> strengths = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        List<String> causes = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> risks = new ArrayList<>();

        strengths.add("The cycle produced " + stats.sessionCount() + " logged training days for the next planning round.");
        if (stats.nonCompletedExerciseCount() == 0) {
            strengths.add("All recorded exercises were marked completed, so the current workload looks manageable.");
        } else {
            issues.add("There were " + stats.nonCompletedExerciseCount() + " exercises marked as partial, failed, or skipped.");
            strengths.add("Most sessions still moved forward even when some exercises were not fully completed.");
            suggestions.add("Reduce volume or simplify exercise order for the movements that repeatedly break down.");
        }

        if (stats.planMatchedMetricCount() > 0) {
            strengths.add("Several recorded actual metrics matched the plan, which suggests the current prescriptions are realistic.");
            suggestions.add("For stable main lifts, consider a small load increase or one extra rep next cycle.");
        } else {
            causes.add("The current history is more useful for identifying weak links than for aggressive progression.");
        }

        String feedbackText = String.join(" ", stats.feedbackTexts()).toLowerCase(Locale.ROOT);
        if (containsPain(feedbackText)) {
            issues.add("The feedback contains pain or discomfort signals that should change the next cycle.");
            causes.add("Pain-related notes usually point to exercise choice, range of motion, or fatigue management issues.");
            risks.add("Do not add load to exercises that repeatedly trigger discomfort before replacing or modifying them.");
            suggestions.add("Replace pain-triggering movements with lower-irritation alternatives and keep notes on what changed.");
        }

        if (issues.isEmpty()) {
            issues.add("No major execution issue was obvious from the current structured records.");
        }
        if (causes.isEmpty()) {
            causes.add("Most adjustments should come from exercise-level feedback rather than a full template rebuild.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Carry forward the stable parts of this cycle and only modify the weakest link first.");
        }
        if (risks.isEmpty() && stats.nonCompletedExerciseCount() > 0) {
            risks.add("Prepare swap options for exercises that repeatedly fail for non-recovery reasons.");
        }

        String notice = recommended.isEmpty()
                ? null
                : "Some profile or body-metric fields are still missing. Add them before the next AI analysis for more specific recommendations.";
        return new CycleSummaryTaskResultResponse(
                cycleRun.getId(),
                cycleRun.getTemplateId(),
                templateName,
                cycleRun.getRunNo(),
                cycleLength,
                "Run #" + cycleRun.getRunNo() + " finished with " + stats.sessionCount()
                        + " logged days and " + stats.nonCompletedExerciseCount()
                        + " exercises that deviated from the original plan.",
                strengths,
                issues,
                causes,
                suggestions,
                risks,
                notice);
    }

    private Map<String, Object> inputSummary(
            CycleSummaryRequest request,
            CycleRunEntity cycleRun,
            String templateName,
            Integer cycleLength,
            UserProfileEntity profile,
            UserCurrentBodyMetricsEntity metrics,
            SummaryStats stats) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("request", request);
        summary.put("cycleRunId", cycleRun.getId());
        summary.put("runNo", cycleRun.getRunNo());
        summary.put("templateName", templateName);
        summary.put("cycleLength", cycleLength);
        summary.put("goalType", profile == null ? null : profile.getGoalType());
        summary.put("trainingLevel", profile == null ? null : profile.getTrainingLevel());
        summary.put("currentWeightKg", metrics == null ? null : metrics.getCurrentWeightKg());
        summary.put("stats", Map.of(
                "sessionCount", stats.sessionCount(),
                "exerciseCount", stats.exerciseCount(),
                "nonCompletedExerciseCount", stats.nonCompletedExerciseCount(),
                "matchedActualMetricCount", stats.planMatchedMetricCount(),
                "feedbackTextCount", stats.feedbackTexts().size(),
                "failureReasonCount", stats.failureReasons().size()));
        return summary;
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
            for (DaySnapshot day : versionSnapshot.days()) {
                if (day.dayIndex() != null) {
                    maxDayIndex = Math.max(maxDayIndex, day.dayIndex());
                }
            }
            if (maxDayIndex > 0) {
                return maxDayIndex;
            }
            return versionSnapshot.days().size();
        }
        return template == null ? null : template.getCycleLength();
    }

    private List<String> recommended(UserProfileEntity profile, UserCurrentBodyMetricsEntity metrics) {
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

    private boolean containsPain(String text) {
        for (String keyword : PAIN_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void addText(List<String> texts, String value) {
        if (StringUtils.hasText(value)) {
            texts.add(value.trim());
        }
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

    private record SummaryStats(
            int sessionCount,
            int exerciseCount,
            int nonCompletedExerciseCount,
            int planMatchedMetricCount,
            List<String> feedbackTexts,
            List<String> failureReasons) {
    }
}
