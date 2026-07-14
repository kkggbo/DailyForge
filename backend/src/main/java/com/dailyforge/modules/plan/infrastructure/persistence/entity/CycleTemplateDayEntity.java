package com.dailyforge.modules.plan.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("cycle_template_days")
public class CycleTemplateDayEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateVersionId;
    private Integer dayIndex;
    private String dayName;
    private String focus;
    private String notes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateVersionId() {
        return templateVersionId;
    }

    public void setTemplateVersionId(Long templateVersionId) {
        this.templateVersionId = templateVersionId;
    }

    public Integer getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(Integer dayIndex) {
        this.dayIndex = dayIndex;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    public String getFocus() {
        return focus;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
