package com.example.ckyc.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AuthFactorType {
    OTP("OTP"),
    DOB("DOB"),
    PAN("PAN"),
    MOBILE("MOBILE"),
    EMAIL("EMAIL");

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
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(input.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported AUTH_FACTOR_TYPE: " + input));
    }
}
