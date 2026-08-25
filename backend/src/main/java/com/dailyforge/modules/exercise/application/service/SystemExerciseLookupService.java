package com.dailyforge.modules.exercise.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.exercise.infrastructure.persistence.entity.SystemExerciseLookupEntity;
import com.dailyforge.modules.exercise.infrastructure.persistence.mapper.ExerciseQueryMapper;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SystemExerciseLookupService {

    private static final Logger log = LoggerFactory.getLogger(SystemExerciseLookupService.class);

    private final ExerciseQueryMapper exerciseQueryMapper;
    private final SystemExerciseCache systemExerciseCache;

    public SystemExerciseLookupService(
            ExerciseQueryMapper exerciseQueryMapper,
            SystemExerciseCache systemExerciseCache) {
        this.exerciseQueryMapper = exerciseQueryMapper;
        this.systemExerciseCache = systemExerciseCache;
    }

    /**
     * Load raw exercise metadata for internal validators that need to distinguish inactive or custom records.
     */
    public Map<Long, SystemExerciseLookupResult> loadExercisesByIds(Collection<Long> exerciseIds) {
        if (exerciseIds == null || exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> distinctIds = exerciseIds.stream().filter(id -> id != null && id > 0).collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<SystemExerciseLookupEntity> entities = exerciseQueryMapper.selectLookupByIds(List.copyOf(distinctIds));
        Map<Long, SystemExerciseLookupResult> result = new LinkedHashMap<>();
        for (SystemExerciseLookupEntity entity : entities) {
            result.put(entity.getId(), toLookupResult(entity));
        }
        log.debug("Exercise lookup loaded. requestedCount={}, matchedCount={}", distinctIds.size(), result.size());
        return result;
    }

    /**
     * Load active system exercise metadata for internal modules such as plan validation.
     * Delegates to the Redis-backed {@link SystemExerciseCache} and rebuilds the map in memory.
     */
    public Map<Long, SystemExerciseLookupResult> loadActiveSystemExercisesByIds(Collection<Long> exerciseIds) {
        if (exerciseIds == null || exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return systemExerciseCache.loadActive(exerciseIds).stream()
                .collect(Collectors.toMap(
                        SystemExerciseLookupResult::id,
                        result -> result,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    /**
     * Require one active system exercise metadata row. Benefits from the system exercise cache
     * via {@link #loadActiveSystemExercisesByIds}.
     */
    public SystemExerciseLookupResult getRequiredActiveSystemExercise(Long exerciseId) {
        if (exerciseId == null || exerciseId < 1) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
        Map<Long, SystemExerciseLookupResult> result = loadActiveSystemExercisesByIds(List.of(exerciseId));
        SystemExerciseLookupResult lookup = result.get(exerciseId);
        if (lookup == null) {
            throw new BusinessException(ErrorCode.EXERCISE_NOT_FOUND);
        }
        return lookup;
    }

    private SystemExerciseLookupResult toLookupResult(SystemExerciseLookupEntity entity) {
        return new SystemExerciseLookupResult(
                entity.getId(),
                entity.getOwnerUserId(),
                entity.getName(),
                entity.getExerciseType(),
                entity.getMovementType(),
                entity.getDefaultUnit(),
                entity.getDefaultStructureType(),
                entity.getIsActive());
    }
}
