package com.dailyforge.modules.plan.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("cycle_day_exercises")
public class CycleDayExerciseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateDayId;
    private Long exerciseId;
    private String exerciseNameSnapshot;
    private Integer targetSets;
    private Integer targetRepsMin;
    private Integer targetRepsMax;
    private BigDecimal targetWeightKg;
    private Integer targetDurationSeconds;
    private Integer targetRestSeconds;
    private BigDecimal targetRpe;
    private String targetExtraJson;
    private String notes;
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateDayId() {
        return templateDayId;
    }

    public void setTemplateDayId(Long templateDayId) {
        this.templateDayId = templateDayId;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getExerciseNameSnapshot() {
        return exerciseNameSnapshot;
    }

    public void setExerciseNameSnapshot(String exerciseNameSnapshot) {
        this.exerciseNameSnapshot = exerciseNameSnapshot;
    }

    public Integer getTargetSets() {
        return targetSets;
    }

    public void setTargetSets(Integer targetSets) {
        this.targetSets = targetSets;
    }

    public Integer getTargetRepsMin() {
        return targetRepsMin;
    }

    public void setTargetRepsMin(Integer targetRepsMin) {
        this.targetRepsMin = targetRepsMin;
    }

    public Integer getTargetRepsMax() {
        return targetRepsMax;
    }

    public void setTargetRepsMax(Integer targetRepsMax) {
        this.targetRepsMax = targetRepsMax;
    }

    public BigDecimal getTargetWeightKg() {
        return targetWeightKg;
    }

    public void setTargetWeightKg(BigDecimal targetWeightKg) {
        this.targetWeightKg = targetWeightKg;
    }

    public Integer getTargetDurationSeconds() {
        return targetDurationSeconds;
    }

    public void setTargetDurationSeconds(Integer targetDurationSeconds) {
        this.targetDurationSeconds = targetDurationSeconds;
    }

    public Integer getTargetRestSeconds() {
        return targetRestSeconds;
    }

    public void setTargetRestSeconds(Integer targetRestSeconds) {
        this.targetRestSeconds = targetRestSeconds;
    }

    public BigDecimal getTargetRpe() {
        return targetRpe;
    }

    public void setTargetRpe(BigDecimal targetRpe) {
        this.targetRpe = targetRpe;
    }

    public String getTargetExtraJson() {
        return targetExtraJson;
    }

    public void setTargetExtraJson(String targetExtraJson) {
        this.targetExtraJson = targetExtraJson;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
