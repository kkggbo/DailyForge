package com.dailyforge.modules.aicoach.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "AI template generation task result")
public record TemplateGenerationTaskResultResponse(
        @Schema(description = "Generated draft template") DraftTemplate draftTemplate,
        @Schema(description = "Generation rationale") GenerationRationale generationRationale) {

    @Schema(description = "Generated draft template")
    public record DraftTemplate(
            @Schema(description = "Template id", example = "501") Long templateId,
            @Schema(description = "Template name", example = "AI Template 2026-07-31 09:30") String templateName,
            @Schema(description = "Template status", example = "draft") String templateStatus,
            @Schema(description = "Cycle length", example = "4") Integer cycleLength,
            @Schema(description = "Template days") List<Day> days) {
    }

    @Schema(description = "Generated template day")
    public record Day(
            @Schema(description = "Day index", example = "1") Integer dayIndex,
            @Schema(description = "Day name", example = "Push") String dayName,
            @Schema(description = "Whether this day is a rest day", example = "false") Boolean isRestDay,
            @Schema(description = "Exercises") List<Exercise> exercises) {
    }

    @Schema(description = "Generated template exercise")
    public record Exercise(
            @Schema(description = "Exercise order", example = "1") Integer sortOrder,
            @Schema(description = "Exercise id", example = "1001") Long exerciseId,
            @Schema(description = "Exercise name", example = "Barbell Bench Press") String exerciseName,
            @Schema(description = "Structure type", example = "set_based") String structureType,
            @Schema(description = "Exercise note") String note,
            @Schema(description = "Items") List<Item> items) {
    }

    @Schema(description = "Generated template item")
    public record Item(
            @Schema(description = "Item index", example = "1") Integer itemIndex,
            @Schema(description = "Item type", example = "set") String itemType,
            @Schema(description = "Item name", example = "Set 1") String itemName,
            @Schema(description = "Item note") String note,
            @Schema(description = "Metrics") List<Metric> metrics) {
    }

    @Schema(description = "Generated template metric")
    public record Metric(
            @Schema(description = "Metric order", example = "1") Integer sortOrder,
            @Schema(description = "Metric key", example = "reps") String metricKey,
            @Schema(description = "Metric numeric value", example = "10") BigDecimal metricValueNumber,
            @Schema(description = "Derived metric unit", example = "count") String metricUnit) {
    }

    @Schema(description = "Generation rationale")
    public record GenerationRationale(
            @Schema(description = "Overall design summary") String overallDesignSummary,
            @Schema(description = "Day rationales") List<DayRationale> dayRationales,
            @Schema(description = "Key exercise rationales") List<KeyExerciseRationale> keyExerciseRationales,
            @Schema(description = "Intensity rationale") IntensityRationale intensityRationale,
            @Schema(description = "Warnings") List<String> warnings) {
    }

    @Schema(description = "Per-day rationale")
    public record DayRationale(
            @Schema(description = "Day index", example = "1") Integer dayIndex,
            @Schema(description = "Day name", example = "Push") String dayName,
            @Schema(description = "Focus summary") String focusSummary,
            @Schema(description = "Rationale") String rationale) {
    }

    @Schema(description = "Key exercise rationale")
    public record KeyExerciseRationale(
            @Schema(description = "Day index", example = "1") Integer dayIndex,
            @Schema(description = "Exercise id", example = "1001") Long exerciseId,
            @Schema(description = "Exercise name", example = "Barbell Bench Press") String exerciseName,
            @Schema(description = "Rationale") String rationale) {
    }

    @Schema(description = "Intensity rationale")
    public record IntensityRationale(
            @Schema(description = "Rationale basis type", example = "starting_recommendation") String basisType,
            @Schema(description = "Summary") String summary) {
    }
}