package com.dailyforge.modules.aicoach.infrastructure.ai.model;

import java.time.Duration;
import java.util.List;

public record AiModelRequest(
        Long taskId,
        String taskType,
        String stage,
        String model,
        Duration timeout,
        List<AiChatMessage> messages,
        List<AiToolDefinition> tools) {
}
