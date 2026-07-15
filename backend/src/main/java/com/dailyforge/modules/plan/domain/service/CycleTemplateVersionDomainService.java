package com.dailyforge.modules.plan.domain.service;

import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleDayExerciseEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleDayExerciseItemEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleDayExerciseItemMetricEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateDayEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateVersionEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleDayExerciseItemMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleDayExerciseItemMetricMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleDayExerciseMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateDayMapper;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleTemplateVersionMapper;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateExerciseRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateItemRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateMetricRequest;
import java.math.BigDecimal;
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
    private final CycleDayExerciseItemMapper cycleDayExerciseItemMapper;
    private final CycleDayExerciseItemMetricMapper cycleDayExerciseItemMetricMapper;
    private final CycleTemplatePolicyService cycleTemplatePolicyService;

    public CycleTemplateVersionDomainService(
            CycleTemplateVersionMapper cycleTemplateVersionMapper,
            CycleTemplateDayMapper cycleTemplateDayMapper,
            CycleDayExerciseMapper cycleDayExerciseMapper,
            CycleDayExerciseItemMapper cycleDayExerciseItemMapper,
            CycleDayExerciseItemMetricMapper cycleDayExerciseItemMetricMapper,
            CycleTemplatePolicyService cycleTemplatePolicyService) {
        this.cycleTemplateVersionMapper = cycleTemplateVersionMapper;
        this.cycleTemplateDayMapper = cycleTemplateDayMapper;
        this.cycleDayExerciseMapper = cycleDayExerciseMapper;
        this.cycleDayExerciseItemMapper = cycleDayExerciseItemMapper;
        this.cycleDayExerciseItemMetricMapper = cycleDayExerciseItemMetricMapper;
        this.cycleTemplatePolicyService = cycleTemplatePolicyService;
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
     * Load one version snapshot with nested day, exercise, item and metric structure.
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
                List<CycleDayExerciseItemEntity> items =
                        cycleDayExerciseItemMapper.selectByCycleDayExerciseId(exercise.getId());
                List<ItemSnapshot> itemSnapshots = new ArrayList<>();
                for (CycleDayExerciseItemEntity item : items) {
                    List<CycleDayExerciseItemMetricEntity> metrics =
                            cycleDayExerciseItemMetricMapper.selectByExerciseItemId(item.getId());
                    List<MetricSnapshot> metricSnapshots = new ArrayList<>();
                    for (CycleDayExerciseItemMetricEntity metric : metrics) {
                        metricSnapshots.add(new MetricSnapshot(
                                metric.getSortOrder(),
                                metric.getMetricKey(),
                                metric.getMetricValueNumber()));
                    }
                    itemSnapshots.add(new ItemSnapshot(
                            item.getItemIndex(),
                            item.getItemType(),
                            item.getItemName(),
                            item.getNote(),
                            metricSnapshots));
                }
                exerciseSnapshots.add(new ExerciseSnapshot(
                        exercise.getSortOrder(),
                        exercise.getExerciseId(),
                        exercise.getExerciseNameSnapshot(),
                        exercise.getStructureType(),
                        exercise.getNote(),
                        itemSnapshots));
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
            Map<Long, SystemExerciseLookupResult> exerciseMap) {
        List<CycleTemplateDayRequest> safeDays = safeDayRequests(days);
        for (CycleTemplateDayRequest dayRequest : safeDays) {
            insertDayRequest(versionId, dayRequest, exerciseMap);
        }
        log.debug("Cycle template version saved. versionId={}, dayCount={}", versionId, safeDays.size());
    }

    /**
     * Clone source version, replace only submitted editable days, and preserve other future days.
     */
    public void cloneLockedDaysAndReplaceEditableDays(
            Long sourceVersionId,
            Long targetVersionId,
            Integer editableFromDayIndex,
            List<CycleTemplateDayRequest> editableDays,
            Map<Long, SystemExerciseLookupResult> exerciseMap) {
        VersionSnapshot sourceSnapshot = loadVersionSnapshot(sourceVersionId);
        Map<Integer, CycleTemplateDayRequest> editableDayMap = toDayRequestMap(safeDayRequests(editableDays));
        for (DaySnapshot daySnapshot : sourceSnapshot.days()) {
            if (daySnapshot.dayIndex() < editableFromDayIndex) {
                insertDaySnapshot(targetVersionId, daySnapshot);
                continue;
            }
            CycleTemplateDayRequest replacement = editableDayMap.remove(daySnapshot.dayIndex());
            if (replacement != null) {
                insertDayRequest(targetVersionId, replacement, exerciseMap);
            } else {
                insertDaySnapshot(targetVersionId, daySnapshot);
            }
        }
        for (CycleTemplateDayRequest dayRequest : editableDayMap.values()) {
            insertDayRequest(targetVersionId, dayRequest, exerciseMap);
        }
        log.debug(
                "Active template version patched. sourceVersionId={}, targetVersionId={}, editableFromDayIndex={}, replacedDayCount={}",
                sourceVersionId,
                targetVersionId,
                editableFromDayIndex,
                editableDays == null ? 0 : editableDays.size());
    }

    /**
     * Clone whole version content into another version id.
     */
    public void cloneWholeVersion(Long sourceVersionId, Long targetVersionId) {
        VersionSnapshot snapshot = loadVersionSnapshot(sourceVersionId);
        for (DaySnapshot daySnapshot : snapshot.days()) {
            insertDaySnapshot(targetVersionId, daySnapshot);
        }
        log.debug("Cycle template version cloned. sourceVersionId={}, targetVersionId={}, dayCount={}",
                sourceVersionId, targetVersionId, snapshot.days().size());
    }

    private Map<Integer, CycleTemplateDayRequest> toDayRequestMap(List<CycleTemplateDayRequest> days) {
        Map<Integer, CycleTemplateDayRequest> dayMap = new LinkedHashMap<>();
        for (CycleTemplateDayRequest day : days) {
            dayMap.put(day.dayIndex(), day);
        }
        return dayMap;
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
            Map<Long, SystemExerciseLookupResult> exerciseMap) {
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
            insertExerciseRequest(dayEntity.getId(), exerciseRequest, exerciseMap.get(exerciseRequest.exerciseId()));
        }
    }

    private void insertDaySnapshot(Long versionId, DaySnapshot daySnapshot) {
        CycleTemplateDayEntity dayEntity = new CycleTemplateDayEntity();
        dayEntity.setTemplateVersionId(versionId);
        dayEntity.setDayIndex(daySnapshot.dayIndex());
        dayEntity.setDayName(daySnapshot.dayName());
        cycleTemplateDayMapper.insert(dayEntity);

        for (ExerciseSnapshot exerciseSnapshot : daySnapshot.exercises()) {
            insertExerciseSnapshot(dayEntity.getId(), exerciseSnapshot);
        }
    }

    private void insertExerciseRequest(
            Long templateDayId,
            CycleTemplateExerciseRequest request,
            SystemExerciseLookupResult exercise) {
        CycleDayExerciseEntity entity = new CycleDayExerciseEntity();
        entity.setTemplateDayId(templateDayId);
        entity.setExerciseId(request.exerciseId());
        entity.setExerciseNameSnapshot(exercise.name());
        entity.setStructureType(request.structureType());
        entity.setNote(request.note());
        entity.setSortOrder(request.sortOrder());
        cycleDayExerciseMapper.insert(entity);

        List<CycleTemplateItemRequest> items = request.items();
        if (items == null || items.isEmpty()) {
            return;
        }
        for (CycleTemplateItemRequest itemRequest : items) {
            CycleDayExerciseItemEntity itemEntity = new CycleDayExerciseItemEntity();
            itemEntity.setCycleDayExerciseId(entity.getId());
            itemEntity.setItemIndex(itemRequest.itemIndex());
            itemEntity.setItemType(itemRequest.itemType());
            itemEntity.setItemName(itemRequest.itemName());
            itemEntity.setNote(itemRequest.note());
            itemEntity.setSortOrder(itemRequest.itemIndex());
            cycleDayExerciseItemMapper.insert(itemEntity);

            insertMetricRequests(itemEntity.getId(), itemRequest.metrics());
        }
    }

    private void insertMetricRequests(Long exerciseItemId, List<CycleTemplateMetricRequest> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        for (CycleTemplateMetricRequest metricRequest : metrics) {
            CycleDayExerciseItemMetricEntity metricEntity = new CycleDayExerciseItemMetricEntity();
            metricEntity.setExerciseItemId(exerciseItemId);
            metricEntity.setMetricKey(metricRequest.metricKey());
            metricEntity.setMetricValueNumber(metricRequest.metricValueNumber());
            metricEntity.setSortOrder(metricRequest.sortOrder());
            cycleDayExerciseItemMetricMapper.insert(metricEntity);
        }
    }

    private void insertExerciseSnapshot(Long templateDayId, ExerciseSnapshot exerciseSnapshot) {
        CycleDayExerciseEntity entity = new CycleDayExerciseEntity();
        entity.setTemplateDayId(templateDayId);
        entity.setExerciseId(exerciseSnapshot.exerciseId());
        entity.setExerciseNameSnapshot(exerciseSnapshot.exerciseNameSnapshot());
        entity.setStructureType(exerciseSnapshot.structureType());
        entity.setNote(exerciseSnapshot.note());
        entity.setSortOrder(exerciseSnapshot.sortOrder());
        cycleDayExerciseMapper.insert(entity);

        for (ItemSnapshot itemSnapshot : exerciseSnapshot.items()) {
            CycleDayExerciseItemEntity itemEntity = new CycleDayExerciseItemEntity();
            itemEntity.setCycleDayExerciseId(entity.getId());
            itemEntity.setItemIndex(itemSnapshot.itemIndex());
            itemEntity.setItemType(itemSnapshot.itemType());
            itemEntity.setItemName(itemSnapshot.itemName());
            itemEntity.setNote(itemSnapshot.note());
            itemEntity.setSortOrder(itemSnapshot.itemIndex());
            cycleDayExerciseItemMapper.insert(itemEntity);

            for (MetricSnapshot metricSnapshot : itemSnapshot.metrics()) {
                CycleDayExerciseItemMetricEntity metricEntity = new CycleDayExerciseItemMetricEntity();
                metricEntity.setExerciseItemId(itemEntity.getId());
                metricEntity.setMetricKey(metricSnapshot.metricKey());
                metricEntity.setMetricValueNumber(metricSnapshot.metricValueNumber());
                metricEntity.setSortOrder(metricSnapshot.sortOrder());
                cycleDayExerciseItemMetricMapper.insert(metricEntity);
            }
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
            String structureType,
            String note,
            List<ItemSnapshot> items) {
    }

    public record ItemSnapshot(
            Integer itemIndex,
            String itemType,
            String itemName,
            String note,
            List<MetricSnapshot> metrics) {
    }

    public record MetricSnapshot(Integer sortOrder, String metricKey, BigDecimal metricValueNumber) {
    }
}
