package com.acme.salary.domain;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Pattern;

public final class Dates {
    private static final Pattern ISO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private Dates() {
    }

    public static LocalDate parse(String raw, String field, Map<String, String> errors) {
        if (raw == null || raw.isBlank()) {
            errors.put(field, "is required");
            return null;
        }
        return parsePresent(raw.trim(), field, errors);
    }

    public static LocalDate parseOptional(String raw, String field, Map<String, String> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parsePresent(raw.trim(), field, errors);
    }

    private static LocalDate parsePresent(String raw, String field, Map<String, String> errors) {
        if (!ISO.matcher(raw).matches()) {
            errors.put(field, "must be a date in YYYY-MM-DD form");
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            errors.put(field, "must be a real calendar date");
            return null;
        }
    }
}
