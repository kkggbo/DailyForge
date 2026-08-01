package com.dailyforge.modules.aicoach.infrastructure.ai.prompt;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiRepairPromptBuilder {

    public String buildSystemPrompt(String promptVersion) {
        return """
                You are repairing an invalid JSON response for DailyForge.
                Return one complete JSON object only.
                Do not explain your changes.
                Prompt version: %s
                """.formatted(promptVersion);
    }

    public String buildUserPrompt(String schemaDescription, String invalidJson, List<String> errors) {
        return """
                The previous JSON response is invalid for the required schema.

                Required schema summary:
                %s

                Validation errors:
                %s

                Invalid JSON to repair:
                %s
                """.formatted(schemaDescription, String.join("\n- ", prependDash(errors)), invalidJson);
    }

    private List<String> prependDash(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return List.of("unknown validation error");
        }
        return errors;
    }
}
