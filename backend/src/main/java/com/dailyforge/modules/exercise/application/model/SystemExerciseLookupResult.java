package com.dailyforge.modules.exercise.application.model;

public record SystemExerciseLookupResult(
        Long id,
        Long ownerUserId,
        String name,
        String exerciseType,
        String movementType,
        String defaultUnit,
        String defaultStructureType,
        Integer isActive) {
}
