package com.dailyforge.modules.workout.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("training_sessions")
public class TrainingSessionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long cycleRunId;
    private Long templateId;
    private Long templateVersionId;
    private Long templateDayId;
    private Integer dayIndex;
    private String status;
    private String sessionType;
    private String templateNameSnapshot;
    private String dayNameSnapshot;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String overallFeeling;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public Long getCycleRunId() {
        return cycleRunId;
    }
    public void setCycleRunId(Long cycleRunId) {
        this.cycleRunId = cycleRunId;
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
    public Long getTemplateDayId() {
        return templateDayId;
    }
    public void setTemplateDayId(Long templateDayId) {
        this.templateDayId = templateDayId;
    }
    public Integer getDayIndex() {
        return dayIndex;
    }
    public void setDayIndex(Integer dayIndex) {
        this.dayIndex = dayIndex;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getSessionType() {
        return sessionType;
    }
    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }
    public String getTemplateNameSnapshot() {
        return templateNameSnapshot;
    }
    public void setTemplateNameSnapshot(String templateNameSnapshot) {
        this.templateNameSnapshot = templateNameSnapshot;
    }
    public String getDayNameSnapshot() {
        return dayNameSnapshot;
    }
    public void setDayNameSnapshot(String dayNameSnapshot) {
        this.dayNameSnapshot = dayNameSnapshot;
    }
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    public String getOverallFeeling() {
        return overallFeeling;
    }
    public void setOverallFeeling(String overallFeeling) {
        this.overallFeeling = overallFeeling;
    }
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
