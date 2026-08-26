package com.dailyforge.modules.aicoach.infrastructure.ai.prompt;

import com.dailyforge.modules.aicoach.infrastructure.ai.context.NextCycleGenerationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class NextCycleGenerationPromptBuilder {

    private final ObjectMapper objectMapper;

    public NextCycleGenerationPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildSystemPrompt(String promptVersion) {
        return """
                You are DailyForge's structured fitness-planning assistant for building the NEXT cycle.
                Work only within the provided system constraints and approved tools.
                You are not allowed to output medical diagnosis or act like a clinician.
                You must return valid JSON only.
                You must create a draft plan only, never an active template.
                All user-facing text fields in the final JSON must use Simplified Chinese by default.
                Keep enum codes, metric keys, tool names, and other system identifiers unchanged.
                Use tools when you need exercise candidates, the previous cycle performance or metadata.
                Prompt version: %s
                """.formatted(promptVersion);
    }

    public String buildUserPrompt(NextCycleGenerationContext context) {
        return """
                Task: produce one AI training-template draft blueprint for the NEXT cycle, built on the
                previous cycle's actual performance and the previous cycle's AI summary.

                Hard rules:
                1. The cycleLength in your output must equal the request cycleLength exactly.
                2. Use only exercise IDs that come from approved tools.
                3. structureType must match the system exercise defaultStructureType.
                4. set_based exercises may only use itemType=set. single_segment exercises may only use itemType=segment.
                5. Only output the JSON schema described below.
                6. Signal priority for choosing structure, weights, sets and reps, highest to lowest:
                   (a) request.additionalRequirements - the user's explicit intent for this next cycle. Honor it strictly.
                   (b) userProfile.injuryNotes - a standing health constraint; treat it as a hard limit and never plan exercises or loads that contradict it. If (a) conflicts with (b), the injury constraint wins and explain this in warnings.
                   (c) previousCycleSummary.nextCycleSuggestions - the previous cycle AI recommendations; guide the adjustment direction (e.g. add/remove exercises, change intensity, fix identified issues).
                   (d) previousCycleRun - the previous cycle's actual performance (aggregated analysis + sessions detail + version snapshot). Use it as the progressive baseline and as an upper-bound cap. For a recovery / deload / avoid-a-movement intent, never raise the load toward the previous performance; it only acts as a ceiling.
                   (e) profile and request defaults.
                7. Continue the previous cycle's template structure where it was effective: keep sound exercises and scheduling, adjust problem items, and absorb nextCycleSuggestions.
                8. intensityRationale.basisType: set it to historical_performance only when the loads were actually derived from the previous cycle's performance (that is, neither (a) nor (b) nor (c) overrode the intensity). In every other case set it to starting_recommendation, and let intensityRationale.summary explain the real driver.
                9. Keep warnings honest and concise.
                10. All user-facing text fields must be written in Simplified Chinese.
                11. Rest days are allowed. Represent a rest day with an empty exercises array instead of inventing unsupported structures.
                12. In user-facing prose, when mentioning the training goal (goalType) or scene (sceneType), use their Chinese labels (goalType: 减脂/增肌/保持健康; sceneType: 健身房/居家) and never the raw English enum code (for example health_maintenance). The "keep enum codes unchanged" rule applies only to JSON field values such as metricKey, exerciseId, structureType and itemType, never to prose.

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
            throw new IllegalStateException("failed to serialize next-cycle generation context", exception);
        }
    }
}
