package com.dailyforge.modules.aicoach.infrastructure.ai.tool;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiToolRegistry {

    private static final String TEMPLATE = "template_generation";
    private static final String SUMMARY = "cycle_summary";

    private final Map<String, AiToolHandler> handlers;

    public AiToolRegistry(ObjectMapper objectMapper, List<AiToolHandler> handlers) {
        this.handlers = new LinkedHashMap<>();
        for (AiToolHandler handler : handlers) {
            this.handlers.put(handler.name(), handler);
        }
    }

    public List<AiToolDefinition> getToolDefinitions(String taskType) {
        return getAllowedToolNames(taskType).stream()
                .map(this::requireHandler)
                .map(handler -> new AiToolDefinition(handler.name(), handler.description(), handler.parametersSchema()))
                .toList();
    }

    public Object execute(String taskType, String toolName, Long userId, JsonNode arguments) {
        if (!getAllowedToolNames(taskType).contains(toolName)) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "tool is not allowed for current task type");
        }
        return requireHandler(toolName).execute(userId, arguments);
    }

    private List<String> getAllowedToolNames(String taskType) {
        if (TEMPLATE.equals(taskType)) {
            return List.of(
                    "get_user_profile_context",
                    "get_user_current_body_metrics_context",
                    "get_template_generation_constraints",
                    "search_candidate_exercises",
                    "get_exercise_detail");
        }
        if (SUMMARY.equals(taskType)) {
            return List.of(
                    "get_user_profile_context",
                    "get_user_current_body_metrics_context",
                    "get_cycle_run_summary",
                    "get_cycle_run_sessions_detail",
                    "get_cycle_run_aggregated_analysis");
        }
        throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "unsupported ai task type");
    }

    private AiToolHandler requireHandler(String toolName) {
        AiToolHandler handler = handlers.get(toolName);
        if (handler == null) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "unsupported ai tool");
        }
        return handler;
    }
}
