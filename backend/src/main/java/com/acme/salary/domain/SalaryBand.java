package com.acme.salary.domain;

import java.math.BigDecimal;

public record SalaryBand(
        Country country,
        JobLevel level,
        String currency,
        BigDecimal min,
        BigDecimal mid,
        BigDecimal max
) {
}
