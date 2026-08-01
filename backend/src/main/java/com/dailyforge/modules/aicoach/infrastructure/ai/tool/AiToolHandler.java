package com.dailyforge.modules.aicoach.infrastructure.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;

public interface AiToolHandler {

    String name();

    String description();

    JsonNode parametersSchema();

    Object execute(Long userId, JsonNode arguments);
}
