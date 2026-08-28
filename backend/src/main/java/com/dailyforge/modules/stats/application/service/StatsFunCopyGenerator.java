package com.dailyforge.modules.stats.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure-function generator for fun-equivalent copy strings used by the stats module.
 * Constants are hardcoded per the confirmed product decision.
 */
public final class StatsFunCopyGenerator {

    private StatsFunCopyGenerator() {
    }

    /** Reference weight in kg for an adult human. */
    static final BigDecimal ADULT_WEIGHT_KG = new BigDecimal("70");
    /** Reference weight in kg for a small car. */
    static final BigDecimal CAR_WEIGHT_KG = new BigDecimal("1500");
    /** Reference weight in kg for an adult elephant. */
    static final BigDecimal ELEPHANT_WEIGHT_KG = new BigDecimal("5000");
    /** Reference weight in kg for a blue whale. */
    static final BigDecimal BLUE_WHALE_WEIGHT_KG = new BigDecimal("150000");
    /** Reference length in km for a standard athletics track lap. */
    static final BigDecimal TRACK_LAP_KM = new BigDecimal("0.4");
    /** Reference length in km for a marathon. */
    static final BigDecimal MARATHON_KM = new BigDecimal("42.195");
    /** Earth circumference approx in km. */
    static final BigDecimal EARTH_CIRCUMFERENCE_KM = new BigDecimal("40075");

    private static final int SCALE = 2;

    /**
     * Build the overview copy: a summary line plus a fun equivalence line when convertible.
     * Segments with zero or missing volume/distance are omitted; earth-lap line only when distance present.
     */
    public static String buildOverviewCopy(
            int sessionCount,
            BigDecimal totalVolumeKg,
            BigDecimal totalDistanceKm) {
        StringBuilder sb = new StringBuilder();
        sb.append("你从开始运动到现在累计训练 ").append(sessionCount).append(" 场");
        if (totalVolumeKg != null && totalVolumeKg.signum() > 0) {
            sb.append("、总容量 ").append(trim(totalVolumeKg)).append("kg");
        }
        if (totalDistanceKm != null && totalDistanceKm.signum() > 0) {
            sb.append("、总里程 ").append(trim(totalDistanceKm)).append("km");
        }
        sb.append("。");
        BigDecimal laps = earthLaps(totalDistanceKm);
        if (laps != null && laps.signum() > 0) {
            sb.append("总里程相当于绕地球 ").append(trim(laps)).append(" 圈。");
        }
        return sb.toString();
    }

    /**
     * Build the fun copy for a single exercise. Returns an empty string when there is nothing
     * meaningful to show (no reps/volume for strength, or no distance for cardio).
     */
    public static String buildExerciseFunCopy(
            String exerciseName,
            boolean strength,
            int repCount,
            BigDecimal totalVolumeKg,
            BigDecimal totalDistanceKm) {
        if (strength) {
            return buildStrengthCopy(exerciseName, repCount, totalVolumeKg);
        }
        return buildCardioCopy(exerciseName, totalDistanceKm);
    }

    private static String buildStrengthCopy(
            String exerciseName,
            int repCount,
            BigDecimal totalVolumeKg) {
        StringBuilder sb = new StringBuilder();
        if (repCount > 0) {
            sb.append("你已经").append(exerciseName).append(" ").append(repCount).append(" 次");
        }
        boolean volumePositive = totalVolumeKg != null && totalVolumeKg.signum() > 0;
        if (volumePositive) {
            sb.append(sb.length() == 0 ? "你已累计" : "，总容量")
                    .append(volumePositive ? " " + trim(totalVolumeKg) + "kg" : "");
            appendVolumeEquivalence(sb, totalVolumeKg);
        }
        if (sb.length() == 0) {
            return "";
        }
        sb.append("。");
        return sb.toString();
    }

    private static String buildCardioCopy(String exerciseName, BigDecimal totalDistanceKm) {
        if (totalDistanceKm == null || totalDistanceKm.signum() <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("你已经累计").append(exerciseName).append(" ").append(trim(totalDistanceKm)).append("km");
        appendDistanceEquivalence(sb, totalDistanceKm);
        sb.append("。");
        return sb.toString();
    }

    /**
     * Append the volume fun equivalence based on total volume brackets (largest matching bracket).
     */
    private static void appendVolumeEquivalence(StringBuilder sb, BigDecimal totalVolumeKg) {
        if (totalVolumeKg == null || totalVolumeKg.signum() <= 0) {
            return;
        }
        if (totalVolumeKg.compareTo(new BigDecimal("100000")) >= 0) {
            sb.append("，相当于 ").append(trim(totalVolumeKg.divide(BLUE_WHALE_WEIGHT_KG, SCALE, RoundingMode.HALF_UP)))
                    .append(" 头蓝鲸");
        } else if (totalVolumeKg.compareTo(new BigDecimal("20000")) >= 0) {
            sb.append("，相当于 ").append(trim(totalVolumeKg.divide(ELEPHANT_WEIGHT_KG, SCALE, RoundingMode.HALF_UP)))
                    .append(" 头成年大象");
        } else if (totalVolumeKg.compareTo(new BigDecimal("2000")) >= 0) {
            sb.append("，相当于 ").append(trim(totalVolumeKg.divide(CAR_WEIGHT_KG, SCALE, RoundingMode.HALF_UP)))
                    .append(" 辆小汽车");
        } else {
            sb.append("，相当于 ").append(trim(totalVolumeKg.divide(ADULT_WEIGHT_KG, SCALE, RoundingMode.HALF_UP)))
                    .append(" 个成年男子");
        }
    }

    /**
     * Append the distance fun equivalence based on distance brackets (largest matching bracket).
     */
    private static void appendDistanceEquivalence(StringBuilder sb, BigDecimal totalDistanceKm) {
        if (totalDistanceKm == null || totalDistanceKm.signum() <= 0) {
            return;
        }
        if (totalDistanceKm.compareTo(EARTH_CIRCUMFERENCE_KM) >= 0) {
            sb.append("，相当于绕地球 ")
                    .append(trim(totalDistanceKm.divide(EARTH_CIRCUMFERENCE_KM, SCALE, RoundingMode.HALF_UP)))
                    .append(" 圈");
        } else if (totalDistanceKm.compareTo(MARATHON_KM) >= 0) {
            sb.append("，相当于 ")
                    .append(trim(totalDistanceKm.divide(MARATHON_KM, SCALE, RoundingMode.HALF_UP)))
                    .append(" 趟马拉松");
        } else {
            sb.append("，相当于绕标准田径场 ")
                    .append(trim(totalDistanceKm.divide(TRACK_LAP_KM, SCALE, RoundingMode.HALF_UP)))
                    .append(" 圈");
        }
    }

    private static BigDecimal earthLaps(BigDecimal totalDistanceKm) {
        if (totalDistanceKm == null || totalDistanceKm.signum() <= 0) {
            return null;
        }
        return totalDistanceKm.divide(EARTH_CIRCUMFERENCE_KM, SCALE, RoundingMode.HALF_UP);
    }

    private static String trim(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }
}
