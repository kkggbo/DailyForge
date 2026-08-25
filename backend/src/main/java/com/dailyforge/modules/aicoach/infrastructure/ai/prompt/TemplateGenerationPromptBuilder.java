package com.dailyforge.modules.aicoach.infrastructure.ai.prompt;

import com.dailyforge.modules.aicoach.infrastructure.ai.context.TemplateGenerationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class TemplateGenerationPromptBuilder {

    private final ObjectMapper objectMapper;

    public TemplateGenerationPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildSystemPrompt(String promptVersion) {
        return """
                You are DailyForge's structured fitness-planning assistant.
                Work only within the provided system constraints and approved tools.
                You are not allowed to output medical diagnosis or act like a clinician.
                You must return valid JSON only.
                You must create a draft plan only, never an active template.
                All user-facing text fields in the final JSON must use Simplified Chinese by default.
                Keep enum codes, metric keys, tool names, and other system identifiers unchanged.
                Use tools when you need exercise candidates or metadata.
                Prompt version: %s
                """.formatted(promptVersion);
    }

    public String buildUserPrompt(TemplateGenerationContext context) {
        return """
                Task: produce one AI training-template draft blueprint for the current user.

                Hard rules:
                1. The cycleLength in your output must equal the request cycleLength exactly.
                2. Use only exercise IDs that come from approved tools.
                3. structureType must match the system exercise defaultStructureType.
                4. set_based exercises may only use itemType=set. single_segment exercises may only use itemType=segment.
                5. Only output the JSON schema described below.
                6. Signal priority for choosing weights, sets and reps, highest to lowest:
                   (a) request.additionalRequirements - the user's explicit intent for this template. Honor it strictly.
                   (b) userProfile.injuryNotes - a standing health constraint; treat it as a hard limit and never plan exercises or loads that contradict it. If (a) conflicts with (b), the injury constraint wins and explain this in warnings.
                   (c) recentWorkout - historical performance, a reference only. Use it to set a realistic starting load only when neither (a) nor (b) specifies intensity, and never use it to raise the load beyond an explicitly requested lower intensity (for recovery, deload or avoidance intent it acts only as an upper-bound cap).
                   (d) profile and request defaults.
                7. intensityRationale.basisType: set it to historical_performance only when recentWorkout.available is true AND the loads were actually derived from the user's recent performance (that is, neither (a) nor (b) overrode the intensity). In every other case set it to starting_recommendation, and let intensityRationale.summary explain the real driver (for example, that the loads follow the user's injury or explicit request).
                8. Keep warnings honest and concise.
                9. All user-facing text fields must be written in Simplified Chinese.
                10. Rest days are allowed. Represent a rest day with an empty exercises array instead of inventing unsupported structures.

                Required output JSON schema:
                {
                  "templateName": "string",
                  "cycleLength": 1,
                  "days": [
                    {
                      "dayIndex": 1,
                      "dayName": "string",
                      "exercises": [
                        {
                          "sortOrder": 1,
                          "exerciseId": 1001,
                          "structureType": "set_based",
                          "note": "optional string or null",
                          "items": [
                            {
                              "itemIndex": 1,
                              "itemType": "set",
                              "itemName": "string",
                              "note": "optional string or null",
                              "metrics": [
                                {
                                  "sortOrder": 1,
                                  "metricKey": "weight_kg",
                                  "metricValueNumber": 50
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ],
                  "generationRationale": {
                    "overallDesignSummary": "string",
                    "dayRationales": [
                      {
                        "dayIndex": 1,
                        "dayName": "string",
                        "focusSummary": "string",
                        "rationale": "string"
                      }
                    ],
                    "keyExerciseRationales": [
                      {
                        "dayIndex": 1,
                        "exerciseId": 1001,
                        "exerciseName": "string",
                        "rationale": "string"
                      }
                    ],
                    "intensityRationale": {
                      "basisType": "historical_performance or starting_recommendation",
                      "summary": "string"
                    },
                    "warnings": ["string"]
                  }
                }

                Structured context JSON:
                %s
                """.formatted(write(context));
    }

    private String write(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize template generation context", exception);
        }
    }
}
