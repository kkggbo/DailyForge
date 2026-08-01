package com.dailyforge.modules.aicoach.infrastructure.ai.context;

import com.dailyforge.modules.aicoach.application.service.AiCoachToolSupportService;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import org.springframework.stereotype.Component;

@Component
public class TemplateGenerationContextBuilder {

    private final AiCoachToolSupportService aiCoachToolSupportService;

    public TemplateGenerationContextBuilder(AiCoachToolSupportService aiCoachToolSupportService) {
        this.aiCoachToolSupportService = aiCoachToolSupportService;
    }

    public TemplateGenerationContext build(Long userId, TemplateGenerationRequest request) {
        return new TemplateGenerationContext(
                userId,
                request,
                aiCoachToolSupportService.getUserProfileContext(userId),
                aiCoachToolSupportService.getUserCurrentBodyMetricsContext(userId),
                aiCoachToolSupportService.getTemplateGenerationConstraints());
    }
}
