package com.acme.salary.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class OrgPayCalculator {
    public static final int MINIMUM_GROUP_SIZE = 5;

    private OrgPayCalculator() {
    }

    public record PayFact(
            String country,
            String countryLabel,
            String department,
            String departmentLabel,
            String level,
            String gender,
            String genderLabel,
            BigDecimal baseUsd,
            BigDecimal totalUsd,
            BandPosition bandPosition
    ) {
    }

    public record Breakdown(
            String key,
            String label,
            int headcount,
            String totalBaseUsd,
            String medianBaseUsd,
            String meanBaseUsd,
            int belowBand,
            int inBand,
            int aboveBand
    ) {
    }

    public record EquityRow(
            String group,
            String label,
            int headcount,
            String medianBaseUsd,
            String meanBaseUsd
    ) {
    }

    public record Summary(
            int headcount,
            String totalBaseUsd,
            String totalCompUsd,
            String medianBaseUsd,
            String meanBaseUsd,
            String p25BaseUsd,
            String p75BaseUsd,
            int belowBand,
            int inBand,
            int aboveBand,
            String outsideBandPercent,
            List<Breakdown> byCountry,
            List<Breakdown> byDepartment,
            List<Breakdown> byLevel,
            List<EquityRow> equity,
            int suppressedGroups,
            int minimumGroupSize
    ) {
    }

    public static Summary summarize(List<PayFact> facts, List<String> levelOrder) {
        List<BigDecimal> bases = facts.stream().map(PayFact::baseUsd).sorted().toList();
        int below = count(facts, BandPosition.BELOW);
        int above = count(facts, BandPosition.ABOVE);
        int inBand = count(facts, BandPosition.IN_BAND);
        int headcount = facts.size();
        BigDecimal totalBase = sum(facts, PayFact::baseUsd);
        BigDecimal totalComp = sum(facts, PayFact::totalUsd);
        Equity equity = equity(facts);

        List<Breakdown> levels = breakdown(facts, PayFact::level, PayFact::level);
        levels.sort(Comparator.comparingInt(row -> {
            int index = levelOrder.indexOf(row.key());
            return index < 0 ? Integer.MAX_VALUE : index;
        }));

        return new Summary(
                headcount,
                money(totalBase),
                money(totalComp),
                money(median(bases)),
                money(mean(bases)),
                money(percentile(bases, 0.25)),
                money(percentile(bases, 0.75)),
                below,
                inBand,
                above,
                outsidePercent(below + above, headcount),
                sortByTotal(breakdown(facts, PayFact::country, PayFact::countryLabel)),
                sortByTotal(breakdown(facts, PayFact::department, PayFact::departmentLabel)),
                levels,
                equity.rows(),
                equity.suppressed(),
                MINIMUM_GROUP_SIZE
        );
    }

    public static BigDecimal median(List<BigDecimal> sorted) {
        if (sorted.isEmpty()) {
            return null;
        }
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2).setScale(2, RoundingMode.HALF_UP);
        }
        return sorted.get(size / 2 - 1).add(sorted.get(size / 2))
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentile(List<BigDecimal> sorted, double p) {
        if (sorted.isEmpty()) {
            return null;
        }
        int rank = (int) Math.ceil(p * sorted.size());
        rank = Math.max(1, Math.min(sorted.size(), rank));
        return sorted.get(rank - 1).setScale(2, RoundingMode.HALF_UP);
    }

    private static Equity equity(List<PayFact> facts) {
        Map<String, List<PayFact>> grouped = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        for (Gender gender : Gender.values()) {
            grouped.put(gender.name(), new ArrayList<>());
            labels.put(gender.name(), gender.label());
        }
        for (PayFact fact : facts) {
            grouped.computeIfAbsent(fact.gender(), key -> new ArrayList<>()).add(fact);
            labels.putIfAbsent(fact.gender(), fact.genderLabel());
        }
        List<EquityRow> rows = new ArrayList<>();
        int suppressed = 0;
        for (Map.Entry<String, List<PayFact>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            if (entry.getValue().size() < MINIMUM_GROUP_SIZE) {
                suppressed++;
                continue;
            }
            List<BigDecimal> bases = entry.getValue().stream().map(PayFact::baseUsd).sorted().toList();
            rows.add(new EquityRow(
                    entry.getKey(),
                    labels.get(entry.getKey()),
                    entry.getValue().size(),
                    money(median(bases)),
                    money(mean(bases))
            ));
        }
        return new Equity(rows, suppressed);
    }

    private static List<Breakdown> breakdown(
            List<PayFact> facts,
            Function<PayFact, String> keyFn,
            Function<PayFact, String> labelFn
    ) {
        Map<String, List<PayFact>> grouped = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        for (PayFact fact : facts) {
            grouped.computeIfAbsent(keyFn.apply(fact), key -> new ArrayList<>()).add(fact);
            labels.putIfAbsent(keyFn.apply(fact), labelFn.apply(fact));
        }
        List<Breakdown> rows = new ArrayList<>();
        for (Map.Entry<String, List<PayFact>> entry : grouped.entrySet()) {
            List<BigDecimal> bases = entry.getValue().stream().map(PayFact::baseUsd).sorted().toList();
            rows.add(new Breakdown(
                    entry.getKey(),
                    labels.get(entry.getKey()),
                    entry.getValue().size(),
                    money(sum(entry.getValue(), PayFact::baseUsd)),
                    money(median(bases)),
                    money(mean(bases)),
                    count(entry.getValue(), BandPosition.BELOW),
                    count(entry.getValue(), BandPosition.IN_BAND),
                    count(entry.getValue(), BandPosition.ABOVE)
            ));
        }
        return rows;
    }

    private static List<Breakdown> sortByTotal(List<Breakdown> rows) {
        return rows.stream()
                .sorted(Comparator.comparing((Breakdown row) -> new BigDecimal(row.totalBaseUsd())).reversed()
                        .thenComparing(Breakdown::key))
                .toList();
    }

    private static int count(List<PayFact> facts, BandPosition position) {
        int count = 0;
        for (PayFact fact : facts) {
            if (fact.bandPosition() == position) {
                count++;
            }
        }
        return count;
    }

    private static BigDecimal sum(List<PayFact> facts, Function<PayFact, BigDecimal> amount) {
        BigDecimal total = BigDecimal.ZERO;
        for (PayFact fact : facts) {
            total = total.add(amount.apply(fact));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal mean(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            total = total.add(value);
        }
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String outsidePercent(int outside, int headcount) {
        if (headcount == 0) {
            return "0.0";
        }
        return BigDecimal.valueOf(outside)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(headcount), 1, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private record Equity(List<EquityRow> rows, int suppressed) {
    }
}
