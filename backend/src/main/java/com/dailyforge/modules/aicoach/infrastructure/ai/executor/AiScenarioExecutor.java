package com.dailyforge.modules.aicoach.infrastructure.ai.executor;

import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;

public interface AiScenarioExecutor {

    String taskType();

    void execute(AiTaskRecordEntity task);
}
