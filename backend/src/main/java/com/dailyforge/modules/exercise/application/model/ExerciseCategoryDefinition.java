package com.dailyforge.modules.exercise.application.model;

import java.util.List;
import java.util.Set;

/**
 * Fixed product-facing exercise selector category definition.
 */
public record ExerciseCategoryDefinition(
        String categoryCode,
        String categoryName,
        int sortOrder,
        List<String> childMuscleCodes,
        Set<String> filterMuscleCodes) {
}
