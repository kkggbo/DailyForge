package com.dailyforge.modules.workout.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("training_session_exercise_item_metrics")
public class TrainingSessionExerciseItemMetricEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionExerciseItemId;
    private String metricKey;
    private BigDecimal plannedValueNumber;
    private BigDecimal actualValueNumber;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getSessionExerciseItemId() {
        return sessionExerciseItemId;
    }
    public void setSessionExerciseItemId(Long sessionExerciseItemId) {
        this.sessionExerciseItemId = sessionExerciseItemId;
    }
    public String getMetricKey() {
        return metricKey;
    }
    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }
    public BigDecimal getPlannedValueNumber() {
        return plannedValueNumber;
    }
    public void setPlannedValueNumber(BigDecimal plannedValueNumber) {
        this.plannedValueNumber = plannedValueNumber;
    }
    public BigDecimal getActualValueNumber() {
        return actualValueNumber;
    }
    public void setActualValueNumber(BigDecimal actualValueNumber) {
        this.actualValueNumber = actualValueNumber;
    }
    public Integer getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
