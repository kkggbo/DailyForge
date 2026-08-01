package com.dailyforge.modules.aicoach.infrastructure.ai.client;

import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiModelRequest;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.AiModelResponse;

public interface AiModelClient {

    AiModelResponse generate(AiModelRequest request);
}
