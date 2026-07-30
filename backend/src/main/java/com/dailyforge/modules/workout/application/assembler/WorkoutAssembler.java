package com.dailyforge.modules.workout.application.assembler;

import com.dailyforge.modules.plan.domain.model.MetricKey;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.ExerciseSnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.ItemSnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.MetricSnapshot;
import com.dailyforge.modules.workout.interfaces.vo.CompleteWorkoutSessionResponse;
import com.dailyforge.modules.workout.interfaces.vo.InitializeCurrentWorkoutSessionResponse;
import com.dailyforge.modules.workout.interfaces.vo.RestartWorkoutCycleResponse;
import com.dailyforge.modules.workout.interfaces.vo.SaveWorkoutSessionResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutDayDetailResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutDayNavigationItemResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutDaySummaryResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutRecentSessionItemResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutRecentSessionPageResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutSessionDetailResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutSessionExerciseItemResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutSessionExerciseMetricResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutSessionExerciseResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutSessionResponse;
import com.dailyforge.modules.workout.interfaces.vo.WorkoutWorkspaceContextResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkoutAssembler {

    default WorkoutWorkspaceContextResponse toWorkspaceContextResponse(
            WorkoutAssemblySource.WorkspaceContext source) {
        return new WorkoutWorkspaceContextResponse(
                source.workspaceState(),
                source.templateId(),
                source.templateName(),
                source.cycleRunId(),
                source.runNo(),
                source.cycleLength(),
                source.currentDayIndex(),
                source.defaultDayIndex(),
                toDayNavigationResponses(source.days()));
    }

    default InitializeCurrentWorkoutSessionResponse toInitializeCurrentSessionResponse(
            boolean sessionCreated,
            WorkoutAssemblySource.DayDetail day) {
        return new InitializeCurrentWorkoutSessionResponse(sessionCreated, toDayDetailResponse(day));
    }

    default WorkoutDayDetailResponse toDayDetailResponse(WorkoutAssemblySource.DayDetail source) {
        if (source == null) {
            return null;
        }
        return new WorkoutDayDetailResponse(
                source.cycleRunId(),
                source.runNo(),
                source.templateId(),
                source.templateName(),
                source.dayIndex(),
                source.dayName(),
                source.isRestDay(),
                source.dayState(),
                source.viewMode(),
                source.canInitializeSession(),
                toSessionResponse(source.session()),
                source.session() == null ? toExerciseResponses(source.previewExercises()) : null);
    }

    default WorkoutSessionResponse toSessionResponse(WorkoutAssemblySource.Session source) {
        if (source == null) {
            return null;
        }
        return new WorkoutSessionResponse(
                source.sessionId(),
                source.sessionType(),
                source.sessionStatus(),
                source.startedAt(),
                source.completedAt(),
                source.notes(),
                toExerciseResponses(source.exercises()));
    }

    default WorkoutSessionDetailResponse toSessionDetailResponse(WorkoutAssemblySource.SessionDetail source) {
        return new WorkoutSessionDetailResponse(
                source.sessionId(),
                source.sessionType(),
                source.sessionStatus(),
                source.cycleRunId(),
                source.runNo(),
                source.templateId(),
                source.templateName(),
                source.dayIndex(),
                source.dayName(),
                source.startedAt(),
                source.completedAt(),
                source.notes(),
                toExerciseResponses(source.exercises()));
    }

    default SaveWorkoutSessionResponse toSaveWorkoutSessionResponse(WorkoutAssemblySource.SaveResult source) {
        return new SaveWorkoutSessionResponse(source.sessionId(), source.sessionStatus(), source.savedAt());
    }

    default CompleteWorkoutSessionResponse toCompleteWorkoutSessionResponse(
            WorkoutAssemblySource.CompletionResult source) {
        return new CompleteWorkoutSessionResponse(
                source.sessionId(),
                source.sessionStatus(),
                source.completedAt(),
                source.completedDayIndex(),
                source.cycleRunId(),
                source.cycleRunStatus(),
                source.nextCurrentDayIndex(),
                toDaySummaryResponse(source.nextDay()),
                toDayDetailResponse(source.completedDay()));
    }

    default WorkoutRecentSessionPageResponse toRecentSessionPageResponse(
            int page,
            int pageSize,
            long total,
            List<WorkoutAssemblySource.RecentSession> records) {
        return new WorkoutRecentSessionPageResponse(page, pageSize, total, toRecentSessionResponses(records));
    }

    default RestartWorkoutCycleResponse toRestartWorkoutCycleResponse(WorkoutAssemblySource.RestartResult source) {
        return new RestartWorkoutCycleResponse(
                source.templateId(),
                source.templateName(),
                source.cycleRunId(),
                source.runNo(),
                source.cycleRunStatus(),
                source.currentDayIndex());
    }

    default WorkoutSessionExerciseResponse toExerciseResponse(WorkoutAssemblySource.Exercise source) {
        return new WorkoutSessionExerciseResponse(
                source.sessionExerciseId(),
                source.sortOrder(),
                source.exerciseId(),
                source.exerciseName(),
                source.structureType(),
                source.exerciseStatus(),
                source.failureReason(),
                source.feedback(),
                toExerciseItemResponses(source.items()));
    }

    default WorkoutSessionExerciseResponse toPreviewExerciseResponse(ExerciseSnapshot source) {
        return new WorkoutSessionExerciseResponse(
                null,
                source.sortOrder(),
                source.exerciseId(),
                source.exerciseNameSnapshot(),
                source.structureType(),
                null,
                null,
                null,
                toPreviewExerciseItemResponses(source.items()));
    }

    default WorkoutSessionExerciseItemResponse toExerciseItemResponse(WorkoutAssemblySource.ExerciseItem source) {
        return new WorkoutSessionExerciseItemResponse(
                source.itemIndex(),
                source.itemType(),
                source.itemName(),
                source.note(),
                toExerciseMetricResponses(source.metrics()));
    }

    default WorkoutSessionExerciseMetricResponse toExerciseMetricResponse(WorkoutAssemblySource.ExerciseMetric source) {
        return new WorkoutSessionExerciseMetricResponse(
                source.sortOrder(),
                source.metricKey(),
                resolveMetricUnit(source.metricKey()),
                source.plannedValueNumber(),
                source.actualValueNumber());
    }

    default String resolveMetricUnit(String metricKeyValue) {
        MetricKey metricKey = MetricKey.fromValue(metricKeyValue);
        return metricKey == null ? null : metricKey.getUnit();
    }

    private List<WorkoutDayNavigationItemResponse> toDayNavigationResponses(
            List<WorkoutAssemblySource.DayNavigation> sources) {
        if (sources == null) {
            return List.of();
        }
        return sources.stream()
                .map(source -> new WorkoutDayNavigationItemResponse(
                        source.dayIndex(),
                        source.dayName(),
                        source.isRestDay(),
                        source.dayState(),
                        source.sessionId(),
                        source.sessionStatus()))
                .toList();
    }

    private List<WorkoutSessionExerciseResponse> toExerciseResponses(
            List<WorkoutAssemblySource.Exercise> sources) {
        if (sources == null) {
            return List.of();
        }
        return sources.stream().map(this::toExerciseResponse).toList();
    }

    private List<WorkoutSessionExerciseItemResponse> toExerciseItemResponses(
            List<WorkoutAssemblySource.ExerciseItem> sources) {
        if (sources == null) {
            return List.of();
        }
        return sources.stream().map(this::toExerciseItemResponse).toList();
    }

    private List<WorkoutSessionExerciseMetricResponse> toExerciseMetricResponses(
            List<WorkoutAssemblySource.ExerciseMetric> sources) {
        if (sources == null) {
            return List.of();
        }
        return sources.stream().map(this::toExerciseMetricResponse).toList();
    }

    private List<WorkoutSessionExerciseItemResponse> toPreviewExerciseItemResponses(List<ItemSnapshot> sources) {
        if (sources == null) {
            return List.of();
        }
        return sources.stream()
                .map(source -> new WorkoutSessionExerciseItemResponse(
                        source.itemIndex(),
                        source.itemType(),
                        source.itemName(),
                        source.note(),
                        toPreviewExerciseMetricResponses(source.metrics())))
                .toList();
    }

    private List<WorkoutSessionExerciseMetricResponse> toPreviewExerciseMetricResponses(List<MetricSnapshot> sources) {
        if (sources == null) {
            return List.of();
        }
        return sources.stream()
                .map(source -> new WorkoutSessionExerciseMetricResponse(
                        source.sortOrder(),
                        source.metricKey(),
                        resolveMetricUnit(source.metricKey()),
                        source.metricValueNumber(),
                        null))
                .toList();
    }

    private WorkoutDaySummaryResponse toDaySummaryResponse(WorkoutAssemblySource.DaySummary source) {
        if (source == null) {
            return null;
        }
        return new WorkoutDaySummaryResponse(source.dayIndex(), source.dayName(), source.isRestDay());
    }

    private List<WorkoutRecentSessionItemResponse> toRecentSessionResponses(
            List<WorkoutAssemblySource.RecentSession> sources) {
        if (sources == null) {
            return List.of();
        }
        return sources.stream()
                .map(source -> new WorkoutRecentSessionItemResponse(
                        source.sessionId(),
                        source.sessionType(),
                        source.sessionStatus(),
                        source.templateId(),
                        source.templateName(),
                        source.cycleRunId(),
                        source.runNo(),
                        source.dayIndex(),
                        source.dayName(),
                        source.startedAt(),
                        source.completedAt()))
                .toList();
    }
}
