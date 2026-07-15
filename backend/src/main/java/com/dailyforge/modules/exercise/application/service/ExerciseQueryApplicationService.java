package com.dailyforge.modules.exercise.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.exercise.application.assembler.ExerciseAssembler;
import com.dailyforge.modules.exercise.domain.service.ExerciseQueryPolicyService;
import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseEntity;
import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseEquipmentRelationEntity;
import com.dailyforge.modules.exercise.infrastructure.persistence.entity.ExerciseMuscleRelationEntity;
import com.dailyforge.modules.exercise.infrastructure.persistence.mapper.ExerciseEquipmentQueryMapper;
import com.dailyforge.modules.exercise.infrastructure.persistence.mapper.ExerciseMuscleQueryMapper;
import com.dailyforge.modules.exercise.infrastructure.persistence.mapper.ExerciseQueryMapper;
import com.dailyforge.modules.exercise.interfaces.dto.ExerciseSystemListQuery;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseEquipmentResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseMuscleResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseSystemDetailResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseSystemListItemResponse;
import com.dailyforge.modules.exercise.interfaces.vo.ExerciseSystemListResponse;
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
public class ExerciseQueryApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseQueryApplicationService.class);

    private final ExerciseQueryMapper exerciseQueryMapper;
    private final ExerciseMuscleQueryMapper exerciseMuscleQueryMapper;
    private final ExerciseEquipmentQueryMapper exerciseEquipmentQueryMapper;
    private final ExerciseQueryPolicyService exerciseQueryPolicyService;
    private final ExerciseAssembler exerciseAssembler;

    public ExerciseQueryApplicationService(
            ExerciseQueryMapper exerciseQueryMapper,
            ExerciseMuscleQueryMapper exerciseMuscleQueryMapper,
            ExerciseEquipmentQueryMapper exerciseEquipmentQueryMapper,
            ExerciseQueryPolicyService exerciseQueryPolicyService,
            ExerciseAssembler exerciseAssembler) {
        this.exerciseQueryMapper = exerciseQueryMapper;
        this.exerciseMuscleQueryMapper = exerciseMuscleQueryMapper;
        this.exerciseEquipmentQueryMapper = exerciseEquipmentQueryMapper;
        this.exerciseQueryPolicyService = exerciseQueryPolicyService;
        this.exerciseAssembler = exerciseAssembler;
    }

    /**
     * Return one page of system exercises with lightweight aggregated muscle and equipment names.
     */
    public ExerciseSystemListResponse getSystemExercises(ExerciseSystemListQuery query, Long userId) {
        exerciseQueryPolicyService.normalizeListQuery(query);
        long total = exerciseQueryMapper.countSystemExercises(query);
        if (total == 0) {
            log.debug("System exercise list loaded. userId={}, page={}, pageSize={}, total=0, recordCount=0",
                    userId, query.getPage(), query.getPageSize());
            return exerciseAssembler.toListResponse(query.getPage(), query.getPageSize(), 0L, Collections.emptyList());
        }

        List<Long> pageIds = exerciseQueryMapper.selectSystemExercisePageIds(query);
        if (pageIds.isEmpty()) {
            log.debug("System exercise list loaded. userId={}, page={}, pageSize={}, total={}, recordCount=0",
                    userId, query.getPage(), query.getPageSize(), total);
            return exerciseAssembler.toListResponse(query.getPage(), query.getPageSize(), total, Collections.emptyList());
        }

        // Query ids first, then batch aggregate one-to-many relations to keep pagination accurate.
        List<ExerciseEntity> exercises = exerciseQueryMapper.selectSystemExercisesByIds(pageIds);
        Map<Long, Integer> orderMap = buildOrderMap(pageIds);
        exercises.sort(Comparator.comparingInt(entity -> orderMap.getOrDefault(entity.getId(), Integer.MAX_VALUE)));

        Map<Long, List<ExerciseMuscleRelationEntity>> muscleMap =
                groupMusclesByExerciseId(exerciseMuscleQueryMapper.selectByExerciseIds(pageIds));
        Map<Long, List<ExerciseEquipmentRelationEntity>> equipmentMap =
                groupEquipmentsByExerciseId(exerciseEquipmentQueryMapper.selectByExerciseIds(pageIds));

        List<ExerciseSystemListItemResponse> records = new ArrayList<>();
        for (ExerciseEntity entity : exercises) {
            List<ExerciseMuscleRelationEntity> muscles =
                    muscleMap.getOrDefault(entity.getId(), Collections.emptyList());
            List<ExerciseEquipmentRelationEntity> equipments =
                    equipmentMap.getOrDefault(entity.getId(), Collections.emptyList());
            records.add(exerciseAssembler.toListItemResponse(
                    entity,
                    extractMuscleNames(muscles, "primary"),
                    extractMuscleNames(muscles, "secondary"),
                    extractEquipmentNames(equipments)));
        }

        log.debug("System exercise list loaded. userId={}, page={}, pageSize={}, total={}, recordCount={}",
                userId, query.getPage(), query.getPageSize(), total, records.size());
        return exerciseAssembler.toListResponse(query.getPage(), query.getPageSize(), total, records);
    }

    /**
     * Return one system exercise detail with full structured muscle and equipment objects.
     */
    public ExerciseSystemDetailResponse getSystemExerciseDetail(Long exerciseId, Long userId) {
        exerciseQueryPolicyService.validateExerciseId(exerciseId);
        ExerciseEntity entity = exerciseQueryMapper.selectSystemExerciseDetailById(exerciseId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.EXERCISE_NOT_FOUND);
        }

        List<ExerciseMuscleRelationEntity> muscles = exerciseMuscleQueryMapper.selectByExerciseId(exerciseId);
        List<ExerciseEquipmentRelationEntity> equipments = exerciseEquipmentQueryMapper.selectByExerciseId(exerciseId);
        List<ExerciseMuscleResponse> primaryMuscles = new ArrayList<>();
        List<ExerciseMuscleResponse> secondaryMuscles = new ArrayList<>();
        for (ExerciseMuscleRelationEntity muscle : muscles) {
            ExerciseMuscleResponse response = exerciseAssembler.toMuscleResponse(muscle);
            if ("primary".equals(muscle.getRelationType())) {
                primaryMuscles.add(response);
            } else if ("secondary".equals(muscle.getRelationType())) {
                secondaryMuscles.add(response);
            }
        }
        List<ExerciseEquipmentResponse> equipmentResponses = new ArrayList<>();
        for (ExerciseEquipmentRelationEntity equipment : equipments) {
            equipmentResponses.add(exerciseAssembler.toEquipmentResponse(equipment));
        }

        log.debug(
                "System exercise detail loaded. userId={}, exerciseId={}, primaryMuscleCount={}, secondaryMuscleCount={}, equipmentCount={}",
                userId,
                exerciseId,
                primaryMuscles.size(),
                secondaryMuscles.size(),
                equipmentResponses.size());
        return exerciseAssembler.toDetailResponse(entity, primaryMuscles, secondaryMuscles, equipmentResponses);
    }

    private Map<Long, Integer> buildOrderMap(List<Long> ids) {
        Map<Long, Integer> orderMap = new LinkedHashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            orderMap.put(ids.get(i), i);
        }
        return orderMap;
    }

    private Map<Long, List<ExerciseMuscleRelationEntity>> groupMusclesByExerciseId(
            List<ExerciseMuscleRelationEntity> relations) {
        Map<Long, List<ExerciseMuscleRelationEntity>> result = new LinkedHashMap<>();
        for (ExerciseMuscleRelationEntity relation : relations) {
            result.computeIfAbsent(relation.getExerciseId(), ignored -> new ArrayList<>()).add(relation);
        }
        return result;
    }

    private Map<Long, List<ExerciseEquipmentRelationEntity>> groupEquipmentsByExerciseId(
            List<ExerciseEquipmentRelationEntity> relations) {
        Map<Long, List<ExerciseEquipmentRelationEntity>> result = new LinkedHashMap<>();
        for (ExerciseEquipmentRelationEntity relation : relations) {
            result.computeIfAbsent(relation.getExerciseId(), ignored -> new ArrayList<>()).add(relation);
        }
        return result;
    }

    private List<String> extractMuscleNames(List<ExerciseMuscleRelationEntity> relations, String relationType) {
        List<String> names = new ArrayList<>();
        for (ExerciseMuscleRelationEntity relation : relations) {
            if (relationType.equals(relation.getRelationType())) {
                names.add(relation.getMuscleName());
            }
        }
        return names;
    }

    private List<String> extractEquipmentNames(List<ExerciseEquipmentRelationEntity> relations) {
        List<String> names = new ArrayList<>();
        for (ExerciseEquipmentRelationEntity relation : relations) {
            names.add(relation.getEquipmentName());
        }
        return names;
    }
}
