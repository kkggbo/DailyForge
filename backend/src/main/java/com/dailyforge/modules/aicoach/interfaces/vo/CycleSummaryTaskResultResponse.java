package com.dailyforge.modules.aicoach.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "AI cycle summary task result")
public record CycleSummaryTaskResultResponse(
        @Schema(description = "Cycle run id", example = "1201") Long cycleRunId,
        @Schema(description = "Template id", example = "301") Long templateId,
        @Schema(description = "Template name", example = "Four Day Split") String templateName,
        @Schema(description = "Run number", example = "3") Integer runNo,
        @Schema(description = "Cycle length", example = "4") Integer cycleLength,
        @Schema(description = "Execution overview") String executionOverview,
        @Schema(description = "Strengths") List<String> strengths,
        @Schema(description = "Issues") List<String> issues,
        @Schema(description = "Cause analysis") List<String> causeAnalysis,
        @Schema(description = "Next cycle suggestions") List<String> nextCycleSuggestions,
        @Schema(description = "Risks") List<String> risks,
        @Schema(description = "Optional data completeness notice") String dataCompletenessNotice) {
}