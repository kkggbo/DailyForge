package com.dailyforge.modules.aicoach.infrastructure.ai.prompt;

import com.dailyforge.modules.aicoach.infrastructure.ai.context.CycleSummaryContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class CycleSummaryPromptBuilder {

    private final ObjectMapper objectMapper;

    public CycleSummaryPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildSystemPrompt(String promptVersion) {
        return """
                You are DailyForge's structured cycle-summary assistant.
                Work only with the provided data and approved tools.
                You may summarize training quality and suggest the next adjustment direction, but you must not generate a new template in this task.
                You must return valid JSON only.
                You are not allowed to output medical diagnosis or certainty beyond the data.
                Prompt version: %s
                """.formatted(promptVersion);
    }

    public String buildUserPrompt(CycleSummaryContext context) {
        return """
                Task: analyze one completed cycle run and produce a structured summary.

                Hard rules:
                1. Do not generate a new template.
                2. Use only the current user's cycle-run data.
                3. Keep every list field concise but non-empty when there is enough evidence.
                4. dataCompletenessNotice may be null when there is no recommended missing field.
                5. Return JSON only.

                Required output JSON schema:
                {
                  "executionOverview": "string",
                  "strengths": ["string"],
                  "issues": ["string"],
                  "causeAnalysis": ["string"],
                  "nextCycleSuggestions": ["string"],
                  "risks": ["string"],
                  "dataCompletenessNotice": "string or null"
                }

                Structured context JSON:
                %s
                """.formatted(write(context));
    }

    private String write(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize cycle summary context", exception);
        }
    }
}
