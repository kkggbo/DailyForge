package com.dailyforge.modules.aicoach.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "AI template generation history item")
public record TemplateGenerationHistoryItemResponse(
        @Schema(description = "Task id", example = "9001") Long taskId,
        @Schema(description = "Task type", example = "template_generation") String taskType,
        @Schema(description = "Task status", example = "succeeded") String taskStatus,
        @Schema(description = "Progress stage", example = "completed") String progressStage,
        @Schema(description = "Scene type", example = "gym") String sceneType,
        @Schema(description = "Goal type", example = "muscle_gain") String goalType,
        @Schema(description = "Cycle length", example = "4") Integer cycleLength,
        @Schema(description = "Whether cardio is allowed", example = "true") Boolean includeCardio,
        @Schema(description = "One-off additional requirements",
                example = "每周至少保留 1 天完整休息，避免大重量深蹲。") String additionalRequirements,
        @Schema(description = "Generated template id", example = "501") Long templateId,
        @Schema(description = "Generated template name", example = "AI 生成模板 2026-07-31 09:30") String templateName,
        @Schema(description = "History summary text", example = "采用 4 天循环，兼顾训练与恢复。") String summaryText,
        @Schema(description = "Created at", example = "2026-07-31T09:30:15") LocalDateTime createdAt,
        @Schema(description = "Completed at", example = "2026-07-31T09:30:20") LocalDateTime completedAt,
        @Schema(description = "Updated at", example = "2026-07-31T09:30:20") LocalDateTime updatedAt) {
}
