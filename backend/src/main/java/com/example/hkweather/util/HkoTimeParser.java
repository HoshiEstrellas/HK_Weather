package com.example.hkweather.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class HkoTimeParser {

    private static final DateTimeFormatter HKO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private HkoTimeParser() {
    }

    public static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, HKO_TIMESTAMP);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
