package com.dailyforge.modules.aicoach.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "AI cycle summary history item")
public record CycleSummaryHistoryItemResponse(
        @Schema(description = "Task id", example = "9101") Long taskId,
        @Schema(description = "Task type", example = "cycle_summary") String taskType,
        @Schema(description = "Task status", example = "succeeded") String taskStatus,
        @Schema(description = "Progress stage", example = "completed") String progressStage,
        @Schema(description = "Cycle run id", example = "1201") Long cycleRunId,
        @Schema(description = "Template id", example = "301") Long templateId,
        @Schema(description = "Template name", example = "四天上/下肢分化") String templateName,
        @Schema(description = "Run number", example = "3") Integer runNo,
        @Schema(description = "Cycle length", example = "4") Integer cycleLength,
        @Schema(description = "History summary text", example = "本轮 4 个 Day 均完成打卡，其中 1 个动作出现部分完成。")
        String summaryText,
        @Schema(description = "Created at", example = "2026-07-31T09:40:10") LocalDateTime createdAt,
        @Schema(description = "Completed at", example = "2026-07-31T09:40:14") LocalDateTime completedAt,
        @Schema(description = "Updated at", example = "2026-07-31T09:40:14") LocalDateTime updatedAt) {
}
