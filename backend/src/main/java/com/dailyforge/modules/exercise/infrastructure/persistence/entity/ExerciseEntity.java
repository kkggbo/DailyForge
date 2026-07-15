package com.dailyforge.modules.exercise.infrastructure.persistence.entity;

import java.math.BigDecimal;

public class ExerciseEntity {

    private Long id;
    private Long ownerUserId;
    private String name;
    private String exerciseType;
    private String movementType;
    private String videoUrl;
    private String defaultUnit;
    private String defaultStructureType;
    private BigDecimal calorieBurnReference;
    private String calorieReferenceUnit;
    private Integer isActive;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public void setDefaultUnit(String defaultUnit) {
        this.defaultUnit = defaultUnit;
    }

    public String getDefaultStructureType() {
        return defaultStructureType;
    }

    public void setDefaultStructureType(String defaultStructureType) {
        this.defaultStructureType = defaultStructureType;
    }

    public BigDecimal getCalorieBurnReference() {
        return calorieBurnReference;
    }

    public void setCalorieBurnReference(BigDecimal calorieBurnReference) {
        this.calorieBurnReference = calorieBurnReference;
    }

    public String getCalorieReferenceUnit() {
        return calorieReferenceUnit;
    }

    public void setCalorieReferenceUnit(String calorieReferenceUnit) {
        this.calorieReferenceUnit = calorieReferenceUnit;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }
}
