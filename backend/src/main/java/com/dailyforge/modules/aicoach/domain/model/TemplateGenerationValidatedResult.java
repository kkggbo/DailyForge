package com.dailyforge.modules.aicoach.domain.model;

import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationTaskResultResponse;
import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest;
import java.util.List;
import java.util.Map;

public record TemplateGenerationValidatedResult(
        String templateName,
        Integer cycleLength,
        List<CycleTemplateDayRequest> days,
        Map<Long, SystemExerciseLookupResult> exerciseMap,
        TemplateGenerationTaskResultResponse.GenerationRationale generationRationale) {
}
