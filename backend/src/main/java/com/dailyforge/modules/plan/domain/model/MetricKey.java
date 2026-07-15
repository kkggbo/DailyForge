package com.dailyforge.modules.plan.domain.model;

import java.util.Arrays;

public enum MetricKey {
    WEIGHT_KG("weight_kg", "kg"),
    REPS("reps", "count"),
    DURATION_SECONDS("duration_seconds", "seconds"),
    DISTANCE_KM("distance_km", "km"),
    SPEED_KMH("speed_kmh", "km/h"),
    PACE_SECONDS_PER_KM("pace_seconds_per_km", "sec/km"),
    INCLINE_PERCENT("incline_percent", "percent"),
    REST_SECONDS("rest_seconds", "seconds"),
    RPE("rpe", "rpe"),
    INTENSITY_LEVEL("intensity_level", "level");

    private final String value;
    private final String unit;

    MetricKey(String value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public static MetricKey fromValue(String value) {
        return Arrays.stream(values())
                .filter(key -> key.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
