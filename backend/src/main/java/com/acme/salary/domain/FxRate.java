package com.acme.salary.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FxRate(String currency, BigDecimal usdPerUnit, LocalDate asOf) {
}
