package com.dailyforge.modules.diet.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Pure calculation of a user's daily nutrition target (Mifflin-St Jeor).
 * When any required profile field is missing, it reports the missing fields and no target.
 */
@Service
public class DietTargetDomainService {

    private static final BigDecimal KCAL_FLOOR = new BigDecimal("1200");
    private static final BigDecimal CAL_PER_PROTEIN = new BigDecimal("4");
    private static final BigDecimal CAL_PER_CARB = new BigDecimal("4");
    private static final BigDecimal CAL_PER_FAT = new BigDecimal("9");
    private static final BigDecimal FAT_PERCENT = new BigDecimal("0.25");

    public record ComputedTarget(
            boolean complete,
            List<String> missingFields,
            BigDecimal caloriesKcal,
            BigDecimal proteinG,
            BigDecimal carbsG,
            BigDecimal fatG) {
    }

    /**
     * Compute the target. gender is 'male'/'female'; goalType is fat_loss/muscle_gain/health_maintenance;
     * activityLevel is sedentary/light/moderate/high/very_high.
     */
    public ComputedTarget compute(
            String gender,
            LocalDate birthDate,
            BigDecimal heightCm,
            BigDecimal currentWeightKg,
            String goalType,
            String activityLevel) {
        List<String> missing = new ArrayList<>();
        if (gender == null) {
            missing.add("gender");
        }
        if (birthDate == null) {
            missing.add("birthDate");
        }
        if (heightCm == null) {
            missing.add("heightCm");
        }
        if (currentWeightKg == null) {
            missing.add("currentWeightKg");
        }
        if (goalType == null) {
            missing.add("goalType");
        }
        if (activityLevel == null) {
            missing.add("activityLevel");
        }
        if (!missing.isEmpty()) {
            return new ComputedTarget(false, List.copyOf(missing), null, null, null, null);
        }

        int age = Period.between(birthDate, LocalDate.now()).getYears();
        // BMR = 10*w + 6.25*h - 5*age + (male? +5 : -161)
        BigDecimal bmr = currentWeightKg.multiply(new BigDecimal("10"))
                .add(heightCm.multiply(new BigDecimal("6.25")))
                .subtract(new BigDecimal(age).multiply(new BigDecimal("5")));
        bmr = "male".equalsIgnoreCase(gender) ? bmr.add(new BigDecimal("5")) : bmr.subtract(new BigDecimal("161"));

        BigDecimal tdee = bmr.multiply(activityCoefficient(activityLevel));

        BigDecimal targetKcal = switch (goalType) {
            case "fat_loss" -> tdee.multiply(new BigDecimal("0.85"));
            case "muscle_gain" -> tdee.multiply(new BigDecimal("1.10"));
            default -> tdee;
        };
        if (targetKcal.compareTo(KCAL_FLOOR) < 0) {
            targetKcal = KCAL_FLOOR;
        }

        BigDecimal proteinPerKg = switch (goalType) {
            case "fat_loss" -> new BigDecimal("1.8");
            case "muscle_gain" -> new BigDecimal("2.0");
            default -> new BigDecimal("1.6");
        };
        BigDecimal protein = currentWeightKg.multiply(proteinPerKg);

        BigDecimal fat = targetKcal.multiply(FAT_PERCENT).divide(CAL_PER_FAT, 0, RoundingMode.HALF_UP);

        BigDecimal proteinKcal = protein.multiply(CAL_PER_PROTEIN);
        BigDecimal fatKcal = fat.multiply(CAL_PER_FAT);
        BigDecimal carbKcal = targetKcal.subtract(proteinKcal).subtract(fatKcal);
        BigDecimal carbs = carbKcal.divide(CAL_PER_CARB, 0, RoundingMode.HALF_UP);
        if (carbs.signum() < 0) {
            carbs = BigDecimal.ZERO;
        }

        return new ComputedTarget(
                true,
                List.of(),
                targetKcal,
                protein,
                carbs,
                fat);
    }

    private BigDecimal activityCoefficient(String activityLevel) {
        return switch (activityLevel) {
            case "sedentary" -> new BigDecimal("1.2");
            case "light" -> new BigDecimal("1.375");
            case "moderate" -> new BigDecimal("1.55");
            case "high" -> new BigDecimal("1.725");
            case "very_high" -> new BigDecimal("1.9");
            default -> new BigDecimal("1.2");
        };
    }
}
