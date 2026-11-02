package com.acme.salary.domain;

import com.acme.salary.error.ApiExceptions.FieldValidationException;

import java.util.Locale;
import java.util.Map;

public final class SearchTerms {
    private SearchTerms() {
    }

    public static String contains(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 80) {
            throw new FieldValidationException(Map.of("q", "must be 80 characters or fewer"));
        }
        String cleaned = trimmed.replace("%", "").replace("_", "").replace("\\", "");
        if (cleaned.isBlank()) {
            return null;
        }
        return "%" + cleaned.toLowerCase(Locale.ROOT) + "%";
    }
}
