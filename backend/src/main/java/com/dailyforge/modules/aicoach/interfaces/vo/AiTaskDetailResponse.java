package com.dailyforge.modules.aicoach.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "AI task detail response")
public record AiTaskDetailResponse<T>(
        @Schema(description = "Task id", example = "9001") Long taskId,
        @Schema(description = "Task type", example = "template_generation") String taskType,
        @Schema(description = "Task status", example = "running") String taskStatus,
        @Schema(description = "Created at", example = "2026-07-31T09:30:15") LocalDateTime createdAt,
        @Schema(description = "Started at", example = "2026-07-31T09:30:16") LocalDateTime startedAt,
        @Schema(description = "Completed at", example = "2026-07-31T09:30:20") LocalDateTime completedAt,
        @Schema(description = "Error code", example = "AI_OUTPUT_INVALID") String errorCode,
        @Schema(description = "Error message", example = "ai output cannot be converted") String errorMessage,
        @Schema(description = "Backend-derived progress stage", example = "calling_tool") String progressStage,
        @Schema(description = "Latest tool-call summary") AiTaskLatestToolCallResponse latestToolCall,
        @Schema(description = "Request snapshot for template generation tasks") TemplateGenerationRequestSnapshotResponse requestSnapshot,
        @Schema(description = "Last updated at", example = "2026-07-31T09:30:17") LocalDateTime updatedAt,
        @Schema(description = "Task result") T result) {
}
