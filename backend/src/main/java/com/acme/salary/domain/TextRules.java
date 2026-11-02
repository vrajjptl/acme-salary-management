package com.acme.salary.domain;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class TextRules {
    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");

    private TextRules() {
    }

    public static String required(String value, String field, int min, int max, Map<String, String> errors) {
        return clean(value, field, min, max, true, errors);
    }

    public static String optional(String value, String field, int min, int max, Map<String, String> errors) {
        return clean(value, field, min, max, false, errors);
    }

    public static String email(String value, Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            errors.put("email", "is required");
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.length() > 254 || !EMAIL.matcher(trimmed).matches()) {
            errors.put("email", "must be a valid email address");
            return null;
        }
        return trimmed;
    }

    private static String clean(String value, String field, int min, int max, boolean required, Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            if (required) {
                errors.put(field, "is required");
            }
            return null;
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.chars().anyMatch(ch -> ch < 32 || ch == 127 || ch == '<' || ch == '>')) {
            errors.put(field, "contains unsupported characters");
            return null;
        }
        if (trimmed.length() < min) {
            errors.put(field, "must be at least " + min + " characters");
            return null;
        }
        if (trimmed.length() > max) {
            errors.put(field, "must be at most " + max + " characters");
            return null;
        }
        return trimmed;
    }
}
