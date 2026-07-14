package com.dailyforge.modules.plan.domain.service;

import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleRunEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.CycleTemplateEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.entity.UserActiveCycleEntity;
import com.dailyforge.modules.plan.infrastructure.persistence.mapper.CycleRunMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class CycleActivationDomainService {

    private final CycleRunMapper cycleRunMapper;

    public CycleActivationDomainService(CycleRunMapper cycleRunMapper) {
        this.cycleRunMapper = cycleRunMapper;
    }

    /**
     * Create a new active run for the target template.
     */
    public CycleRunEntity createNewRun(Long userId, CycleTemplateEntity template) {
        Integer maxRunNo = cycleRunMapper.selectMaxRunNo(userId, template.getId());
        CycleRunEntity run = new CycleRunEntity();
        run.setUserId(userId);
        run.setTemplateId(template.getId());
        run.setTemplateVersionId(template.getCurrentVersionId());
        run.setRunNo((maxRunNo == null ? 0 : maxRunNo) + 1);
        run.setStatus("active");
        run.setStartedAt(LocalDateTime.now());
        cycleRunMapper.insert(run);
        return run;
    }

    /**
     * Overwrite or create current active cycle context.
     */
    public UserActiveCycleEntity buildActiveCycleContext(
            UserActiveCycleEntity existing,
            Long userId,
            CycleTemplateEntity template,
            CycleRunEntity run) {
        UserActiveCycleEntity activeCycle = existing == null ? new UserActiveCycleEntity() : existing;
        activeCycle.setUserId(userId);
        activeCycle.setTemplateId(template.getId());
        activeCycle.setTemplateVersionId(template.getCurrentVersionId());
        activeCycle.setCurrentRunId(run.getId());
        activeCycle.setCurrentDayIndex(1);
        activeCycle.setActivatedAt(LocalDateTime.now());
        return activeCycle;
    }
}
