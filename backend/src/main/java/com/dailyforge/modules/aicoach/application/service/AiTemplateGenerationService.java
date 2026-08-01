package com.dailyforge.modules.aicoach.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.domain.model.TemplateGenerationValidatedResult;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskRecordMapper;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationTaskResultResponse;
import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.plan.domain.model.MetricKey;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateVersionEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateVersionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiTemplateGenerationService {

    private final AiTaskRecordMapper taskMapper;
    private final CycleTemplateMapper templateMapper;
    private final CycleTemplateVersionMapper versionMapper;
    private final CycleTemplateVersionDomainService versionService;
    private final ObjectMapper objectMapper;

    public AiTemplateGenerationService(
            AiTaskRecordMapper taskMapper,
            CycleTemplateMapper templateMapper,
            CycleTemplateVersionMapper versionMapper,
            CycleTemplateVersionDomainService versionService,
            ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.templateMapper = templateMapper;
        this.versionMapper = versionMapper;
        this.versionService = versionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void persistSuccessfulResult(
            Long taskId,
            String inputSummaryJson,
            TemplateGenerationValidatedResult validatedResult) {
        AiTaskRecordEntity task = requireTask(taskId, "template_generation");
        TemplateGenerationRequest request = read(task.getRequestPayloadJson(), TemplateGenerationRequest.class);

        CycleTemplateEntity template = new CycleTemplateEntity();
        template.setUserId(task.getUserId());
        template.setName(validatedResult.templateName());
        template.setCycleLength(validatedResult.cycleLength());
        template.setGoalType(request.goalType());
        template.setStatus("draft");
        templateMapper.insert(template);

        CycleTemplateVersionEntity version = versionService.createVersion(template.getId(), "ai_generated", "ai_task_" + taskId);
        version.setSourceTaskId(taskId);
        versionMapper.updateById(version);
        versionService.saveFullVersionContent(version.getId(), validatedResult.days(), validatedResult.exerciseMap());
        template.setCurrentVersionId(version.getId());
        templateMapper.updateById(template);

        TemplateGenerationTaskResultResponse result =
                toResponse(template, validatedResult.days(), validatedResult.exerciseMap(), validatedResult.generationRationale());
        LocalDateTime completedAt = LocalDateTime.now();
        task.setRelatedEntityType("cycle_template_version");
        task.setRelatedEntityId(version.getId());
        task.setInputSummaryJson(inputSummaryJson);
        task.setResultJson(write(result));
        task.setOutputPreview(limit(validatedResult.generationRationale().overallDesignSummary(), 1000));
        task.setLatencyMs(task.getStartedAt() == null ? 0 : (int) Duration.between(task.getStartedAt(), completedAt).toMillis());
        task.setCompletedAt(completedAt);
        task.setStatus("succeeded");
        task.setErrorCode(null);
        task.setErrorMessage(null);
        taskMapper.updateById(task);
    }

    private TemplateGenerationTaskResultResponse toResponse(
            CycleTemplateEntity template,
            List<com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest> days,
            Map<Long, SystemExerciseLookupResult> exerciseMap,
            TemplateGenerationTaskResultResponse.GenerationRationale generationRationale) {
        List<TemplateGenerationTaskResultResponse.Day> dayResponses = new ArrayList<>();
        for (com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest day : days) {
            List<TemplateGenerationTaskResultResponse.Exercise> exercises = new ArrayList<>();
            if (day.exercises() != null) {
                for (com.dailyforge.modules.plan.interfaces.dto.CycleTemplateExerciseRequest exercise : day.exercises()) {
                    List<TemplateGenerationTaskResultResponse.Item> items = new ArrayList<>();
                    for (com.dailyforge.modules.plan.interfaces.dto.CycleTemplateItemRequest item : exercise.items()) {
                        List<TemplateGenerationTaskResultResponse.Metric> metrics = new ArrayList<>();
                        for (com.dailyforge.modules.plan.interfaces.dto.CycleTemplateMetricRequest metric : item.metrics()) {
                            metrics.add(new TemplateGenerationTaskResultResponse.Metric(
                                    metric.sortOrder(),
                                    metric.metricKey(),
                                    metric.metricValueNumber(),
                                    metricUnit(metric.metricKey())));
                        }
                        items.add(new TemplateGenerationTaskResultResponse.Item(
                                item.itemIndex(),
                                item.itemType(),
                                item.itemName(),
                                item.note(),
                                metrics));
                    }
                    SystemExerciseLookupResult lookup = exerciseMap.get(exercise.exerciseId());
                    exercises.add(new TemplateGenerationTaskResultResponse.Exercise(
                            exercise.sortOrder(),
                            exercise.exerciseId(),
                            lookup == null ? null : lookup.name(),
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
        return new TemplateGenerationTaskResultResponse(
                new TemplateGenerationTaskResultResponse.DraftTemplate(
                        template.getId(),
                        template.getName(),
                        template.getStatus(),
                        template.getCycleLength(),
                        dayResponses),
                generationRationale);
    }

    private String metricUnit(String metricKey) {
        MetricKey key = MetricKey.fromValue(metricKey);
        return key == null ? null : key.getUnit();
    }

    private AiTaskRecordEntity requireTask(Long taskId, String taskType) {
        AiTaskRecordEntity task = taskMapper.selectByIdForUpdate(taskId);
        if (task == null || !taskType.equals(task.getTaskType())) {
            throw new BusinessException(ErrorCode.AI_TASK_NOT_FOUND);
        }
        if (!"running".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "ai task is not running");
        }
        return task;
    }

    private String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "failed to serialize ai payload");
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "failed to parse ai task request payload");
        }
    }
}
