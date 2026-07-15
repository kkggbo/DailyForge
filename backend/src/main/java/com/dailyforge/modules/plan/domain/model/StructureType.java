package com.dailyforge.modules.plan.domain.model;

import java.util.Arrays;

public enum StructureType {
    SET_BASED("set_based"),
    SINGLE_SEGMENT("single_segment");

    private final String value;

    StructureType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StructureType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
