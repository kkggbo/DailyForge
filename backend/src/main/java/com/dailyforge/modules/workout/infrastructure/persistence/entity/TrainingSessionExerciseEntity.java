package com.dailyforge.modules.workout.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("training_session_exercises")
public class TrainingSessionExerciseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long cycleDayExerciseId;
    private Long exerciseId;
    private String exerciseNameSnapshot;
    private String structureType;
    private String exerciseStatus;
    private String feeling;
    private String failureReason;
    private String adjustmentNote;
    private Integer sortOrder;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getSessionId() {
        return sessionId;
    }
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
    public Long getCycleDayExerciseId() {
        return cycleDayExerciseId;
    }
    public void setCycleDayExerciseId(Long cycleDayExerciseId) {
        this.cycleDayExerciseId = cycleDayExerciseId;
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
    public String getStructureType() {
        return structureType;
    }
    public void setStructureType(String structureType) {
        this.structureType = structureType;
    }
    public String getExerciseStatus() {
        return exerciseStatus;
    }
    public void setExerciseStatus(String exerciseStatus) {
        this.exerciseStatus = exerciseStatus;
    }
    public String getFeeling() {
        return feeling;
    }
    public void setFeeling(String feeling) {
        this.feeling = feeling;
    }
    public String getFailureReason() {
        return failureReason;
    }
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    public String getAdjustmentNote() {
        return adjustmentNote;
    }
    public void setAdjustmentNote(String adjustmentNote) {
        this.adjustmentNote = adjustmentNote;
    }
    public Integer getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
