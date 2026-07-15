package com.dailyforge.modules.plan.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("cycle_day_exercise_item_metrics")
public class CycleDayExerciseItemMetricEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long exerciseItemId;
    private String metricKey;
    private BigDecimal metricValueNumber;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExerciseItemId() {
        return exerciseItemId;
    }

    public void setExerciseItemId(Long exerciseItemId) {
        this.exerciseItemId = exerciseItemId;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    public BigDecimal getMetricValueNumber() {
        return metricValueNumber;
    }

    public void setMetricValueNumber(BigDecimal metricValueNumber) {
        this.metricValueNumber = metricValueNumber;
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
