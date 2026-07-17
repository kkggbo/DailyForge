package com.dailyforge.modules.exercise.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "System exercise list query")
public class ExerciseSystemListQuery {

    @Schema(description = "Keyword matched against exercise name", example = "bench")
    private String keyword;

    @Schema(description = "Product-level category code", example = "chest")
    private String categoryCode;

    @Schema(description = "Exercise type", example = "strength")
    private String exerciseType;

    @Schema(description = "Movement type", example = "push")
    private String movementType;

    @Schema(description = "Default structure type", example = "set_based")
    private String structureType;

    @Schema(description = "Equipment scene type", example = "gym")
    private String sceneType;

    @Schema(description = "Muscle id filter", example = "3")
    private Long muscleId;

    @Schema(description = "Page number", example = "1", defaultValue = "1")
    private Integer page;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    private Integer pageSize;

    @Schema(hidden = true)
    private List<Long> categoryMuscleIds;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
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

    public String getStructureType() {
        return structureType;
    }

    public void setStructureType(String structureType) {
        this.structureType = structureType;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public Long getMuscleId() {
        return muscleId;
    }

    public void setMuscleId(Long muscleId) {
        this.muscleId = muscleId;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<Long> getCategoryMuscleIds() {
        return categoryMuscleIds;
    }

    public void setCategoryMuscleIds(List<Long> categoryMuscleIds) {
        this.categoryMuscleIds = categoryMuscleIds;
    }

    public int getOffset() {
        return (page - 1) * pageSize;
    }

    public boolean hasFilters() {
        return keyword != null
                || categoryCode != null
                || exerciseType != null
                || movementType != null
                || structureType != null
                || sceneType != null
                || muscleId != null;
    }
}
