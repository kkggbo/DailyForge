package com.dailyforge.modules.aicoach.infrastructure.ai.model;

public record AiToolCall(
        String id,
        String name,
        String argumentsJson) {
}
