package com.dailyforge.modules.aicoach.infrastructure.ai;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.application.service.AiCycleSummaryService;
import com.dailyforge.modules.aicoach.application.service.AiTemplateGenerationService;
import com.dailyforge.modules.aicoach.infrastructure.persistence.entity.AiTaskRecordEntity;
import com.dailyforge.modules.aicoach.infrastructure.persistence.mapper.AiTaskRecordMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class AiTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiTaskExecutor.class);
    private static final String TEMPLATE = "template_generation";
    private static final String SUMMARY = "cycle_summary";

    private final AiTaskRecordMapper taskMapper;
    private final AiTemplateGenerationService templateService;
    private final AiCycleSummaryService summaryService;
    private final TransactionTemplate tx;

    public AiTaskExecutor(
            AiTaskRecordMapper taskMapper,
            AiTemplateGenerationService templateService,
            AiCycleSummaryService summaryService,
            PlatformTransactionManager transactionManager) {
        this.taskMapper = taskMapper;
        this.templateService = templateService;
        this.summaryService = summaryService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public void execute(Long taskId) {
        if (taskId == null || taskId < 1) {
            return;
        }
        AiTaskRecordEntity task = markRunning(taskId);
        if (task == null) {
            return;
        }
        try {
            if (TEMPLATE.equals(task.getTaskType())) {
                templateService.processTask(taskId);
            } else if (SUMMARY.equals(task.getTaskType())) {
                summaryService.processTask(taskId);
            } else {
                throw new BusinessException(ErrorCode.AI_OUTPUT_INVALID, "unsupported ai task type");
            }
        } catch (BusinessException ex) {
            fail(taskId, ex.getErrorCode(), ex.getMessage());
            log.warn("AI task failed. taskId={}, taskType={}, code={}", taskId, task.getTaskType(), ex.getErrorCode().getCode());
        } catch (Exception ex) {
            fail(taskId, ErrorCode.AI_SERVICE_UNAVAILABLE, ErrorCode.AI_SERVICE_UNAVAILABLE.getDefaultMessage());
            log.error("AI task failed unexpectedly. taskId={}, taskType={}", taskId, task.getTaskType(), ex);
        }
    }

    private AiTaskRecordEntity markRunning(Long taskId) {
        return tx.execute(s -> {
            AiTaskRecordEntity task = taskMapper.selectByIdForUpdate(taskId);
            if (task == null || !"pending".equals(task.getStatus())) {
                return null;
            }
            task.setStatus("running");
            task.setStartedAt(LocalDateTime.now());
            task.setCompletedAt(null);
            task.setErrorCode(null);
            task.setErrorMessage(null);
            taskMapper.updateById(task);
            return task;
        });
    }

    private void fail(Long taskId, ErrorCode code, String message) {
        tx.executeWithoutResult(s -> {
            AiTaskRecordEntity task = taskMapper.selectByIdForUpdate(taskId);
            if (task == null || "succeeded".equals(task.getStatus())) {
                return;
            }
            LocalDateTime completedAt = LocalDateTime.now();
            task.setStatus("failed");
            task.setCompletedAt(completedAt);
            task.setErrorCode(code.getCode());
            task.setErrorMessage(trim(StringUtils.hasText(message) ? message : code.getDefaultMessage()));
            if (task.getStartedAt() != null) {
                task.setLatencyMs((int) Duration.between(task.getStartedAt(), completedAt).toMillis());
            }
            taskMapper.updateById(task);
        });
    }

    private String trim(String value) {
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }
}