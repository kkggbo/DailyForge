package com.dailyforge.modules.diet.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("diet_food_logs")
public class DietFoodLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long foodId;
    private String foodNameSnapshot;
    private String mealType;
    private LocalDate recordDate;
    private BigDecimal quantityGrams;
    private BigDecimal caloriesKcal;
    private BigDecimal proteinG;
    private BigDecimal carbsG;
    private BigDecimal fatG;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public String getFoodNameSnapshot() { return foodNameSnapshot; }
    public void setFoodNameSnapshot(String foodNameSnapshot) { this.foodNameSnapshot = foodNameSnapshot; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public BigDecimal getQuantityGrams() { return quantityGrams; }
    public void setQuantityGrams(BigDecimal quantityGrams) { this.quantityGrams = quantityGrams; }
    public BigDecimal getCaloriesKcal() { return caloriesKcal; }
    public void setCaloriesKcal(BigDecimal caloriesKcal) { this.caloriesKcal = caloriesKcal; }
    public BigDecimal getProteinG() { return proteinG; }
    public void setProteinG(BigDecimal proteinG) { this.proteinG = proteinG; }
    public BigDecimal getCarbsG() { return carbsG; }
    public void setCarbsG(BigDecimal carbsG) { this.carbsG = carbsG; }
    public BigDecimal getFatG() { return fatG; }
    public void setFatG(BigDecimal fatG) { this.fatG = fatG; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
