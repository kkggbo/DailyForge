package com.dailyforge.modules.aicoach.infrastructure.ai.executor;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.application.service.AiCycleSummaryService;
import com.dailyforge.modules.aicoach.domain.model.CycleSummaryValidatedResult;
import com.dailyforge.modules.aicoach.domain.service.AiOutputValidationDomainService;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiCoachProperties;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiConversationService;
import com.dailyforge.modules.aicoach.infrastructure.ai.AiJsonRepairService;
import com.dailyforge.modules.aicoach.infrastructure.ai.context.CycleSummaryContext;
import com.dailyforge.modules.aicoach.infrastructure.ai.context.CycleSummaryContextBuilder;
import com.dailyforge.modules.aicoach.infrastructure.ai.prompt.CycleSummaryPromptBuilder;
import com.dailyforge.modules.aicoach.infrastructure.ai.tool.AiToolRegistry;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.interfaces.dto.CycleSummaryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CycleSummaryExecutor implements AiScenarioExecutor {

    private final ObjectMapper objectMapper;
    private final AiCoachProperties aiCoachProperties;
    private final CycleSummaryContextBuilder contextBuilder;
    private final CycleSummaryPromptBuilder promptBuilder;
    private final AiToolRegistry aiToolRegistry;
    private final AiConversationService aiConversationService;
    private final AiOutputValidationDomainService aiOutputValidationDomainService;
    private final AiJsonRepairService aiJsonRepairService;
    private final AiCycleSummaryService aiCycleSummaryService;

    public CycleSummaryExecutor(
            ObjectMapper objectMapper,
            AiCoachProperties aiCoachProperties,
            CycleSummaryContextBuilder contextBuilder,
            CycleSummaryPromptBuilder promptBuilder,
            AiToolRegistry aiToolRegistry,
            AiConversationService aiConversationService,
            AiOutputValidationDomainService aiOutputValidationDomainService,
            AiJsonRepairService aiJsonRepairService,
            AiCycleSummaryService aiCycleSummaryService) {
        this.objectMapper = objectMapper;
        this.aiCoachProperties = aiCoachProperties;
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.aiToolRegistry = aiToolRegistry;
        this.aiConversationService = aiConversationService;
        this.aiOutputValidationDomainService = aiOutputValidationDomainService;
        this.aiJsonRepairService = aiJsonRepairService;
        this.aiCycleSummaryService = aiCycleSummaryService;
    }

    @Override
    public String taskType() {
        return "cycle_summary";
    }

    @Override
    public void execute(AiTaskRecordEntity task) {
        CycleSummaryRequest request = read(task.getRequestPayloadJson(), CycleSummaryRequest.class);
        Long cycleRunId = task.getRelatedEntityId() != null ? task.getRelatedEntityId() : request.cycleRunId();
        CycleSummaryContext context = contextBuilder.build(task.getUserId(), request, cycleRunId);
        String currentJson = aiConversationService.generateJson(
                task,
                promptBuilder.buildSystemPrompt(task.getPromptVersion()),
                promptBuilder.buildUserPrompt(context),
                aiToolRegistry.getToolDefinitions(task.getTaskType()),
                true);
        CycleSummaryValidatedResult validatedResult = validateWithRepair(task, currentJson);
        aiCycleSummaryService.persistSuccessfulResult(
                task.getId(),
                write(context),
                validatedResult);
    }

    private CycleSummaryValidatedResult validateWithRepair(AiTaskRecordEntity task, String initialJson) {
        String currentJson = initialJson;
        BusinessException lastException = null;
        for (int attempt = 0; attempt <= aiCoachProperties.getMaxRepairAttempts(); attempt++) {
            try {
                return aiOutputValidationDomainService.validateCycleSummary(currentJson);
            } catch (BusinessException exception) {
                lastException = exception;
                if (exception.getErrorCode() != ErrorCode.AI_OUTPUT_INVALID
                        || attempt == aiCoachProperties.getMaxRepairAttempts()) {
                    throw exception;
                }
                currentJson = aiJsonRepairService.repair(
                        task,
                        aiOutputValidationDomainService.cycleSummarySchemaDescription(),
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
