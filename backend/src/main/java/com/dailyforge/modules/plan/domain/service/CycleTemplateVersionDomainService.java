package com.dailyforge.modules.plan.domain.service;

import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleDayExerciseEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateDayEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateVersionEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.ExerciseReadEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleDayExerciseMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateDayMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateVersionMapper;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateExerciseRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CycleTemplateVersionDomainService {

    private static final Logger log = LoggerFactory.getLogger(CycleTemplateVersionDomainService.class);

    private final CycleTemplateVersionMapper cycleTemplateVersionMapper;
    private final CycleTemplateDayMapper cycleTemplateDayMapper;
    private final CycleDayExerciseMapper cycleDayExerciseMapper;
    private final CycleTemplatePolicyService cycleTemplatePolicyService;
    private final ObjectMapper objectMapper;

    public CycleTemplateVersionDomainService(
            CycleTemplateVersionMapper cycleTemplateVersionMapper,
            CycleTemplateDayMapper cycleTemplateDayMapper,
            CycleDayExerciseMapper cycleDayExerciseMapper,
            CycleTemplatePolicyService cycleTemplatePolicyService,
            ObjectMapper objectMapper) {
        this.cycleTemplateVersionMapper = cycleTemplateVersionMapper;
        this.cycleTemplateDayMapper = cycleTemplateDayMapper;
        this.cycleDayExerciseMapper = cycleDayExerciseMapper;
        this.cycleTemplatePolicyService = cycleTemplatePolicyService;
        this.objectMapper = objectMapper;
    }

    /**
     * Create and persist a new version row with incremented version number.
     */
    public CycleTemplateVersionEntity createVersion(Long templateId, String sourceType, String changeNote) {
        Integer maxVersionNo = cycleTemplateVersionMapper.selectMaxVersionNo(templateId);
        CycleTemplateVersionEntity version = new CycleTemplateVersionEntity();
        version.setTemplateId(templateId);
        version.setVersionNo((maxVersionNo == null ? 0 : maxVersionNo) + 1);
        version.setSourceType(sourceType);
        version.setChangeNote(changeNote);
        cycleTemplateVersionMapper.insert(version);
        return version;
    }

    /**
     * Load one version snapshot with nested day and exercise structure.
     */
    public VersionSnapshot loadVersionSnapshot(Long versionId) {
        if (versionId == null) {
            return new VersionSnapshot(Collections.emptyList());
        }
        List<CycleTemplateDayEntity> days = cycleTemplateDayMapper.selectByVersionId(versionId);
        List<DaySnapshot> snapshots = new ArrayList<>();
        for (CycleTemplateDayEntity day : days) {
            List<CycleDayExerciseEntity> exercises = cycleDayExerciseMapper.selectByTemplateDayId(day.getId());
            List<ExerciseSnapshot> exerciseSnapshots = new ArrayList<>();
            for (CycleDayExerciseEntity exercise : exercises) {
                exerciseSnapshots.add(new ExerciseSnapshot(
                        exercise.getSortOrder(),
                        exercise.getExerciseId(),
                        exercise.getExerciseNameSnapshot(),
                        exercise.getTargetSets(),
                        exercise.getTargetRepsMin(),
                        exercise.getTargetRepsMax(),
                        exercise.getTargetWeightKg(),
                        exercise.getTargetDurationSeconds(),
                        exercise.getTargetRestSeconds(),
                        exercise.getTargetRpe(),
                        exercise.getNotes(),
                        exercise.getTargetExtraJson()));
            }
            snapshots.add(new DaySnapshot(day.getDayIndex(), day.getDayName(), exerciseSnapshots));
        }
        return new VersionSnapshot(snapshots);
    }

    /**
     * Replace the whole version content with request days.
     */
    public void saveFullVersionContent(
            Long versionId,
            List<CycleTemplateDayRequest> days,
            Map<Long, ExerciseReadEntity> exerciseMap) {
        List<CycleTemplateDayRequest> safeDays = safeDayRequests(days);
        for (CycleTemplateDayRequest dayRequest : safeDays) {
            insertDayRequest(versionId, dayRequest, exerciseMap);
        }
    }

    /**
     * Clone only locked days from source version, then append editable request days.
     */
    public void cloneLockedDaysAndReplaceEditableDays(
            Long sourceVersionId,
            Long targetVersionId,
            Integer editableFromDayIndex,
            List<CycleTemplateDayRequest> editableDays,
            Map<Long, ExerciseReadEntity> exerciseMap) {
        VersionSnapshot sourceSnapshot = loadVersionSnapshot(sourceVersionId);
        for (DaySnapshot daySnapshot : sourceSnapshot.days()) {
            if (daySnapshot.dayIndex() < editableFromDayIndex) {
                insertDaySnapshot(targetVersionId, daySnapshot);
            }
        }
        for (CycleTemplateDayRequest editableDay : safeDayRequests(editableDays)) {
            insertDayRequest(targetVersionId, editableDay, exerciseMap);
        }
        log.debug("Active template version patched. sourceVersionId={}, targetVersionId={}, editableFromDayIndex={}",
                sourceVersionId, targetVersionId, editableFromDayIndex);
    }

    /**
     * Clone whole version content into another version id.
     */
    public void cloneWholeVersion(Long sourceVersionId, Long targetVersionId) {
        VersionSnapshot snapshot = loadVersionSnapshot(sourceVersionId);
        for (DaySnapshot daySnapshot : snapshot.days()) {
            insertDaySnapshot(targetVersionId, daySnapshot);
        }
    }

    private List<CycleTemplateDayRequest> safeDayRequests(List<CycleTemplateDayRequest> days) {
        if (days == null || days.isEmpty()) {
            return Collections.emptyList();
        }
        List<CycleTemplateDayRequest> sorted = new ArrayList<>(days);
        sorted.sort(Comparator.comparing(CycleTemplateDayRequest::dayIndex));
        return sorted;
    }

    private void insertDayRequest(
            Long versionId,
            CycleTemplateDayRequest dayRequest,
            Map<Long, ExerciseReadEntity> exerciseMap) {
        CycleTemplateDayEntity dayEntity = new CycleTemplateDayEntity();
        dayEntity.setTemplateVersionId(versionId);
        dayEntity.setDayIndex(dayRequest.dayIndex());
        dayEntity.setDayName(cycleTemplatePolicyService.normalizeDayName(dayRequest.dayName(), dayRequest.dayIndex()));
        cycleTemplateDayMapper.insert(dayEntity);

        List<CycleTemplateExerciseRequest> exercises = dayRequest.exercises();
        if (exercises == null || exercises.isEmpty()) {
            return;
        }
        for (CycleTemplateExerciseRequest exerciseRequest : exercises) {
            insertExercise(dayEntity.getId(), exerciseRequest, exerciseMap.get(exerciseRequest.exerciseId()));
        }
    }

    private void insertDaySnapshot(Long versionId, DaySnapshot daySnapshot) {
        CycleTemplateDayEntity dayEntity = new CycleTemplateDayEntity();
        dayEntity.setTemplateVersionId(versionId);
        dayEntity.setDayIndex(daySnapshot.dayIndex());
        dayEntity.setDayName(daySnapshot.dayName());
        cycleTemplateDayMapper.insert(dayEntity);

        for (ExerciseSnapshot exerciseSnapshot : daySnapshot.exercises()) {
            CycleDayExerciseEntity entity = new CycleDayExerciseEntity();
            entity.setTemplateDayId(dayEntity.getId());
            entity.setExerciseId(exerciseSnapshot.exerciseId());
            entity.setExerciseNameSnapshot(exerciseSnapshot.exerciseNameSnapshot());
            entity.setTargetSets(exerciseSnapshot.targetSets());
            entity.setTargetRepsMin(exerciseSnapshot.targetRepsMin());
            entity.setTargetRepsMax(exerciseSnapshot.targetRepsMax());
            entity.setTargetWeightKg(exerciseSnapshot.targetWeightKg());
            entity.setTargetDurationSeconds(exerciseSnapshot.targetDurationSeconds());
            entity.setTargetRestSeconds(exerciseSnapshot.targetRestSeconds());
            entity.setTargetRpe(exerciseSnapshot.targetRpe());
            entity.setTargetExtraJson(exerciseSnapshot.targetExtraJson());
            entity.setNotes(exerciseSnapshot.notes());
            entity.setSortOrder(exerciseSnapshot.sortOrder());
            cycleDayExerciseMapper.insert(entity);
        }
    }

    private void insertExercise(
            Long templateDayId,
            CycleTemplateExerciseRequest request,
            ExerciseReadEntity exercise) {
        CycleDayExerciseEntity entity = new CycleDayExerciseEntity();
        entity.setTemplateDayId(templateDayId);
        entity.setExerciseId(request.exerciseId());
        entity.setExerciseNameSnapshot(exercise.getName());
        entity.setTargetSets(request.targetSets());
        entity.setTargetRepsMin(request.targetRepsMin());
        entity.setTargetRepsMax(request.targetRepsMax());
        entity.setTargetWeightKg(request.targetWeightKg());
        entity.setTargetDurationSeconds(request.targetDurationSeconds());
        entity.setTargetRestSeconds(request.restSeconds());
        entity.setTargetRpe(request.targetRpe());
        entity.setTargetExtraJson(writeJson(request.targetExtraJson()));
        entity.setNotes(request.note());
        entity.setSortOrder(request.sortOrder());
        cycleDayExerciseMapper.insert(entity);
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize targetExtraJson", exception);
        }
    }

    public record VersionSnapshot(List<DaySnapshot> days) {
        public Map<Integer, DaySnapshot> toDayIndexMap() {
            Map<Integer, DaySnapshot> map = new LinkedHashMap<>();
            for (DaySnapshot day : days) {
                map.put(day.dayIndex(), day);
            }
            return map;
        }
    }

    public record DaySnapshot(Integer dayIndex, String dayName, List<ExerciseSnapshot> exercises) {
    }

    public record ExerciseSnapshot(
            Integer sortOrder,
            Long exerciseId,
            String exerciseNameSnapshot,
            Integer targetSets,
            Integer targetRepsMin,
            Integer targetRepsMax,
            java.math.BigDecimal targetWeightKg,
            Integer targetDurationSeconds,
            Integer targetRestSeconds,
            java.math.BigDecimal targetRpe,
            String notes,
            String targetExtraJson) {
    }
}
