package com.dailyforge.modules.exercise.application.service;

import com.dailyforge.modules.exercise.application.model.ExerciseCategoryDefinition;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ExerciseSelectorCategoryService {

    private static final List<ExerciseCategoryDefinition> CATEGORY_DEFINITIONS = List.of(
            new ExerciseCategoryDefinition(
                    "chest",
                    "胸",
                    10,
                    List.of("pectoralis_major_upper", "pectoralis_major_middle", "pectoralis_major_lower"),
                    orderedSet("pectoralis_major", "pectoralis_major_upper", "pectoralis_major_middle",
                            "pectoralis_major_lower")),
            new ExerciseCategoryDefinition(
                    "back",
                    "背",
                    20,
                    List.of("latissimus_dorsi", "trapezius", "rhomboids"),
                    orderedSet("back", "latissimus_dorsi", "trapezius", "rhomboids")),
            new ExerciseCategoryDefinition(
                    "shoulder",
                    "肩",
                    30,
                    List.of("deltoid_front", "deltoid_middle", "deltoid_rear"),
                    orderedSet("deltoid", "deltoid_front", "deltoid_middle", "deltoid_rear")),
            new ExerciseCategoryDefinition(
                    "arms",
                    "手臂",
                    40,
                    List.of("biceps_brachii", "triceps_brachii", "forearm"),
                    orderedSet("biceps_brachii", "triceps_brachii", "forearm")),
            new ExerciseCategoryDefinition(
                    "legs",
                    "腿",
                    50,
                    List.of("gluteus_maximus", "gluteus_medius", "quadriceps", "hamstrings", "adductors",
                            "gastrocnemius", "soleus"),
                    orderedSet("glutes", "gluteus_maximus", "gluteus_medius", "legs", "quadriceps", "hamstrings",
                            "adductors", "calves", "gastrocnemius", "soleus")),
            new ExerciseCategoryDefinition(
                    "core",
                    "核心",
                    60,
                    List.of("rectus_abdominis", "obliques", "erector_spinae"),
                    orderedSet("core", "rectus_abdominis", "obliques", "erector_spinae")),
            new ExerciseCategoryDefinition(
                    "cardio",
                    "有氧",
                    70,
                    List.of("cardio"),
                    orderedSet("cardio")));

    public List<ExerciseCategoryDefinition> getCategoryDefinitions() {
        return CATEGORY_DEFINITIONS;
    }

    public boolean isSupportedCategoryCode(String categoryCode) {
        return CATEGORY_DEFINITIONS.stream().anyMatch(definition -> definition.categoryCode().equals(categoryCode));
    }

    public ExerciseCategoryDefinition getRequiredCategoryDefinition(String categoryCode) {
        return CATEGORY_DEFINITIONS.stream()
                .filter(definition -> definition.categoryCode().equals(categoryCode))
                .findFirst()
                .orElse(null);
    }

    public Set<String> getAllChildMuscleCodes() {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (ExerciseCategoryDefinition definition : CATEGORY_DEFINITIONS) {
            codes.addAll(definition.childMuscleCodes());
        }
        return codes;
    }

    private static Set<String> orderedSet(String... values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }
}
