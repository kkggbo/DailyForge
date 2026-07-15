package com.dailyforge.modules.plan.domain.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.plan.domain.model.ItemType;
import com.dailyforge.modules.plan.domain.model.MetricKey;
import com.dailyforge.modules.plan.domain.model.StructureType;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateExerciseRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateItemRequest;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateMetricRequest;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExerciseStructurePolicyService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseStructurePolicyService.class);

    /**
     * Validate request day structure against exercise metadata.
     */
    public void validateDayRequests(List<CycleTemplateDayRequest> days, Map<Long, SystemExerciseLookupResult> exerciseMap) {
        if (days == null || days.isEmpty()) {
            return;
        }
        for (CycleTemplateDayRequest day : days) {
            if (day.exercises() == null || day.exercises().isEmpty()) {
                continue;
            }
            for (CycleTemplateExerciseRequest exercise : day.exercises()) {
                validateExerciseRequest(exercise, exerciseMap.get(exercise.exerciseId()));
            }
        }
        log.debug("Cycle template request structure validated. dayCount={}, exerciseCount={}",
                days.size(), countExercises(days));
    }

    /**
     * Validate persisted version snapshot against current exercise metadata.
     */
    public void validateVersionSnapshot(
            CycleTemplateVersionDomainService.VersionSnapshot snapshot,
            Map<Long, SystemExerciseLookupResult> exerciseMap) {
        if (snapshot == null || snapshot.days().isEmpty()) {
            return;
        }
        for (CycleTemplateVersionDomainService.DaySnapshot day : snapshot.days()) {
            for (CycleTemplateVersionDomainService.ExerciseSnapshot exercise : day.exercises()) {
                validateExerciseSnapshot(exercise, exerciseMap.get(exercise.exerciseId()));
            }
        }
        log.debug("Persisted cycle template structure validated. dayCount={}, exerciseCount={}",
                snapshot.days().size(), countExercises(snapshot));
    }

    private void validateExerciseRequest(CycleTemplateExerciseRequest exercise, SystemExerciseLookupResult exerciseMeta) {
        StructureType structureType = validateStructureType(exercise.structureType(), exerciseMeta);
        List<CycleTemplateItemRequest> items = exercise.items();
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID, "items must not be empty");
        }
        validateItems(structureType, items);
    }

    private void validateExerciseSnapshot(
            CycleTemplateVersionDomainService.ExerciseSnapshot exercise,
            SystemExerciseLookupResult exerciseMeta) {
        StructureType structureType = validateStructureType(exercise.structureType(), exerciseMeta);
        List<CycleTemplateVersionDomainService.ItemSnapshot> items = exercise.items();
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID, "persisted items must not be empty");
        }
        validateSnapshotItems(structureType, items);
    }

    private StructureType validateStructureType(String rawStructureType, SystemExerciseLookupResult exerciseMeta) {
        StructureType structureType = StructureType.fromValue(rawStructureType);
        if (structureType == null || exerciseMeta == null
                || StructureType.fromValue(exerciseMeta.defaultStructureType()) == null
                || !rawStructureType.equals(exerciseMeta.defaultStructureType())) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_STRUCTURE_TYPE_INVALID);
        }
        return structureType;
    }

    private void validateItems(StructureType structureType, List<CycleTemplateItemRequest> items) {
        if (structureType == StructureType.SINGLE_SEGMENT && items.size() != 1) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_COUNT_INVALID);
        }
        Set<Integer> itemIndexes = new HashSet<>();
        for (CycleTemplateItemRequest item : items) {
            if (!itemIndexes.add(item.itemIndex())) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID,
                        "duplicate itemIndex: " + item.itemIndex());
            }
            ItemType itemType = ItemType.fromValue(item.itemType());
            if (itemType == null) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID);
            }
            if (structureType == StructureType.SET_BASED && itemType != ItemType.SET) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID);
            }
            if (structureType == StructureType.SINGLE_SEGMENT && itemType != ItemType.SEGMENT) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID);
            }
            validateMetrics(item.metrics());
        }
    }

    private void validateSnapshotItems(
            StructureType structureType,
            List<CycleTemplateVersionDomainService.ItemSnapshot> items) {
        if (structureType == StructureType.SINGLE_SEGMENT && items.size() != 1) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_COUNT_INVALID);
        }
        Set<Integer> itemIndexes = new HashSet<>();
        for (CycleTemplateVersionDomainService.ItemSnapshot item : items) {
            if (!itemIndexes.add(item.itemIndex())) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID);
            }
            ItemType itemType = ItemType.fromValue(item.itemType());
            if (itemType == null) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID);
            }
            if (structureType == StructureType.SET_BASED && itemType != ItemType.SET) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID);
            }
            if (structureType == StructureType.SINGLE_SEGMENT && itemType != ItemType.SEGMENT) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID);
            }
            validateSnapshotMetrics(item.metrics());
        }
    }

    private void validateMetrics(List<CycleTemplateMetricRequest> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID, "metrics must not be empty");
        }
        Set<String> metricKeys = new HashSet<>();
        for (CycleTemplateMetricRequest metric : metrics) {
            MetricKey metricKey = MetricKey.fromValue(metric.metricKey());
            if (metricKey == null) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_METRIC_KEY_INVALID);
            }
            if (!metricKeys.add(metric.metricKey())) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_METRIC_DUPLICATE);
            }
            validateMetricValue(metric.metricValueNumber());
        }
    }

    private void validateSnapshotMetrics(List<CycleTemplateVersionDomainService.MetricSnapshot> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_ITEM_INVALID, "persisted metrics must not be empty");
        }
        Set<String> metricKeys = new HashSet<>();
        for (CycleTemplateVersionDomainService.MetricSnapshot metric : metrics) {
            MetricKey metricKey = MetricKey.fromValue(metric.metricKey());
            if (metricKey == null) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_METRIC_KEY_INVALID);
            }
            if (!metricKeys.add(metric.metricKey())) {
                throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_METRIC_DUPLICATE);
            }
            validateMetricValue(metric.metricValueNumber());
        }
    }

    private void validateMetricValue(BigDecimal metricValueNumber) {
        if (metricValueNumber == null || metricValueNumber.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_METRIC_VALUE_INVALID);
        }
    }

    private int countExercises(List<CycleTemplateDayRequest> days) {
        int count = 0;
        for (CycleTemplateDayRequest day : days) {
            count += day.exercises() == null ? 0 : day.exercises().size();
        }
        return count;
    }

    private int countExercises(CycleTemplateVersionDomainService.VersionSnapshot snapshot) {
        int count = 0;
        for (CycleTemplateVersionDomainService.DaySnapshot day : snapshot.days()) {
            count += day.exercises().size();
        }
        return count;
    }
}
