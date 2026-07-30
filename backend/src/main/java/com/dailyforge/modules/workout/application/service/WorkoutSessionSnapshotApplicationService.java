package com.dailyforge.modules.workout.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.DaySnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.ExerciseSnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.ItemSnapshot;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService.MetricSnapshot;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleDayExerciseEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateDayEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleDayExerciseMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateDayMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.entity.TrainingSessionExerciseItemMetricEntity;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseItemMetricMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionExerciseMapper;
import com.dailyforge.modules.workout.infrastructure.persistence.mapper.TrainingSessionMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WorkoutSessionSnapshotApplicationService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutSessionSnapshotApplicationService.class);

    private final CycleTemplateDayMapper cycleTemplateDayMapper;
    private final CycleDayExerciseMapper cycleDayExerciseMapper;
    private final CycleTemplateVersionDomainService cycleTemplateVersionDomainService;
    private final TrainingSessionMapper trainingSessionMapper;
    private final TrainingSessionExerciseMapper trainingSessionExerciseMapper;
    private final TrainingSessionExerciseItemMapper trainingSessionExerciseItemMapper;
    private final TrainingSessionExerciseItemMetricMapper trainingSessionExerciseItemMetricMapper;

    public WorkoutSessionSnapshotApplicationService(
            CycleTemplateDayMapper cycleTemplateDayMapper,
            CycleDayExerciseMapper cycleDayExerciseMapper,
            CycleTemplateVersionDomainService cycleTemplateVersionDomainService,
            TrainingSessionMapper trainingSessionMapper,
            TrainingSessionExerciseMapper trainingSessionExerciseMapper,
            TrainingSessionExerciseItemMapper trainingSessionExerciseItemMapper,
            TrainingSessionExerciseItemMetricMapper trainingSessionExerciseItemMetricMapper) {
        this.cycleTemplateDayMapper = cycleTemplateDayMapper;
        this.cycleDayExerciseMapper = cycleDayExerciseMapper;
        this.cycleTemplateVersionDomainService = cycleTemplateVersionDomainService;
        this.trainingSessionMapper = trainingSessionMapper;
        this.trainingSessionExerciseMapper = trainingSessionExerciseMapper;
        this.trainingSessionExerciseItemMapper = trainingSessionExerciseItemMapper;
        this.trainingSessionExerciseItemMetricMapper = trainingSessionExerciseItemMetricMapper;
    }

    /**
     * 复制模板 Day 的动作快照，保证训练 session 后续不直接依赖可变模板结构。
     */
    public void copyTemplateDaySnapshot(
            Long sessionId,
            Long templateDayId,
            List<ExerciseSnapshot> exercises) {
        Map<String, CycleDayExerciseEntity> planExercises = new HashMap<>();
        for (CycleDayExerciseEntity planExercise : cycleDayExerciseMapper.selectByTemplateDayId(templateDayId)) {
            planExercises.put(planExerciseKey(planExercise.getSortOrder(), planExercise.getExerciseId()), planExercise);
        }
        for (ExerciseSnapshot exerciseSnapshot : exercises) {
            CycleDayExerciseEntity planExercise = planExercises.get(
                    planExerciseKey(exerciseSnapshot.sortOrder(), exerciseSnapshot.exerciseId()));
            if (planExercise == null) {
                throw new BusinessException(ErrorCode.WORKOUT_CURRENT_DAY_SESSION_CONFLICT);
            }
            TrainingSessionExerciseEntity exercise = new TrainingSessionExerciseEntity();
            exercise.setSessionId(sessionId);
            exercise.setCycleDayExerciseId(planExercise.getId());
            exercise.setExerciseId(exerciseSnapshot.exerciseId());
            exercise.setExerciseNameSnapshot(exerciseSnapshot.exerciseNameSnapshot());
            exercise.setStructureType(exerciseSnapshot.structureType());
            exercise.setSortOrder(exerciseSnapshot.sortOrder());
            trainingSessionExerciseMapper.insert(exercise);
            for (ItemSnapshot itemSnapshot : exerciseSnapshot.items()) {
                TrainingSessionExerciseItemEntity item = new TrainingSessionExerciseItemEntity();
                item.setSessionExerciseId(exercise.getId());
                item.setItemIndex(itemSnapshot.itemIndex());
                item.setItemType(itemSnapshot.itemType());
                item.setItemNameSnapshot(itemSnapshot.itemName());
                item.setNoteSnapshot(itemSnapshot.note());
                item.setSortOrder(itemSnapshot.itemIndex());
                trainingSessionExerciseItemMapper.insert(item);
                for (MetricSnapshot metricSnapshot : itemSnapshot.metrics()) {
                    TrainingSessionExerciseItemMetricEntity metric = new TrainingSessionExerciseItemMetricEntity();
                    metric.setSessionExerciseItemId(item.getId());
                    metric.setMetricKey(metricSnapshot.metricKey());
                    metric.setPlannedValueNumber(metricSnapshot.metricValueNumber());
                    metric.setSortOrder(metricSnapshot.sortOrder());
                    trainingSessionExerciseItemMetricMapper.insert(metric);
                }
            }
        }
    }

    /**
     * 刷新当前 Day 的进行中 session；前端必须在保存 active 模板前提示该操作会覆盖当前训练填写记录。
     */
    public boolean refreshCurrentDaySession(
            Long userId,
            Long cycleRunId,
            Long templateId,
            String templateName,
            Long templateVersionId,
            Integer currentDayIndex) {
        if (currentDayIndex == null) {
            return false;
        }
        TrainingSessionEntity session =
                trainingSessionMapper.selectByCycleRunIdAndDayIndexForUpdate(cycleRunId, currentDayIndex);
        if (session == null || !userId.equals(session.getUserId()) || !"in_progress".equals(session.getStatus())) {
            return false;
        }

        CycleTemplateDayEntity templateDay = findTemplateDay(templateVersionId, currentDayIndex);
        DaySnapshot snapshot = cycleTemplateVersionDomainService.loadVersionSnapshot(templateVersionId)
                .toDayIndexMap()
                .get(currentDayIndex);
        if (snapshot == null) {
            throw new BusinessException(ErrorCode.WORKOUT_DAY_OUT_OF_RANGE);
        }

        deleteExerciseSnapshot(session.getId());
        int updated = trainingSessionMapper.refreshInProgressSnapshot(
                session.getId(),
                userId,
                templateId,
                templateVersionId,
                templateDay.getId(),
                templateName,
                resolveDayName(snapshot, currentDayIndex),
                isRestDay(snapshot) ? "rest_day" : "workout");
        if (updated != 1) {
            throw new BusinessException(ErrorCode.WORKOUT_CURRENT_DAY_SESSION_CONFLICT);
        }
        copyTemplateDaySnapshot(session.getId(), templateDay.getId(), snapshot.exercises());

        log.debug(
                "Refreshed workout session snapshot. userId={}, cycleRunId={}, sessionId={}, templateVersionId={}, dayIndex={}",
                userId,
                cycleRunId,
                session.getId(),
                templateVersionId,
                currentDayIndex);
        return true;
    }

    private void deleteExerciseSnapshot(Long sessionId) {
        List<TrainingSessionExerciseEntity> exercises = trainingSessionExerciseMapper.selectBySessionId(sessionId);
        for (TrainingSessionExerciseEntity exercise : exercises) {
            List<TrainingSessionExerciseItemEntity> items =
                    trainingSessionExerciseItemMapper.selectBySessionExerciseId(exercise.getId());
            for (TrainingSessionExerciseItemEntity item : items) {
                for (TrainingSessionExerciseItemMetricEntity metric :
                        trainingSessionExerciseItemMetricMapper.selectBySessionExerciseItemId(item.getId())) {
                    trainingSessionExerciseItemMetricMapper.deleteById(metric.getId());
                }
                trainingSessionExerciseItemMapper.deleteById(item.getId());
            }
            trainingSessionExerciseMapper.deleteById(exercise.getId());
        }
    }

    private CycleTemplateDayEntity findTemplateDay(Long templateVersionId, int dayIndex) {
        return cycleTemplateDayMapper.selectByVersionId(templateVersionId).stream()
                .filter(day -> day.getDayIndex().equals(dayIndex))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKOUT_DAY_OUT_OF_RANGE));
    }


    private String resolveDayName(DaySnapshot day, int dayIndex) {
        return day.dayName() == null || day.dayName().isBlank() ? "Day " + dayIndex : day.dayName();
    }

    private boolean isRestDay(DaySnapshot day) {
        return day.exercises() == null || day.exercises().isEmpty();
    }

    private String planExerciseKey(Integer sortOrder, Long exerciseId) {
        return sortOrder + ":" + exerciseId;
    }
}
