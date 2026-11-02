package com.acme.salary.domain;

import java.util.Map;

public final class Enums {
    private Enums() {
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String raw, String field, Map<String, String> errors) {
        if (raw == null || raw.isBlank()) {
            errors.put(field, "is required");
            return null;
        }
        return match(type, raw.trim(), field, errors);
    }

    public static <E extends Enum<E>> E parseOptional(Class<E> type, String raw, String field, Map<String, String> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return match(type, raw.trim(), field, errors);
    }

    private static <E extends Enum<E>> E match(Class<E> type, String raw, String field, Map<String, String> errors) {
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            errors.put(field, "is not a supported value");
            return null;
        }
    }
}
