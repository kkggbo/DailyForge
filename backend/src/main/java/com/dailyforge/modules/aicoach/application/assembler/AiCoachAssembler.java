package com.dailyforge.modules.aicoach.application.assembler;

import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.interfaces.vo.AiAsyncTaskAcceptedResponse;
import com.dailyforge.modules.aicoach.interfaces.vo.AiTaskDetailResponse;
import org.springframework.stereotype.Component;

@Component
public class AiCoachAssembler {

    public AiAsyncTaskAcceptedResponse toAcceptedResponse(AiTaskRecordEntity entity) {
        return new AiAsyncTaskAcceptedResponse(
                entity.getId(),
                entity.getTaskType(),
                entity.getStatus(),
                entity.getCreatedAt(),
                2);
    }

    public <T> AiTaskDetailResponse<T> toTaskDetailResponse(AiTaskRecordEntity entity, T result) {
        return new AiTaskDetailResponse<>(
                entity.getId(),
                entity.getTaskType(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                result);
    }
}