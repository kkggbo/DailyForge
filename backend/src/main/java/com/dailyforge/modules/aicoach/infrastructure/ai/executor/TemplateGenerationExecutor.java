package com.dailyforge.modules.aicoach.infrastructure.ai.executor;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.application.service.AiTemplateGenerationService;
import com.dailyforge.modules.aicoach.domain.model.TemplateGenerationValidatedResult;
import com.dailyforge.modules.aicoach.domain.service.AiOutputValidationDomainService;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiCoachProperties;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiConversationService;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiJsonRepairService;
import com.dailyforge.modules.aicoach.infrastructure.ai.context.TemplateGenerationContext;
import com.dailyforge.modules.aicoach.infrastructure.ai.context.TemplateGenerationContextBuilder;
import com.dailyforge.modules.aicoach.infrastructure.ai.prompt.TemplateGenerationPromptBuilder;
import com.dailyforge.modules.aicoach.infrastructure.ai.tool.AiToolRegistry;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TemplateGenerationExecutor implements AiScenarioExecutor {

    private final ObjectMapper objectMapper;
    private final AiCoachProperties aiCoachProperties;
    private final TemplateGenerationContextBuilder contextBuilder;
    private final TemplateGenerationPromptBuilder promptBuilder;
    private final AiToolRegistry aiToolRegistry;
    private final AiConversationService aiConversationService;
    private final AiOutputValidationDomainService aiOutputValidationDomainService;
    private final AiJsonRepairService aiJsonRepairService;
    private final AiTemplateGenerationService aiTemplateGenerationService;

    public TemplateGenerationExecutor(
            ObjectMapper objectMapper,
            AiCoachProperties aiCoachProperties,
            TemplateGenerationContextBuilder contextBuilder,
            TemplateGenerationPromptBuilder promptBuilder,
            AiToolRegistry aiToolRegistry,
            AiConversationService aiConversationService,
            AiOutputValidationDomainService aiOutputValidationDomainService,
            AiJsonRepairService aiJsonRepairService,
            AiTemplateGenerationService aiTemplateGenerationService) {
        this.objectMapper = objectMapper;
        this.aiCoachProperties = aiCoachProperties;
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.aiToolRegistry = aiToolRegistry;
        this.aiConversationService = aiConversationService;
        this.aiOutputValidationDomainService = aiOutputValidationDomainService;
        this.aiJsonRepairService = aiJsonRepairService;
        this.aiTemplateGenerationService = aiTemplateGenerationService;
    }

    @Override
    public String taskType() {
        return "template_generation";
    }

    @Override
    public void execute(AiTaskRecordEntity task) {
        TemplateGenerationRequest request = read(task.getRequestPayloadJson(), TemplateGenerationRequest.class);
        TemplateGenerationContext context = contextBuilder.build(task.getUserId(), request);
        String currentJson = aiConversationService.generateJson(
                task,
                promptBuilder.buildSystemPrompt(task.getPromptVersion()),
                promptBuilder.buildUserPrompt(context),
                aiToolRegistry.getToolDefinitions(task.getTaskType()),
                true);
        TemplateGenerationValidatedResult validatedResult = validateWithRepair(task, currentJson, request);
        aiTemplateGenerationService.persistSuccessfulResult(
                task.getId(),
                write(context),
                validatedResult);
    }

    private TemplateGenerationValidatedResult validateWithRepair(
            AiTaskRecordEntity task,
            String initialJson,
            TemplateGenerationRequest request) {
        String currentJson = initialJson;
        BusinessException lastException = null;
        for (int attempt = 0; attempt <= aiCoachProperties.getMaxRepairAttempts(); attempt++) {
            try {
                return aiOutputValidationDomainService.validateTemplateGeneration(currentJson, request);
            } catch (BusinessException exception) {
                lastException = exception;
                if (exception.getErrorCode() != ErrorCode.AI_OUTPUT_INVALID
                        || attempt == aiCoachProperties.getMaxRepairAttempts()) {
                    throw exception;
                }
                currentJson = aiJsonRepairService.repair(
                        task,
                        aiOutputValidationDomainService.templateGenerationSchemaDescription(),
                        currentJson,
                        splitErrors(exception.getMessage()));
            }
        }
        throw lastException == null ? new BusinessException(ErrorCode.AI_OUTPUT_INVALID) : lastException;
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "failed to parse ai task request payload");
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "failed to serialize ai context");
        }
    }

    private List<String> splitErrors(String message) {
        if (message == null || message.isBlank()) {
            return List.of(ErrorCode.AI_OUTPUT_INVALID.getDefaultMessage());
        }
        return Arrays.stream(message.split("; ")).toList();
    }
}
