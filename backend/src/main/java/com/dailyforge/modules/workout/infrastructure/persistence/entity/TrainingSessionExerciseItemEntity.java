package com.dailyforge.modules.workout.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("training_session_exercise_items")
public class TrainingSessionExerciseItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionExerciseId;
    private Integer itemIndex;
    private String itemType;
    private String itemNameSnapshot;
    private String noteSnapshot;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getSessionExerciseId() {
        return sessionExerciseId;
    }
    public void setSessionExerciseId(Long sessionExerciseId) {
        this.sessionExerciseId = sessionExerciseId;
    }
    public Integer getItemIndex() {
        return itemIndex;
    }
    public void setItemIndex(Integer itemIndex) {
        this.itemIndex = itemIndex;
    }
    public String getItemType() {
        return itemType;
    }
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
    public String getItemNameSnapshot() {
        return itemNameSnapshot;
    }
    public void setItemNameSnapshot(String itemNameSnapshot) {
        this.itemNameSnapshot = itemNameSnapshot;
    }
    public String getNoteSnapshot() {
        return noteSnapshot;
    }
    public void setNoteSnapshot(String noteSnapshot) {
        this.noteSnapshot = noteSnapshot;
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
