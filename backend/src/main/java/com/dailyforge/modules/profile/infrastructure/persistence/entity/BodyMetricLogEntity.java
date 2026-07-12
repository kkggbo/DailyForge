package com.dailyforge.modules.profile.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("body_metric_logs")
public class BodyMetricLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate recordDate;
    private BigDecimal weightKg;
    private BigDecimal bodyFatPercent;
    private BigDecimal bmi;
    private BigDecimal skeletalMusclePercent;
    private BigDecimal bodyWaterPercent;
    private BigDecimal basalMetabolicRateKcal;
    private BigDecimal waistCm;
    private BigDecimal hipCm;
    private BigDecimal waistHipRatio;
    private Integer bodyAge;
    private String bodyType;
    private String dataSource;
    private String note;
    private Boolean isDel;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public BigDecimal getBodyFatPercent() {
        return bodyFatPercent;
    }

    public void setBodyFatPercent(BigDecimal bodyFatPercent) {
        this.bodyFatPercent = bodyFatPercent;
    }

    public BigDecimal getBmi() {
        return bmi;
    }

    public void setBmi(BigDecimal bmi) {
        this.bmi = bmi;
    }

    public BigDecimal getSkeletalMusclePercent() {
        return skeletalMusclePercent;
    }

    public void setSkeletalMusclePercent(BigDecimal skeletalMusclePercent) {
        this.skeletalMusclePercent = skeletalMusclePercent;
    }

    public BigDecimal getBodyWaterPercent() {
        return bodyWaterPercent;
    }

    public void setBodyWaterPercent(BigDecimal bodyWaterPercent) {
        this.bodyWaterPercent = bodyWaterPercent;
    }

    public BigDecimal getBasalMetabolicRateKcal() {
        return basalMetabolicRateKcal;
    }

    public void setBasalMetabolicRateKcal(BigDecimal basalMetabolicRateKcal) {
        this.basalMetabolicRateKcal = basalMetabolicRateKcal;
    }

    public BigDecimal getWaistCm() {
        return waistCm;
    }

    public void setWaistCm(BigDecimal waistCm) {
        this.waistCm = waistCm;
    }

    public BigDecimal getHipCm() {
        return hipCm;
    }

    public void setHipCm(BigDecimal hipCm) {
        this.hipCm = hipCm;
    }

    public BigDecimal getWaistHipRatio() {
        return waistHipRatio;
    }

    public void setWaistHipRatio(BigDecimal waistHipRatio) {
        this.waistHipRatio = waistHipRatio;
    }

    public Integer getBodyAge() {
        return bodyAge;
    }

    public void setBodyAge(Integer bodyAge) {
        this.bodyAge = bodyAge;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getIsDel() {
        return isDel;
    }

    public void setIsDel(Boolean isDel) {
        this.isDel = isDel;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
