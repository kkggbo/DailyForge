package com.dailyforge.modules.aicoach.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "AI async task accepted response")
public record AiAsyncTaskAcceptedResponse(
        @Schema(description = "Task id", example = "9001") Long taskId,
        @Schema(description = "Task type", example = "template_generation") String taskType,
        @Schema(description = "Task status", example = "pending") String taskStatus,
        @Schema(description = "Created at", example = "2026-07-31T09:30:15") LocalDateTime createdAt,
        @Schema(description = "Recommended polling delay in seconds", example = "2") Integer pollAfterSeconds) {
}