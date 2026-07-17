package com.dailyforge.modules.exercise.infrastructure.persistence.entity;

public class ExerciseMuscleNodeEntity {

    private Long muscleId;
    private String muscleName;
    private String muscleCode;
    private Long parentMuscleId;
    private String parentMuscleName;
    private Integer sortOrder;

    public Long getMuscleId() {
        return muscleId;
    }

    public void setMuscleId(Long muscleId) {
        this.muscleId = muscleId;
    }

    public String getMuscleName() {
        return muscleName;
    }

    public void setMuscleName(String muscleName) {
        this.muscleName = muscleName;
    }

    public String getMuscleCode() {
        return muscleCode;
    }

    public void setMuscleCode(String muscleCode) {
        this.muscleCode = muscleCode;
    }

    public Long getParentMuscleId() {
        return parentMuscleId;
    }

    public void setParentMuscleId(Long parentMuscleId) {
        this.parentMuscleId = parentMuscleId;
    }

    public String getParentMuscleName() {
        return parentMuscleName;
    }

    public void setParentMuscleName(String parentMuscleName) {
        this.parentMuscleName = parentMuscleName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
