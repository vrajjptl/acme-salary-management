package com.acme.salary.domain;

import java.math.BigDecimal;

public enum Country {
    US("United States", "USD", "1.00"),
    GB("United Kingdom", "GBP", "0.95"),
    DE("Germany", "EUR", "0.92"),
    IN("India", "INR", "0.28"),
    SG("Singapore", "SGD", "0.90"),
    BR("Brazil", "BRL", "0.35"),
    AE("United Arab Emirates", "AED", "0.85"),
    CA("Canada", "CAD", "0.88"),
    AU("Australia", "AUD", "0.90"),
    NL("Netherlands", "EUR", "0.94");

    private final String label;
    private final String currency;
    private final BigDecimal payIndex;

    Country(String label, String currency, String payIndex) {
        this.label = label;
        this.currency = currency;
        this.payIndex = new BigDecimal(payIndex);
    }

    public String label() {
        return label;
    }

    public String currency() {
        return currency;
    }

    public BigDecimal payIndex() {
        return payIndex;
    }
}
