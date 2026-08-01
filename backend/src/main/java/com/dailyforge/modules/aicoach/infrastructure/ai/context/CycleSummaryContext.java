package com.dailyforge.modules.aicoach.infrastructure.ai.context;

import com.dailyforge.modules.aicoach.interfaces.dto.CycleSummaryRequest;
import java.util.Map;

public record CycleSummaryContext(
        Long userId,
        Long cycleRunId,
        CycleSummaryRequest request,
        Map<String, Object> userProfile,
        Map<String, Object> currentBodyMetrics,
        Map<String, Object> cycleRunSummary,
        Map<String, Object> cycleRunAggregatedAnalysis,
        Map<String, Object> versionSnapshot) {
}
