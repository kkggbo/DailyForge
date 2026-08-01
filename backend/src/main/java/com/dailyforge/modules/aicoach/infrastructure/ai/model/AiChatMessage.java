package com.dailyforge.modules.aicoach.infrastructure.ai.model;

import java.util.List;

public record AiChatMessage(
        String role,
        String content,
        String toolCallId,
        List<AiToolCall> toolCalls) {

    public static AiChatMessage system(String content) {
        return new AiChatMessage("system", content, null, List.of());
    }

    public static AiChatMessage user(String content) {
        return new AiChatMessage("user", content, null, List.of());
    }

    public static AiChatMessage assistantWithToolCalls(List<AiToolCall> toolCalls) {
        return new AiChatMessage("assistant", null, null, toolCalls == null ? List.of() : toolCalls);
    }

    public static AiChatMessage tool(String toolCallId, String content) {
        return new AiChatMessage("tool", content, toolCallId, List.of());
    }
}
