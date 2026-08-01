package com.dailyforge.modules.aicoach.infrastructure.ai.model;

import com.fasterxml.jackson.databind.JsonNode;

public record AiToolDefinition(
        String name,
        String description,
        JsonNode parametersSchema) {
}
