package com.dailyforge.modules.profile.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Current user basic profile response")
public record ProfileBasicResponse(
        @Schema(description = "Gender", example = "male") String gender,
        @Schema(description = "Birth date", example = "1998-06-15") LocalDate birthDate,
        @Schema(description = "Height in centimeters", example = "178.00") BigDecimal heightCm,
        @Schema(description = "Goal type", example = "fat_loss") String goalType,
        @Schema(description = "Training level", example = "beginner") String trainingLevel,
        @Schema(description = "Injury notes", example = "Old left knee injury") String injuryNotes,
        @Schema(description = "Current weight from snapshot", example = "76.50") BigDecimal currentWeightKg,
        @Schema(description = "Latest active body metric record date", example = "2026-07-12")
        LocalDate latestBodyMetricRecordDate) {
}
