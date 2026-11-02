package com.acme.salary.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Pattern;

public final class MoneyText {
    public static final BigDecimal MAX = new BigDecimal("999999999.99");
    private static final Pattern PATTERN = Pattern.compile("\\d{1,9}(\\.\\d{1,2})?");

    private MoneyText() {
    }

    public static BigDecimal parse(String raw, String field, boolean mustBePositive, Map<String, String> errors) {
        if (raw == null || raw.isBlank()) {
            errors.put(field, "is required");
            return null;
        }
        String trimmed = raw.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            errors.put(field, "must be a decimal with up to 2 places and no sign");
            return null;
        }
        BigDecimal value = new BigDecimal(trimmed).setScale(2, RoundingMode.UNNECESSARY);
        if (mustBePositive && value.signum() <= 0) {
            errors.put(field, "must be greater than zero");
            return null;
        }
        if (!mustBePositive && value.signum() < 0) {
            errors.put(field, "cannot be negative");
            return null;
        }
        if (value.compareTo(MAX) > 0) {
            errors.put(field, "exceeds the maximum allowed");
            return null;
        }
        return value;
    }

    public static String format(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String formatRatio(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    public static String formatRate(BigDecimal value) {
        return value.setScale(8, RoundingMode.HALF_UP).toPlainString();
    }
}
