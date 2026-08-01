package com.dailyforge.modules.aicoach.config;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.application.service.AiCoachToolSupportService;
import com.dailyforge.modules.aicoach.infrastructure.ai.tool.AiToolHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiCoachToolConfig {

    @Bean
    public AiToolHandler getUserProfileContextTool(AiCoachToolSupportService toolSupportService, ObjectMapper objectMapper) {
        return new SimpleAiToolHandler(
                "get_user_profile_context",
                "Return the current user's AI-relevant profile fields such as gender, birth date, height, goal and training level.",
                emptySchema(objectMapper),
                (userId, arguments) -> toolSupportService.getUserProfileContext(userId));
    }

    @Bean
    public AiToolHandler getUserCurrentBodyMetricsContextTool(
            AiCoachToolSupportService toolSupportService,
            ObjectMapper objectMapper) {
        return new SimpleAiToolHandler(
                "get_user_current_body_metrics_context",
                "Return the current user's latest body metrics such as weight, body fat and waist-related measurements.",
                emptySchema(objectMapper),
                (userId, arguments) -> toolSupportService.getUserCurrentBodyMetricsContext(userId));
    }

    @Bean
    public AiToolHandler getTemplateGenerationConstraintsTool(
            AiCoachToolSupportService toolSupportService,
            ObjectMapper objectMapper) {
        return new SimpleAiToolHandler(
                "get_template_generation_constraints",
                "Return system constraints for AI template generation, including allowed structure types, metric keys and cycle length limits.",
                emptySchema(objectMapper),
                (userId, arguments) -> toolSupportService.getTemplateGenerationConstraints());
    }

    @Bean
    public AiToolHandler searchCandidateExercisesTool(
            AiCoachToolSupportService toolSupportService,
            ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("sceneType").put("type", "string");
        properties.putObject("keyword").put("type", "string");
        properties.putObject("movementType").put("type", "string");
        properties.putObject("structureType").put("type", "string");
        properties.putObject("limit").put("type", "integer");
        return new SimpleAiToolHandler(
                "search_candidate_exercises",
                "Search active system exercises by scene type, movement type, structure type or keyword and return compact candidates.",
                schema,
                (userId, arguments) -> toolSupportService.searchCandidateExercises(
                        text(arguments, "sceneType"),
                        text(arguments, "keyword"),
                        text(arguments, "movementType"),
                        text(arguments, "structureType"),
                        integer(arguments, "limit")));
    }

    @Bean
    public AiToolHandler getExerciseDetailTool(AiCoachToolSupportService toolSupportService, ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("exerciseId").put("type", "integer");
        ArrayNode required = schema.putArray("required");
        required.add("exerciseId");
        return new SimpleAiToolHandler(
                "get_exercise_detail",
                "Return full detail for one active system exercise, including default unit and structure metadata.",
                schema,
                (userId, arguments) -> toolSupportService.getExerciseDetail(longValue(arguments, "exerciseId")));
    }

    @Bean
    public AiToolHandler getCycleRunSummaryTool(AiCoachToolSupportService toolSupportService, ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("cycleRunId").put("type", "integer");
        schema.putArray("required").add("cycleRunId");
        return new SimpleAiToolHandler(
                "get_cycle_run_summary",
                "Return high-level summary for one cycle run owned by the current user.",
                schema,
                (userId, arguments) -> toolSupportService.getCycleRunSummary(userId, longValue(arguments, "cycleRunId")));
    }

    @Bean
    public AiToolHandler getCycleRunSessionsDetailTool(
            AiCoachToolSupportService toolSupportService,
            ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("cycleRunId").put("type", "integer");
        schema.putArray("required").add("cycleRunId");
        return new SimpleAiToolHandler(
                "get_cycle_run_sessions_detail",
                "Return full nested training-session detail for one cycle run owned by the current user.",
                schema,
                (userId, arguments) -> toolSupportService.getCycleRunSessionsDetail(
                        userId,
                        longValue(arguments, "cycleRunId")));
    }

    @Bean
    public AiToolHandler getCycleRunAggregatedAnalysisTool(
            AiCoachToolSupportService toolSupportService,
            ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("cycleRunId").put("type", "integer");
        schema.putArray("required").add("cycleRunId");
        return new SimpleAiToolHandler(
                "get_cycle_run_aggregated_analysis",
                "Return aggregated execution statistics, failure reasons and pain signals for one cycle run owned by the current user.",
                schema,
                (userId, arguments) -> toolSupportService.getCycleRunAggregatedAnalysis(
                        userId,
                        longValue(arguments, "cycleRunId")));
    }

    private static JsonNode emptySchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        return schema;
    }

    private static String text(JsonNode arguments, String fieldName) {
        JsonNode value = arguments.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static Integer integer(JsonNode arguments, String fieldName) {
        JsonNode value = arguments.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private static Long longValue(JsonNode arguments, String fieldName) {
        JsonNode value = arguments.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, fieldName + " is required");
        }
        return value.asLong();
    }

    private record SimpleAiToolHandler(
            String name,
            String description,
            JsonNode parametersSchema,
            ToolExecutor executor) implements AiToolHandler {

        @Override
        public Object execute(Long userId, JsonNode arguments) {
            return executor.execute(userId, arguments);
        }
    }

    @FunctionalInterface
    private interface ToolExecutor {
        Object execute(Long userId, JsonNode arguments);
    }
}
