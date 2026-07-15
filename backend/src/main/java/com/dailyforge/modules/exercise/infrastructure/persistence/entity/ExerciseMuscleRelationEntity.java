package com.dailyforge.modules.exercise.infrastructure.persistence.entity;

public class ExerciseMuscleRelationEntity {

    private Long exerciseId;
    private Long muscleId;
    private String muscleName;
    private String muscleCode;
    private String relationType;
    private Integer sortOrder;

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

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

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
