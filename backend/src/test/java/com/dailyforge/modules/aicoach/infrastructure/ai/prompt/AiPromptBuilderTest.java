package com.dailyforge.modules.aicoach.infrastructure.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.dailyforge.modules.aicoach.infrastructure.ai.context.CycleSummaryContext;
import com.dailyforge.modules.aicoach.infrastructure.ai.context.TemplateGenerationContext;
import com.dailyforge.modules.aicoach.interfaces.dto.CycleSummaryRequest;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiPromptBuilderTest {

    private TemplateGenerationPromptBuilder templateGenerationPromptBuilder;
    private CycleSummaryPromptBuilder cycleSummaryPromptBuilder;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        templateGenerationPromptBuilder = new TemplateGenerationPromptBuilder(objectMapper);
        cycleSummaryPromptBuilder = new CycleSummaryPromptBuilder(objectMapper);
    }

    @Test
    void templateGenerationPromptShouldRequireChineseOutputAndCarryAdditionalRequirements() {
        // Given
        TemplateGenerationContext context = new TemplateGenerationContext(
                101L,
                new TemplateGenerationRequest(
                        "req-1",
                        "gym",
                        "muscle_gain",
                        4,
                        true,
                        "每周至少保留 1 天完整休息，避免大重量深蹲。"),
                Map.of("goalType", "muscle_gain"),
                Map.of("currentWeightKg", 75.5),
                Map.of("available", false),
                Map.of("sceneTypes", List.of("gym", "home")));

        // When
        String systemPrompt = templateGenerationPromptBuilder.buildSystemPrompt("template_generation_v2");
        String userPrompt = templateGenerationPromptBuilder.buildUserPrompt(context);

        // Then
        assertThat(systemPrompt).contains("Simplified Chinese");
        assertThat(userPrompt).contains("All user-facing text fields must be written in Simplified Chinese.");
        assertThat(userPrompt).contains("Rest days are allowed.");
        assertThat(userPrompt).contains("request.additionalRequirements");
        assertThat(userPrompt).contains("每周至少保留 1 天完整休息，避免大重量深蹲。");
    }

    @Test
    void cycleSummaryPromptShouldRequireChineseOutput() {
        // Given
        CycleSummaryContext context = new CycleSummaryContext(
                101L,
                1201L,
                new CycleSummaryRequest("req-2", 1201L),
                Map.of("trainingLevel", "beginner"),
                Map.of("currentWeightKg", 75.5),
                Map.of("cycleRunId", 1201L),
                Map.of("feedbackTexts", List.of("腿部疲劳明显")),
                Map.of("days", List.of()));

        // When
        String systemPrompt = cycleSummaryPromptBuilder.buildSystemPrompt("cycle_summary_v2");
        String userPrompt = cycleSummaryPromptBuilder.buildUserPrompt(context);

        // Then
        assertThat(systemPrompt).contains("Simplified Chinese");
        assertThat(userPrompt).contains("All user-facing text fields must be written in Simplified Chinese.");
        assertThat(userPrompt).contains("\"cycleRunId\" : 1201");
    }
}
