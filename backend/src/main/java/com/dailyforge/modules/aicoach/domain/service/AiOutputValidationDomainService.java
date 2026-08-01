package com.dailyforge.modules.aicoach.domain.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.domain.model.CycleSummaryValidatedResult;
import com.dailyforge.modules.aicoach.domain.model.TemplateGenerationValidatedResult;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.CycleSummaryModelOutput;
import com.dailyforge.modules.aicoach.infrastructure.ai.model.TemplateGenerationModelOutput;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationTaskResultResponse;
import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.exercise.application.service.SystemExerciseLookupService;
import com.dailyforge.modules.plan.domain.service.CycleTemplatePolicyService;
import com.dailyforge.modules.plan.domain.service.ExerciseStructurePolicyService;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiOutputValidationDomainService {

    private static final Set<String> BASIS_TYPES = Set.of("historical_performance", "starting_recommendation");

    private final ObjectMapper objectMapper;
    private final SystemExerciseLookupService systemExerciseLookupService;
    private final CycleTemplatePolicyService cycleTemplatePolicyService;
    private final ExerciseStructurePolicyService exerciseStructurePolicyService;

    public AiOutputValidationDomainService(
            ObjectMapper objectMapper,
            SystemExerciseLookupService systemExerciseLookupService,
            CycleTemplatePolicyService cycleTemplatePolicyService,
            ExerciseStructurePolicyService exerciseStructurePolicyService) {
        this.objectMapper = objectMapper;
        this.systemExerciseLookupService = systemExerciseLookupService;
        this.cycleTemplatePolicyService = cycleTemplatePolicyService;
        this.exerciseStructurePolicyService = exerciseStructurePolicyService;
    }

    public TemplateGenerationValidatedResult validateTemplateGeneration(
            String json,
            TemplateGenerationRequest request) {
        List<String> errors = new ArrayList<>();
        TemplateGenerationModelOutput output = read(json, TemplateGenerationModelOutput.class, errors);
        if (!errors.isEmpty()) {
            throw invalid(errors);
        }

        if (!StringUtils.hasText(output.templateName())) {
            errors.add("templateName must not be blank");
        }
        if (output.cycleLength() == null || !output.cycleLength().equals(request.cycleLength())) {
            errors.add("cycleLength must equal request.cycleLength");
        }
        if (output.days() == null || output.days().isEmpty()) {
            errors.add("days must not be empty");
        }
        validateGenerationRationale(output.generationRationale(), errors);
        if (!errors.isEmpty()) {
            throw invalid(errors);
        }

        Map<Long, SystemExerciseLookupResult> exerciseMap = loadExerciseMap(output.days(), errors);
        if (!errors.isEmpty()) {
            throw invalid(errors);
        }
        try {
            cycleTemplatePolicyService.validateDraftCycleLength(output.cycleLength());
            cycleTemplatePolicyService.validateDayRequests(output.cycleLength(), output.days());
            exerciseStructurePolicyService.validateDayRequests(output.days(), exerciseMap);
        } catch (BusinessException exception) {
            errors.add(exception.getMessage());
            throw invalid(errors);
        }
        return new TemplateGenerationValidatedResult(
                output.templateName().trim(),
                output.cycleLength(),
                output.days(),
                exerciseMap,
                normalizeGenerationRationale(output.generationRationale()));
    }

    public CycleSummaryValidatedResult validateCycleSummary(String json) {
        List<String> errors = new ArrayList<>();
        CycleSummaryModelOutput output = read(json, CycleSummaryModelOutput.class, errors);
        if (!errors.isEmpty()) {
            throw invalid(errors);
        }
        validateText(output.executionOverview(), "executionOverview", errors);
        validateStringList(output.strengths(), "strengths", errors);
        validateStringList(output.issues(), "issues", errors);
        validateStringList(output.causeAnalysis(), "causeAnalysis", errors);
        validateStringList(output.nextCycleSuggestions(), "nextCycleSuggestions", errors);
        validateStringList(output.risks(), "risks", errors);
        if (!errors.isEmpty()) {
            throw invalid(errors);
        }
        return new CycleSummaryValidatedResult(new CycleSummaryModelOutput(
                output.executionOverview().trim(),
                trimList(output.strengths()),
                trimList(output.issues()),
                trimList(output.causeAnalysis()),
                trimList(output.nextCycleSuggestions()),
                trimList(output.risks()),
                StringUtils.hasText(output.dataCompletenessNotice()) ? output.dataCompletenessNotice().trim() : null));
    }

    public String templateGenerationSchemaDescription() {
        return """
                {
                  "templateName": "non-empty string",
                  "cycleLength": "must equal request cycleLength",
                  "days": "non-empty array of cycle-template day objects",
                  "generationRationale": {
                    "overallDesignSummary": "non-empty string",
                    "dayRationales": "array",
                    "keyExerciseRationales": "array",
                    "intensityRationale": {
                      "basisType": "historical_performance | starting_recommendation",
                      "summary": "non-empty string"
                    },
                    "warnings": "array"
                  }
                }
                """;
    }

    public String cycleSummarySchemaDescription() {
        return """
                {
                  "executionOverview": "non-empty string",
                  "strengths": "non-empty string array",
                  "issues": "non-empty string array",
                  "causeAnalysis": "non-empty string array",
                  "nextCycleSuggestions": "non-empty string array",
                  "risks": "non-empty string array",
                  "dataCompletenessNotice": "string or null"
                }
                """;
    }

    private void validateGenerationRationale(
            TemplateGenerationTaskResultResponse.GenerationRationale rationale,
            List<String> errors) {
        if (rationale == null) {
            errors.add("generationRationale must not be null");
            return;
        }
        validateText(rationale.overallDesignSummary(), "generationRationale.overallDesignSummary", errors);
        if (rationale.dayRationales() == null) {
            errors.add("generationRationale.dayRationales must not be null");
        }
        if (rationale.keyExerciseRationales() == null) {
            errors.add("generationRationale.keyExerciseRationales must not be null");
        }
        if (rationale.intensityRationale() == null) {
            errors.add("generationRationale.intensityRationale must not be null");
            return;
        }
        if (!StringUtils.hasText(rationale.intensityRationale().basisType())
                || !BASIS_TYPES.contains(rationale.intensityRationale().basisType().trim())) {
            errors.add("generationRationale.intensityRationale.basisType is invalid");
        }
        validateText(rationale.intensityRationale().summary(), "generationRationale.intensityRationale.summary", errors);
        if (rationale.warnings() == null) {
            errors.add("generationRationale.warnings must not be null");
        }
    }

    private TemplateGenerationTaskResultResponse.GenerationRationale normalizeGenerationRationale(
            TemplateGenerationTaskResultResponse.GenerationRationale rationale) {
        List<TemplateGenerationTaskResultResponse.DayRationale> dayRationales =
                rationale.dayRationales() == null ? List.of() : rationale.dayRationales().stream()
                        .map(value -> new TemplateGenerationTaskResultResponse.DayRationale(
                                value.dayIndex(),
                                trim(value.dayName()),
                                trim(value.focusSummary()),
                                trim(value.rationale())))
                        .toList();
        List<TemplateGenerationTaskResultResponse.KeyExerciseRationale> keyRationales =
                rationale.keyExerciseRationales() == null ? List.of() : rationale.keyExerciseRationales().stream()
                        .map(value -> new TemplateGenerationTaskResultResponse.KeyExerciseRationale(
                                value.dayIndex(),
                                value.exerciseId(),
                                trim(value.exerciseName()),
                                trim(value.rationale())))
                        .toList();
        List<String> warnings = rationale.warnings() == null ? List.of() : trimList(rationale.warnings());
        return new TemplateGenerationTaskResultResponse.GenerationRationale(
                trim(rationale.overallDesignSummary()),
                dayRationales,
                keyRationales,
                new TemplateGenerationTaskResultResponse.IntensityRationale(
                        trim(rationale.intensityRationale().basisType()),
                        trim(rationale.intensityRationale().summary())),
                warnings);
    }

    private Map<Long, SystemExerciseLookupResult> loadExerciseMap(
            List<CycleTemplateDayRequest> days,
            List<String> errors) {
        Set<Long> exerciseIds = new LinkedHashSet<>();
        for (CycleTemplateDayRequest day : days) {
            if (day == null) {
                errors.add("day entry must not be null");
                continue;
            }
            if (day.exercises() == null) {
                continue;
            }
            day.exercises().forEach(exercise -> {
                if (exercise == null || exercise.exerciseId() == null) {
                    errors.add("exerciseId must not be null");
                } else {
                    exerciseIds.add(exercise.exerciseId());
                }
            });
        }
        if (!errors.isEmpty()) {
            return Map.of();
        }
        Map<Long, SystemExerciseLookupResult> exerciseMap =
                systemExerciseLookupService.loadActiveSystemExercisesByIds(exerciseIds);
        for (Long exerciseId : exerciseIds) {
            if (!exerciseMap.containsKey(exerciseId)) {
                errors.add("exerciseId does not exist in active system exercise library: " + exerciseId);
            }
        }
        return exerciseMap;
    }

    private void validateText(String value, String fieldName, List<String> errors) {
        if (!StringUtils.hasText(value)) {
            errors.add(fieldName + " must not be blank");
        }
    }

    private void validateStringList(List<String> values, String fieldName, List<String> errors) {
        if (values == null || values.isEmpty()) {
            errors.add(fieldName + " must not be empty");
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            if (!StringUtils.hasText(values.get(i))) {
                errors.add(fieldName + "[" + i + "] must not be blank");
            }
        }
    }

    private List<String> trimList(List<String> values) {
        return values.stream().map(this::trim).toList();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private <T> T read(String json, Class<T> type, List<String> errors) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            errors.add("json cannot be parsed into " + type.getSimpleName());
            return null;
        }
    }

    private BusinessException invalid(List<String> errors) {
        return new BusinessException(ErrorCode.AI_OUTPUT_INVALID, String.join("; ", errors));
    }
}
