package com.dailyforge.modules.plan.domain.model;

import java.util.Arrays;

public enum ItemType {
    SET("set"),
    SEGMENT("segment");

    private final String value;

    ItemType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ItemType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
