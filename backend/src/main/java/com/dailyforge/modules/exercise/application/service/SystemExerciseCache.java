package com.dailyforge.modules.exercise.application.service;

import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.exercise.infrastructure.persistence.entity.SystemExerciseLookupEntity;
import com.dailyforge.modules.exercise.infrastructure.persistence.mapper.ExerciseQueryMapper;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Cached access to the active system exercise library (stable reference data).
 *
 * <p>Returns a {@link List} (a JSON array) rather than a {@code Map<Long, ...>} on purpose:
 * JSON serialization turns map keys into strings, so a Redis round-trip would deserialize them
 * as {@link String} and break {@code get(Long)} lookups. A list round-trips losslessly and the
 * caller rebuilds the map in memory.
 */
@Component
public class SystemExerciseCache {

    private final ExerciseQueryMapper exerciseQueryMapper;

    public SystemExerciseCache(ExerciseQueryMapper exerciseQueryMapper) {
        this.exerciseQueryMapper = exerciseQueryMapper;
    }

    /**
     * Load active system exercises for the given ids, cached in Redis ("systemExercises", 30m).
     * Returns a serialization-safe List; callers build the map in memory.
     */
    @Cacheable(cacheNames = "systemExercises", key = "#exerciseIds")
    public List<SystemExerciseLookupResult> loadActive(Collection<Long> exerciseIds) {
        if (exerciseIds == null || exerciseIds.isEmpty()) {
            return List.of();
        }
        Set<Long> distinctIds = exerciseIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        return exerciseQueryMapper.selectActiveSystemLookupByIds(List.copyOf(distinctIds)).stream()
                .map(this::toLookupResult)
                .toList();
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
