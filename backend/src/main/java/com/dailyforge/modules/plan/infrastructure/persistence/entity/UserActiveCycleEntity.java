package com.dailyforge.modules.plan.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_active_cycles")
public class UserActiveCycleEntity {

    @TableId
    private Long userId;
    private Long templateId;
    private Long templateVersionId;
    private Long currentRunId;
    private Integer currentDayIndex;
    private Long lastSessionId;
    private LocalDateTime activatedAt;
    private LocalDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getTemplateVersionId() {
        return templateVersionId;
    }

    public void setTemplateVersionId(Long templateVersionId) {
        this.templateVersionId = templateVersionId;
    }

    public Long getCurrentRunId() {
        return currentRunId;
    }

    public void setCurrentRunId(Long currentRunId) {
        this.currentRunId = currentRunId;
    }

    public Integer getCurrentDayIndex() {
        return currentDayIndex;
    }

    public void setCurrentDayIndex(Integer currentDayIndex) {
        this.currentDayIndex = currentDayIndex;
    }

    public Long getLastSessionId() {
        return lastSessionId;
    }

    public void setLastSessionId(Long lastSessionId) {
        this.lastSessionId = lastSessionId;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
