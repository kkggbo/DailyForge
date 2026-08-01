package com.dailyforge.modules.aicoach.infrastructure.ai;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.infrastructure.ai.client.AiModelClient;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiChatMessage;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiModelRequest;
import com.dailyforge.modules.aicoach.infrastructure.ai.prompt.AiRepairPromptBuilder;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskRecordMapper;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiJsonRepairService {

    private final AiModelClient aiModelClient;
    private final AiCoachProperties aiCoachProperties;
    private final AiRepairPromptBuilder aiRepairPromptBuilder;
    private final AiTaskRecordMapper aiTaskRecordMapper;

    public AiJsonRepairService(
            AiModelClient aiModelClient,
            AiCoachProperties aiCoachProperties,
            AiRepairPromptBuilder aiRepairPromptBuilder,
            AiTaskRecordMapper aiTaskRecordMapper) {
        this.aiModelClient = aiModelClient;
        this.aiCoachProperties = aiCoachProperties;
        this.aiRepairPromptBuilder = aiRepairPromptBuilder;
        this.aiTaskRecordMapper = aiTaskRecordMapper;
    }

    public String repair(
            AiTaskRecordEntity task,
            String schemaDescription,
            String invalidJson,
            List<String> validationErrors) {
        aiTaskRecordMapper.incrementRepairAttemptCount(task.getId());
        String promptVersion = task.getPromptVersion() + "_repair_v1";
        String systemPrompt = aiRepairPromptBuilder.buildSystemPrompt(promptVersion);
        String userPrompt = aiRepairPromptBuilder.buildUserPrompt(schemaDescription, invalidJson, validationErrors);
        String repaired = aiModelClient.generate(new AiModelRequest(
                task.getId(),
                task.getTaskType(),
                "json-repair",
                task.getModel(),
                aiCoachProperties.getTimeout(),
                List.of(AiChatMessage.system(systemPrompt), AiChatMessage.user(userPrompt)),
                List.of())).content();
        if (!StringUtils.hasText(repaired)) {
            throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "ai repair response is empty");
        }
        return repaired;
    }
}
