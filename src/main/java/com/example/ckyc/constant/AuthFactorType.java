package com.example.ckyc.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

public enum AuthFactorType {
    DOI("01"),
    MOBILE("03"),
    EMAIL("04"),
    PINCODE("05");

    private final String value;

    AuthFactorType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AuthFactorType fromValue(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(normalized) || type.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseGet(() -> fromLegacyAlias(normalized));
    }

    private static AuthFactorType fromLegacyAlias(String normalized) {
        return switch (normalized) {
            case "01", "DOB" -> DOI;
            case "03", "MOBILE", "MOB" -> MOBILE;
            case "04", "EMAIL" -> EMAIL;
            case "05", "PIN", "PINCODE", "PIN_CODE" -> PINCODE;
            default -> throw new IllegalArgumentException("Unsupported AUTH_FACTOR_TYPE: " + normalized);
        };
    }
}
