package com.dailyforge.modules.aicoach.application.assembler;

import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskToolCallEntity;
import com.dailyforge.modules.aicoach.interfaces.vo.AiAsyncTaskAcceptedResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.AiTaskDetailResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.AiTaskLatestToolCallResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.CycleSummaryHistoryItemResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.CycleSummaryTaskResultResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationRequestSnapshotResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationHistoryItemResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationTaskResultResponse;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AiCoachAssembler {

    private static final Map<String, String> TOOL_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("get_user_profile_context", "获取用户档案"),
            Map.entry("get_user_current_body_metrics_context", "获取当前身体指标"),
            Map.entry("get_template_generation_constraints", "获取模板生成约束"),
            Map.entry("search_candidate_exercises", "搜索候选动作"),
            Map.entry("get_exercise_detail", "获取动作详情"),
            Map.entry("get_cycle_run_aggregated_analysis", "获取周期执行聚合分析"));

    public AiAsyncTaskAcceptedResponse toAcceptedResponse(AiTaskRecordEntity entity) {
        return new AiAsyncTaskAcceptedResponse(
                entity.getId(),
                entity.getTaskType(),
                entity.getStatus(),
                entity.getCreatedAt(),
                2);
    }

    public <T> AiTaskDetailResponse<T> toTaskDetailResponse(
            AiTaskRecordEntity entity,
            AiTaskToolCallEntity latestToolCall,
            TemplateGenerationRequest requestSnapshot,
            T result) {
        return new AiTaskDetailResponse<>(
                entity.getId(),
                entity.getTaskType(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                resolveProgressStage(entity, latestToolCall),
                toLatestToolCallResponse(latestToolCall),
                toTemplateGenerationRequestSnapshot(requestSnapshot),
                resolveUpdatedAt(entity),
                result);
    }

    public TemplateGenerationHistoryItemResponse toTemplateGenerationHistoryItem(
            AiTaskRecordEntity entity,
            AiTaskToolCallEntity latestToolCall,
            TemplateGenerationRequest request,
            TemplateGenerationTaskResultResponse result,
            String summaryText) {
        TemplateGenerationTaskResultResponse.DraftTemplate draftTemplate = result == null ? null : result.draftTemplate();
        return new TemplateGenerationHistoryItemResponse(
                entity.getId(),
                entity.getTaskType(),
                entity.getStatus(),
                resolveProgressStage(entity, latestToolCall),
                request == null ? null : request.sceneType(),
                request == null ? null : request.goalType(),
                request == null ? null : request.cycleLength(),
                request == null ? null : request.includeCardio(),
                request == null ? null : request.additionalRequirements(),
                draftTemplate == null ? null : draftTemplate.templateId(),
                draftTemplate == null ? null : draftTemplate.templateName(),
                summaryText,
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                resolveUpdatedAt(entity));
    }

    public CycleSummaryHistoryItemResponse toCycleSummaryHistoryItem(
            AiTaskRecordEntity entity,
            AiTaskToolCallEntity latestToolCall,
            Long cycleRunId,
            CycleSummaryTaskResultResponse result,
            String summaryText) {
        return new CycleSummaryHistoryItemResponse(
                entity.getId(),
                entity.getTaskType(),
                entity.getStatus(),
                resolveProgressStage(entity, latestToolCall),
                cycleRunId,
                result == null ? null : result.templateId(),
                result == null ? null : result.templateName(),
                result == null ? null : result.runNo(),
                result == null ? null : result.cycleLength(),
                summaryText,
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                resolveUpdatedAt(entity));
    }

    private AiTaskLatestToolCallResponse toLatestToolCallResponse(AiTaskToolCallEntity latestToolCall) {
        if (latestToolCall == null) {
            return null;
        }
        return new AiTaskLatestToolCallResponse(
                latestToolCall.getRoundNo(),
                latestToolCall.getToolName(),
                TOOL_DISPLAY_NAMES.get(latestToolCall.getToolName()),
                latestToolCall.getStatus(),
                latestToolCall.getCreatedAt());
    }

    private TemplateGenerationRequestSnapshotResponse toTemplateGenerationRequestSnapshot(
            TemplateGenerationRequest requestSnapshot) {
        if (requestSnapshot == null) {
            return null;
        }
        return new TemplateGenerationRequestSnapshotResponse(
                requestSnapshot.sceneType(),
                requestSnapshot.goalType(),
                requestSnapshot.cycleLength(),
                requestSnapshot.includeCardio(),
                requestSnapshot.additionalRequirements());
    }

    private String resolveProgressStage(AiTaskRecordEntity entity, AiTaskToolCallEntity latestToolCall) {
        if (entity == null || entity.getStatus() == null) {
            return null;
        }
        return switch (entity.getStatus()) {
            case "pending" -> "queued";
            case "running" -> resolveRunningProgressStage(entity, latestToolCall);
            case "succeeded" -> "completed";
            case "failed" -> "failed";
            default -> null;
        };
    }

    private String resolveRunningProgressStage(AiTaskRecordEntity entity, AiTaskToolCallEntity latestToolCall) {
        if (entity.getRepairAttemptCount() != null && entity.getRepairAttemptCount() > 0) {
            return "repairing_output";
        }
        if (latestToolCall != null) {
            return "calling_tool";
        }
        return "generating_result";
    }

    private LocalDateTime resolveUpdatedAt(AiTaskRecordEntity entity) {
        if (entity.getUpdatedAt() != null) {
            return entity.getUpdatedAt();
        }
        if (entity.getCompletedAt() != null) {
            return entity.getCompletedAt();
        }
        if (entity.getStartedAt() != null) {
            return entity.getStartedAt();
        }
        return entity.getCreatedAt();
    }
}
