package com.dailyforge.modules.aicoach.infrastructure.ai.tool;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskToolCallEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskRecordMapper;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskToolCallMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AiToolDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AiToolDispatcher.class);

    private final AiToolRegistry aiToolRegistry;
    private final AiTaskToolCallMapper aiTaskToolCallMapper;
    private final AiTaskRecordMapper aiTaskRecordMapper;
    private final ObjectMapper objectMapper;

    public AiToolDispatcher(
            AiToolRegistry aiToolRegistry,
            AiTaskToolCallMapper aiTaskToolCallMapper,
            AiTaskRecordMapper aiTaskRecordMapper,
            ObjectMapper objectMapper) {
        this.aiToolRegistry = aiToolRegistry;
        this.aiTaskToolCallMapper = aiTaskToolCallMapper;
        this.aiTaskRecordMapper = aiTaskRecordMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String dispatch(Long taskId, String taskType, Long userId, int roundNo, String toolName, JsonNode arguments) {
        LocalDateTime startedAt = LocalDateTime.now();
        AiTaskToolCallEntity entity = new AiTaskToolCallEntity();
        entity.setTaskId(taskId);
        entity.setRoundNo(roundNo);
        entity.setToolName(toolName);
        entity.setRequestSummaryJson(write(arguments));
        try {
            Object result = aiToolRegistry.execute(taskType, toolName, userId, arguments);
            String resultJson = write(result);
            entity.setStatus("succeeded");
            entity.setResponseSummaryJson(resultJson);
            entity.setLatencyMs((int) Duration.between(startedAt, LocalDateTime.now()).toMillis());
            entity.setCreatedAt(startedAt);
            aiTaskToolCallMapper.insert(entity);
            aiTaskRecordMapper.incrementToolCallCount(taskId);
            return resultJson;
        } catch (BusinessException exception) {
            log.error("AI tool dispatch failed. taskId={}, taskType={}, toolName={}, roundNo={}",
                    taskId, taskType, toolName, roundNo, exception);
            entity.setStatus("failed");
            entity.setErrorMessage(trim(exception.getMessage()));
            entity.setLatencyMs((int) Duration.between(startedAt, LocalDateTime.now()).toMillis());
            entity.setCreatedAt(startedAt);
            aiTaskToolCallMapper.insert(entity);
            aiTaskRecordMapper.incrementToolCallCount(taskId);
            throw exception;
        } catch (Exception exception) {
            log.error("AI tool dispatch failed unexpectedly. taskId={}, taskType={}, toolName={}, roundNo={}",
                    taskId, taskType, toolName, roundNo, exception);
            entity.setStatus("failed");
            entity.setErrorMessage(trim(exception.getMessage()));
            entity.setLatencyMs((int) Duration.between(startedAt, LocalDateTime.now()).toMillis());
            entity.setCreatedAt(startedAt);
            aiTaskToolCallMapper.insert(entity);
            aiTaskRecordMapper.incrementToolCallCount(taskId);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "failed to serialize ai tool payload");
        }
    }

    private String trim(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
