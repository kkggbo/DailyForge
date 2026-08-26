package com.dailyforge.modules.aicoach.infrastructure.ai.context;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.application.service.AiCoachToolSupportService;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskRecordMapper;
import com.dailyforge.modules.aicoach.interfaces.dto.NextCycleGenerationRequest;
import com.dailyforge.modules.aicoach.interfaces.vo.CycleSummaryTaskResultResponse;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleRunMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NextCycleGenerationContextBuilder {

    private static final String TASK_CYCLE_SUMMARY = "cycle_summary";
    private static final String RELATED_ENTITY_CYCLE_RUN = "cycle_run";

    private final AiCoachToolSupportService aiCoachToolSupportService;
    private final AiTaskRecordMapper aiTaskRecordMapper;
    private final CycleRunMapper cycleRunMapper;
    private final CycleTemplateVersionDomainService cycleTemplateVersionDomainService;
    private final ObjectMapper objectMapper;

    public NextCycleGenerationContextBuilder(
            AiCoachToolSupportService aiCoachToolSupportService,
            AiTaskRecordMapper aiTaskRecordMapper,
            CycleRunMapper cycleRunMapper,
            CycleTemplateVersionDomainService cycleTemplateVersionDomainService,
            ObjectMapper objectMapper) {
        this.aiCoachToolSupportService = aiCoachToolSupportService;
        this.aiTaskRecordMapper = aiTaskRecordMapper;
        this.cycleRunMapper = cycleRunMapper;
        this.cycleTemplateVersionDomainService = cycleTemplateVersionDomainService;
        this.objectMapper = objectMapper;
    }

    public NextCycleGenerationContext build(Long userId, NextCycleGenerationRequest request) {
        Long sourceCycleRunId = request.sourceCycleRunId();
        CycleSummaryTaskResultResponse summary = resolvePreviousCycleSummary(userId, request);
        return new NextCycleGenerationContext(
                userId,
                sourceCycleRunId,
                request,
                aiCoachToolSupportService.getUserProfileContext(userId),
                aiCoachToolSupportService.getUserCurrentBodyMetricsContext(userId),
                toSummaryMap(summary),
                aiCoachToolSupportService.getCycleRunAggregatedAnalysis(userId, sourceCycleRunId),
                aiCoachToolSupportService.getCycleRunSessionsDetail(userId, sourceCycleRunId),
                resolveVersionSnapshot(userId, sourceCycleRunId),
                aiCoachToolSupportService.getTemplateGenerationConstraints());
    }

    private CycleSummaryTaskResultResponse resolvePreviousCycleSummary(
            Long userId,
            NextCycleGenerationRequest request) {
        AiTaskRecordEntity task = null;
        if (request.sourceSummaryTaskId() != null) {
            task = aiTaskRecordMapper.selectByIdAndUserIdAndTaskType(
                    request.sourceSummaryTaskId(), userId, TASK_CYCLE_SUMMARY);
            if (task != null
                    && !request.sourceCycleRunId().equals(task.getRelatedEntityId())) {
                task = null;
            }
        } else {
            task = aiTaskRecordMapper.selectLatestSucceededByUserIdAndTaskTypeAndRelatedEntity(
                    userId, TASK_CYCLE_SUMMARY, RELATED_ENTITY_CYCLE_RUN, request.sourceCycleRunId());
        }
        if (task == null || !"succeeded".equals(task.getStatus()) || !StringUtils.hasText(task.getResultJson())) {
            throw new BusinessException(ErrorCode.AI_CYCLE_SUMMARY_REQUIRED);
        }
        return read(task.getResultJson(), CycleSummaryTaskResultResponse.class);
    }

    private Map<String, Object> toSummaryMap(CycleSummaryTaskResultResponse summary) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("executionOverview", summary.executionOverview());
        result.put("strengths", summary.strengths());
        result.put("issues", summary.issues());
        result.put("causeAnalysis", summary.causeAnalysis());
        result.put("nextCycleSuggestions", summary.nextCycleSuggestions());
        result.put("risks", summary.risks());
        return result;
    }

    private Map<String, Object> resolveVersionSnapshot(Long userId, Long sourceCycleRunId) {
        CycleRunEntity cycleRun = cycleRunMapper.selectById(sourceCycleRunId);
        if (cycleRun == null) {
            return Map.of();
        }
        if (!userId.equals(cycleRun.getUserId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (cycleRun.getTemplateVersionId() == null) {
            return Map.of();
        }
        return aiCoachToolSupportService.versionSnapshotToSummary(
                cycleTemplateVersionDomainService.loadVersionSnapshot(cycleRun.getTemplateVersionId()));
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.AI_CYCLE_SUMMARY_REQUIRED, "failed to parse cycle summary result");
        }
    }
}
