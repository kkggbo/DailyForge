package com.dailyforge.modules.plan.application.service;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.plan.interfaces.dto.AiGenerateDraftCycleTemplateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CycleTemplateAiApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CycleTemplateAiApplicationService.class);

    private final PlanUserSupportService planUserSupportService;

    public CycleTemplateAiApplicationService(PlanUserSupportService planUserSupportService) {
        this.planUserSupportService = planUserSupportService;
    }

    /**
     * Placeholder endpoint for future AI generation.
     */
    public void generateDraft(AiGenerateDraftCycleTemplateRequest request) {
        Long userId = planUserSupportService.requireActiveUserId();
        log.debug("AI draft generate placeholder called. userId={}, useProfileData={}, cycleLength={}",
                userId, request.useProfileData(), request.cycleLength());
        throw new BusinessException(ErrorCode.CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED);
    }
}
