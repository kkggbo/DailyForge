package com.dailyforge.modules.aicoach.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Template generation request snapshot")
public record TemplateGenerationRequestSnapshotResponse(
        @Schema(description = "Scene type", example = "gym") String sceneType,
        @Schema(description = "Goal type for this generation", example = "muscle_gain") String goalType,
        @Schema(description = "Cycle length", example = "4") Integer cycleLength,
        @Schema(description = "Whether cardio is allowed in this generation", example = "true") Boolean includeCardio,
        @Schema(description = "One-off additional requirements",
                example = "每周至少保留 1 天完整休息，避免大重量深蹲。") String additionalRequirements) {
}
