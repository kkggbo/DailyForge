package com.dailyforge.modules.profile.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("user_current_body_metrics")
public class UserCurrentBodyMetricsEntity {

    @TableId
    private Long userId;
    private BigDecimal currentWeightKg;
    private BigDecimal currentBodyFatPercent;
    private BigDecimal currentBmi;
    private BigDecimal currentSkeletalMusclePercent;
    private BigDecimal currentBodyWaterPercent;
    private BigDecimal currentBasalMetabolicRateKcal;
    private BigDecimal currentWaistCm;
    private BigDecimal currentHipCm;
    private BigDecimal currentWaistHipRatio;
    private Integer currentBodyAge;
    private String currentBodyType;
    private LocalDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getCurrentWeightKg() {
        return currentWeightKg;
    }

    public void setCurrentWeightKg(BigDecimal currentWeightKg) {
        this.currentWeightKg = currentWeightKg;
    }

    public BigDecimal getCurrentBodyFatPercent() {
        return currentBodyFatPercent;
    }

    public void setCurrentBodyFatPercent(BigDecimal currentBodyFatPercent) {
        this.currentBodyFatPercent = currentBodyFatPercent;
    }

    public BigDecimal getCurrentBmi() {
        return currentBmi;
    }

    public void setCurrentBmi(BigDecimal currentBmi) {
        this.currentBmi = currentBmi;
    }

    public BigDecimal getCurrentSkeletalMusclePercent() {
        return currentSkeletalMusclePercent;
    }

    public void setCurrentSkeletalMusclePercent(BigDecimal currentSkeletalMusclePercent) {
        this.currentSkeletalMusclePercent = currentSkeletalMusclePercent;
    }

    public BigDecimal getCurrentBodyWaterPercent() {
        return currentBodyWaterPercent;
    }

    public void setCurrentBodyWaterPercent(BigDecimal currentBodyWaterPercent) {
        this.currentBodyWaterPercent = currentBodyWaterPercent;
    }

    public BigDecimal getCurrentBasalMetabolicRateKcal() {
        return currentBasalMetabolicRateKcal;
    }

    public void setCurrentBasalMetabolicRateKcal(BigDecimal currentBasalMetabolicRateKcal) {
        this.currentBasalMetabolicRateKcal = currentBasalMetabolicRateKcal;
    }

    public BigDecimal getCurrentWaistCm() {
        return currentWaistCm;
    }

    public void setCurrentWaistCm(BigDecimal currentWaistCm) {
        this.currentWaistCm = currentWaistCm;
    }

    public BigDecimal getCurrentHipCm() {
        return currentHipCm;
    }

    public void setCurrentHipCm(BigDecimal currentHipCm) {
        this.currentHipCm = currentHipCm;
    }

    public BigDecimal getCurrentWaistHipRatio() {
        return currentWaistHipRatio;
    }

    public void setCurrentWaistHipRatio(BigDecimal currentWaistHipRatio) {
        this.currentWaistHipRatio = currentWaistHipRatio;
    }

    public Integer getCurrentBodyAge() {
        return currentBodyAge;
    }

    public void setCurrentBodyAge(Integer currentBodyAge) {
        this.currentBodyAge = currentBodyAge;
    }

    public String getCurrentBodyType() {
        return currentBodyType;
    }

    public void setCurrentBodyType(String currentBodyType) {
        this.currentBodyType = currentBodyType;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
