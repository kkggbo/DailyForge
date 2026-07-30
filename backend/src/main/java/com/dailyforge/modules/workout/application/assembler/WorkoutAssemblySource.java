package com.dailyforge.modules.workout.application.assembler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Transport-independent projections consumed by {@link WorkoutAssembler}.
 *
 * <p>Workout persistence entities are introduced by the data-layer implementation. Application services
 * construct these projections after loading entities and template snapshots, keeping that persistence model
 * out of the interface layer.</p>
 */
public final class WorkoutAssemblySource {

    private WorkoutAssemblySource() {
    }

    public record WorkspaceContext(
            String workspaceState,
            Long templateId,
            String templateName,
            Long cycleRunId,
            Integer runNo,
            Integer cycleLength,
            Integer currentDayIndex,
            Integer defaultDayIndex,
            List<DayNavigation> days) {
    }

    public record DayNavigation(
            Integer dayIndex,
            String dayName,
            Boolean isRestDay,
            String dayState,
            Long sessionId,
            String sessionStatus) {
    }

    public record DayDetail(
            Long cycleRunId,
            Integer runNo,
            Long templateId,
            String templateName,
            Integer dayIndex,
            String dayName,
            Boolean isRestDay,
            String dayState,
            String viewMode,
            Boolean canInitializeSession,
            Session session,
            List<Exercise> previewExercises) {
    }

    public record Session(
            Long sessionId,
            String sessionType,
            String sessionStatus,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            String notes,
            List<Exercise> exercises) {
    }

    public record Exercise(
            Long sessionExerciseId,
            Integer sortOrder,
            Long exerciseId,
            String exerciseName,
            String structureType,
            String exerciseStatus,
            String failureReason,
            String feedback,
            List<ExerciseItem> items) {
    }

    public record ExerciseItem(
            Integer itemIndex,
            String itemType,
            String itemName,
            String note,
            List<ExerciseMetric> metrics) {
    }

    public record ExerciseMetric(
            Integer sortOrder,
            String metricKey,
            BigDecimal plannedValueNumber,
            BigDecimal actualValueNumber) {
    }

    public record SessionDetail(
            Long sessionId,
            String sessionType,
            String sessionStatus,
            Long cycleRunId,
            Integer runNo,
            Long templateId,
            String templateName,
            Integer dayIndex,
            String dayName,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            String notes,
            List<Exercise> exercises) {
    }

    public record RecentSession(
            Long sessionId,
            String sessionType,
            String sessionStatus,
            Long templateId,
            String templateName,
            Long cycleRunId,
            Integer runNo,
            Integer dayIndex,
            String dayName,
            LocalDateTime startedAt,
            LocalDateTime completedAt) {
    }

    public record SaveResult(Long sessionId, String sessionStatus, LocalDateTime savedAt) {
    }

    public record DaySummary(Integer dayIndex, String dayName, Boolean isRestDay) {
    }

    public record CompletionResult(
            Long sessionId,
            String sessionStatus,
            LocalDateTime completedAt,
            Integer completedDayIndex,
            Long cycleRunId,
            String cycleRunStatus,
            Integer nextCurrentDayIndex,
            DaySummary nextDay,
            DayDetail completedDay) {
    }

    public record RestartResult(
            Long templateId,
            String templateName,
            Long cycleRunId,
            Integer runNo,
            String cycleRunStatus,
            Integer currentDayIndex) {
    }
}
