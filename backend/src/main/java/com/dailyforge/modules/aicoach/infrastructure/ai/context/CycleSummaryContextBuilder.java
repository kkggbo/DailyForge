package com.dailyforge.modules.aicoach.infrastructure.ai.context;

import com.dailyforge.modules.aicoach.application.service.AiCoachToolSupportService;
import com.dailyforge.modules.aicoach.interfaces.dto.CycleSummaryRequest;
import com.dailyforge.modules.plan.domain.service.CycleTemplateVersionDomainService;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleRunMapper;
import org.springframework.stereotype.Component;

@Component
public class CycleSummaryContextBuilder {

    private final AiCoachToolSupportService aiCoachToolSupportService;
    private final CycleRunMapper cycleRunMapper;
    private final CycleTemplateVersionDomainService cycleTemplateVersionDomainService;

    public CycleSummaryContextBuilder(
            AiCoachToolSupportService aiCoachToolSupportService,
            CycleRunMapper cycleRunMapper,
            CycleTemplateVersionDomainService cycleTemplateVersionDomainService) {
        this.aiCoachToolSupportService = aiCoachToolSupportService;
        this.cycleRunMapper = cycleRunMapper;
        this.cycleTemplateVersionDomainService = cycleTemplateVersionDomainService;
    }

    public CycleSummaryContext build(Long userId, CycleSummaryRequest request, Long cycleRunId) {
        CycleRunEntity cycleRun = cycleRunMapper.selectById(cycleRunId);
        return new CycleSummaryContext(
                userId,
                cycleRunId,
                request,
                aiCoachToolSupportService.getUserProfileContext(userId),
                aiCoachToolSupportService.getUserCurrentBodyMetricsContext(userId),
                aiCoachToolSupportService.getCycleRunSummary(userId, cycleRunId),
                aiCoachToolSupportService.getCycleRunAggregatedAnalysis(userId, cycleRunId),
                cycleRun == null
                        ? java.util.Map.of()
                        : aiCoachToolSupportService.versionSnapshotToSummary(
                                cycleTemplateVersionDomainService.loadVersionSnapshot(cycleRun.getTemplateVersionId())));
    }
}
