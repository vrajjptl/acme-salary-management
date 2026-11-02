package com.acme.salary.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PaySnapshot(
        BigDecimal baseSalaryUsd,
        BigDecimal totalCompUsd,
        BigDecimal compaRatio,
        BandPosition bandPosition,
        BigDecimal bandMin,
        BigDecimal bandMid,
        BigDecimal bandMax
) {
    public static PaySnapshot of(
            BigDecimal base,
            BigDecimal bonus,
            BigDecimal allowances,
            SalaryBand band,
            BigDecimal usdPerUnit
    ) {
        BigDecimal total = base.add(bonus).add(allowances);
        BigDecimal baseUsd = base.multiply(usdPerUnit).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalUsd = total.multiply(usdPerUnit).setScale(2, RoundingMode.HALF_UP);
        BigDecimal compa = base.divide(band.mid(), 4, RoundingMode.HALF_UP);
        return new PaySnapshot(
                baseUsd,
                totalUsd,
                compa,
                BandPosition.of(base, band.min(), band.max()),
                band.min(),
                band.mid(),
                band.max()
        );
    }
}
