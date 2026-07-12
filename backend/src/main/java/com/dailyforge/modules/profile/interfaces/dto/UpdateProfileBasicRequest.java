package com.dailyforge.modules.profile.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Partial update request for current user basic profile")
public record UpdateProfileBasicRequest(
        @Schema(description = "Gender", example = "male")
        @Pattern(regexp = "male|female", message = "must be one of: male, female")
        String gender,

        @Schema(description = "Birth date", example = "1998-06-15")
        LocalDate birthDate,

        @Schema(description = "Height in centimeters", example = "178.00")
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        @DecimalMax(value = "300.00", message = "must be less than or equal to 300")
        BigDecimal heightCm,

        @Schema(description = "Goal type", example = "fat_loss")
        @Pattern(regexp = "fat_loss|muscle_gain|health_maintenance",
                message = "must be one of: fat_loss, muscle_gain, health_maintenance")
        String goalType,

        @Schema(description = "Training level", example = "beginner")
        @Pattern(regexp = "beginner|experienced", message = "must be one of: beginner, experienced")
        String trainingLevel,

        @Schema(description = "Injury notes", example = "Old left knee injury, keep squat load conservative")
        @Size(max = 1000, message = "size must be less than or equal to 1000")
        String injuryNotes) {
}
