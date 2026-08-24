package com.dailyforge.modules.plan.domain.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum MetricKey {
    WEIGHT_KG("weight_kg", "kg", EnumSet.of(StructureType.SET_BASED), false),
    REPS("reps", "count", EnumSet.of(StructureType.SET_BASED), false),
    DURATION_SECONDS("duration_seconds", "seconds",
            EnumSet.of(StructureType.SET_BASED, StructureType.SINGLE_SEGMENT), false),
    DURATION_MINUTES("duration_minutes", "minutes",
            EnumSet.of(StructureType.SET_BASED, StructureType.SINGLE_SEGMENT), false),
    DISTANCE_KM("distance_km", "km", EnumSet.of(StructureType.SINGLE_SEGMENT), false),
    SPEED_KMH("speed_kmh", "km/h", EnumSet.of(StructureType.SINGLE_SEGMENT), false),
    PACE_SECONDS_PER_KM("pace_seconds_per_km", "sec/km", EnumSet.of(StructureType.SINGLE_SEGMENT), false),
    INCLINE_PERCENT("incline_percent", "percent", EnumSet.of(StructureType.SINGLE_SEGMENT), false),
    REST_SECONDS("rest_seconds", "seconds", EnumSet.of(StructureType.SET_BASED), false),
    RPE("rpe", "rpe", EnumSet.of(StructureType.SET_BASED), true),
    INTENSITY_LEVEL("intensity_level", "level", EnumSet.of(StructureType.SINGLE_SEGMENT), false);

    private final String value;
    private final String unit;
    private final Set<StructureType> allowedStructureTypes;
    private final boolean hidden;

    MetricKey(String value, String unit, Set<StructureType> allowedStructureTypes, boolean hidden) {
        this.value = value;
        this.unit = unit;
        this.allowedStructureTypes = allowedStructureTypes;
        this.hidden = hidden;
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public Set<StructureType> getAllowedStructureTypes() {
        return allowedStructureTypes;
    }

    public boolean isHidden() {
        return hidden;
    }

    public static MetricKey fromValue(String value) {
        return Arrays.stream(values())
                .filter(key -> key.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    public static List<MetricKey> allowedFor(StructureType structureType) {
        return Arrays.stream(values())
                .filter(key -> !key.hidden && key.allowedStructureTypes.contains(structureType))
                .toList();
    }
}
