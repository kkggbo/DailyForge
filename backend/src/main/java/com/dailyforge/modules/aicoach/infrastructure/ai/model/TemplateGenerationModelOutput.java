package com.dailyforge.modules.aicoach.infrastructure.ai.model;

import com.dailyforge.modules.aicoach.interfaces.vo.TemplateGenerationTaskResultResponse;
import com.dailyforge.modules.plan.interfaces.dto.CycleTemplateDayRequest;
import java.util.List;

public record TemplateGenerationModelOutput(
        String templateName,
        Integer cycleLength,
        List<CycleTemplateDayRequest> days,
        TemplateGenerationTaskResultResponse.GenerationRationale generationRationale) {
}
