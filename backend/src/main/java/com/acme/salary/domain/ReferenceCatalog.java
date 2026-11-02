package com.acme.salary.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReferenceCatalog {
    public static final LocalDate FX_AS_OF = LocalDate.of(2026, 11, 1);

    private static final Map<String, BigDecimal> FX = fx();
    private static final List<SalaryBand> BANDS = buildBands();
    private static final Map<String, SalaryBand> BAND_INDEX = index(BANDS);

    private ReferenceCatalog() {
    }

    public static List<SalaryBand> bands() {
        return BANDS;
    }

    public static SalaryBand band(Country country, JobLevel level) {
        SalaryBand band = BAND_INDEX.get(key(country, level));
        if (band == null) {
            throw new IllegalStateException("Missing band for " + country + " " + level);
        }
        return band;
    }

    public static List<FxRate> rates() {
        List<FxRate> rates = new ArrayList<>();
        FX.forEach((currency, rate) -> rates.add(new FxRate(currency, rate, FX_AS_OF)));
        return List.copyOf(rates);
    }

    public static BigDecimal usdPerUnit(String currency) {
        BigDecimal rate = FX.get(currency);
        if (rate == null) {
            throw new IllegalStateException("Missing FX rate for " + currency);
        }
        return rate;
    }

    private static Map<String, BigDecimal> fx() {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("USD", new BigDecimal("1.00000000"));
        rates.put("GBP", new BigDecimal("1.27000000"));
        rates.put("EUR", new BigDecimal("1.08000000"));
        rates.put("INR", new BigDecimal("0.01200000"));
        rates.put("SGD", new BigDecimal("0.74000000"));
        rates.put("BRL", new BigDecimal("0.18000000"));
        rates.put("AED", new BigDecimal("0.27230000"));
        rates.put("CAD", new BigDecimal("0.73000000"));
        rates.put("AUD", new BigDecimal("0.66000000"));
        return Map.copyOf(rates);
    }

    private static List<SalaryBand> buildBands() {
        List<SalaryBand> bands = new ArrayList<>();
        for (Country country : Country.values()) {
            BigDecimal rate = FX.get(country.currency());
            for (JobLevel level : JobLevel.values()) {
                BigDecimal usdMid = level.usdMid().multiply(country.payIndex());
                BigDecimal localMid = usdMid.divide(rate, 2, RoundingMode.HALF_UP);
                BigDecimal min = localMid.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal max = localMid.multiply(new BigDecimal("1.20")).setScale(2, RoundingMode.HALF_UP);
                bands.add(new SalaryBand(country, level, country.currency(), min, localMid, max));
            }
        }
        return List.copyOf(bands);
    }

    private static Map<String, SalaryBand> index(List<SalaryBand> bands) {
        Map<String, SalaryBand> index = new LinkedHashMap<>();
        for (SalaryBand band : bands) {
            index.put(key(band.country(), band.level()), band);
        }
        return Map.copyOf(index);
    }

    private static String key(Country country, JobLevel level) {
        return country.name() + ":" + level.name();
    }
}
