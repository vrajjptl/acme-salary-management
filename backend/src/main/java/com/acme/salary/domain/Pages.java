package com.acme.salary.domain;

import com.acme.salary.error.ApiExceptions.FieldValidationException;

import java.util.Map;

public final class Pages {
    public static final int MAX_SIZE = 100;

    private Pages() {
    }

    public static int page(int page) {
        if (page < 0) {
            throw new FieldValidationException(Map.of("page", "cannot be negative"));
        }
        if (page > 10_000) {
            throw new FieldValidationException(Map.of("page", "is too large"));
        }
        return page;
    }

    public static int size(int size) {
        if (size < 1) {
            throw new FieldValidationException(Map.of("size", "must be at least 1"));
        }
        if (size > MAX_SIZE) {
            throw new FieldValidationException(Map.of("size", "cannot exceed 100"));
        }
        return size;
    }
}
