package com.dailyforge.modules.aicoach.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Latest AI tool-call summary")
public record AiTaskLatestToolCallResponse(
        @Schema(description = "Tool round number", example = "1") Integer roundNo,
        @Schema(description = "Tool name", example = "search_candidate_exercises") String toolName,
        @Schema(description = "Tool display name in Chinese", example = "搜索候选动作") String toolDisplayName,
        @Schema(description = "Tool call status", example = "succeeded") String status,
        @Schema(description = "Tool call created at", example = "2026-07-31T09:30:17") LocalDateTime createdAt) {
}
