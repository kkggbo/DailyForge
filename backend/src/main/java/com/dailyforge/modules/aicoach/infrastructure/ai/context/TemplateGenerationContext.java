package com.dailyforge.modules.aicoach.infrastructure.ai.context;

import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import java.util.Map;

public record TemplateGenerationContext(
        Long userId,
        TemplateGenerationRequest request,
        Map<String, Object> userProfile,
        Map<String, Object> currentBodyMetrics,
        Map<String, Object> recentWorkout,
        Map<String, Object> templateConstraints) {
}
