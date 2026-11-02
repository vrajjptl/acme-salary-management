package com.acme.salary.domain;

import java.math.BigDecimal;

public enum BandPosition {
    BELOW,
    IN_BAND,
    ABOVE;

    public static BandPosition of(BigDecimal base, BigDecimal min, BigDecimal max) {
        if (base.compareTo(min) < 0) {
            return BELOW;
        }
        if (base.compareTo(max) > 0) {
            return ABOVE;
        }
        return IN_BAND;
    }
}
