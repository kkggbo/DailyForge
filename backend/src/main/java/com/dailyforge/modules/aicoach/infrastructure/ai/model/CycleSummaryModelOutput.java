package com.dailyforge.modules.aicoach.infrastructure.ai.model;

import java.util.List;

public record CycleSummaryModelOutput(
        String executionOverview,
        List<String> strengths,
        List<String> issues,
        List<String> causeAnalysis,
        List<String> nextCycleSuggestions,
        List<String> risks,
        String dataCompletenessNotice) {
}
