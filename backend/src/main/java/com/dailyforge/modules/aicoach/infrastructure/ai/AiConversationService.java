package com.dailyforge.modules.aicoach.infrastructure.ai;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.infrastructure.ai.client.AiModelClient;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiChatMessage;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiModelRequest;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiModelResponse;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiToolCall;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiToolDefinition;
import com.dailyforge.modules.aicoach.infrastructure.ai.tool.AiToolDispatcher;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiConversationService {

    private final AiModelClient aiModelClient;
    private final AiCoachProperties aiCoachProperties;
    private final AiToolDispatcher aiToolDispatcher;
    private final ObjectMapper objectMapper;

    public AiConversationService(
            AiModelClient aiModelClient,
            AiCoachProperties aiCoachProperties,
            AiToolDispatcher aiToolDispatcher,
            ObjectMapper objectMapper) {
        this.aiModelClient = aiModelClient;
        this.aiCoachProperties = aiCoachProperties;
        this.aiToolDispatcher = aiToolDispatcher;
        this.objectMapper = objectMapper;
    }

    public String generateJson(
            AiTaskRecordEntity task,
            String systemPrompt,
            String userPrompt,
            List<AiToolDefinition> tools,
            boolean requireToolCall) {
        List<AiChatMessage> messages = new ArrayList<>();
        messages.add(AiChatMessage.system(systemPrompt));
        messages.add(AiChatMessage.user(userPrompt));
        boolean toolCallObserved = false;
        int finalPromptAttempts = 0;
        while (finalPromptAttempts <= aiCoachProperties.getMaxToolRounds()) {
            AiModelResponse response = aiModelClient.generate(new AiModelRequest(
                    task.getId(),
                    task.getTaskType(),
                    toolCallObserved ? "tool-followup" : "initial-generation",
                    task.getModel(),
                    aiCoachProperties.getTimeout(),
                    List.copyOf(messages),
                    tools));
            if (response.hasToolCalls()) {
                toolCallObserved = true;
                finalPromptAttempts++;
                messages.add(AiChatMessage.assistantWithToolCalls(response.toolCalls()));
                for (AiToolCall toolCall : response.toolCalls()) {
                    JsonNode arguments = parseArguments(toolCall.argumentsJson());
                    String toolResult = aiToolDispatcher.dispatch(
                            task.getId(),
                            task.getTaskType(),
                            task.getUserId(),
                            finalPromptAttempts,
                            toolCall.name(),
                            arguments);
                    messages.add(AiChatMessage.tool(toolCall.id(), toolResult));
                }
                continue;
            }
            if (requireToolCall && !toolCallObserved) {
                finalPromptAttempts++;
                messages.add(AiChatMessage.user(
                        "You must call at least one approved tool before returning the final JSON. Use the tool that best verifies your answer."));
                continue;
            }
            if (!StringUtils.hasText(response.content())) {
                throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "ai response content is empty");
            }
            return response.content();
        }
        throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "ai tool calling exceeded max rounds");
    }

    private JsonNode parseArguments(String argumentsJson) {
        try {
            return objectMapper.readTree(argumentsJson);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "ai tool arguments are invalid");
        }
    }
}
