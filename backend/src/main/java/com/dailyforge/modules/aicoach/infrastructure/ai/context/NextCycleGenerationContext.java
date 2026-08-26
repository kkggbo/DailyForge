package com.dailyforge.modules.aicoach.infrastructure.ai.context;

import com.dailyforge.modules.aicoach.interfaces.dto.NextCycleGenerationRequest;
import java.util.Map;

public record NextCycleGenerationContext(
        Long userId,
        Long sourceCycleRunId,
        NextCycleGenerationRequest request,
        Map<String, Object> userProfile,
        Map<String, Object> currentBodyMetrics,
        Map<String, Object> previousCycleSummary,
        Map<String, Object> previousCycleRunAggregated,
        Map<String, Object> previousCycleRunSessions,
        Map<String, Object> previousVersionSnapshot,
        Map<String, Object> templateConstraints) {
}
