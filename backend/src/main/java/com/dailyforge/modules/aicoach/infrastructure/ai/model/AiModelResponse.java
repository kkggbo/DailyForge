package com.dailyforge.modules.aicoach.infrastructure.ai.model;

import java.util.List;

public record AiModelResponse(
        String content,
        List<AiToolCall> toolCalls,
        String finishReason) {

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
