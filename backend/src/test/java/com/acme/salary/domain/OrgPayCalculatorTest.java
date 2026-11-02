package com.acme.salary.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrgPayCalculatorTest {
    @Test
    void medianAveragesTheTwoMiddleValues() {
        List<BigDecimal> values = List.of(money("1.00"), money("2.00"), money("3.00"), money("4.00"));
        assertThat(OrgPayCalculator.median(values)).isEqualByComparingTo("2.50");
        assertThat(OrgPayCalculator.median(values.subList(0, 3))).isEqualByComparingTo("2.00");
        assertThat(OrgPayCalculator.percentile(values, 0.25)).isEqualByComparingTo("1.00");
        assertThat(OrgPayCalculator.percentile(values, 0.75)).isEqualByComparingTo("3.00");
    }

    @Test
    void emptyWorkforce() {
        OrgPayCalculator.Summary summary = OrgPayCalculator.summarize(List.of(), List.of("L1"));
        assertThat(summary.headcount()).isZero();
        assertThat(summary.medianBaseUsd()).isNull();
        assertThat(summary.outsideBandPercent()).isEqualTo("0.0");
        assertThat(summary.equity()).isEmpty();
        assertThat(summary.suppressedGroups()).isZero();
    }

    @Test
    void equitySuppressesGroupsUnderFiveAfterTheFilter() {
        List<OrgPayCalculator.PayFact> facts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            facts.add(fact("US", "FEMALE", "100.00", BandPosition.IN_BAND));
        }
        for (int i = 0; i < 4; i++) {
            facts.add(fact("US", "MALE", "80.00", BandPosition.BELOW));
        }
        facts.add(fact("DE", "FEMALE", "90.00", BandPosition.ABOVE));
        OrgPayCalculator.Summary all = OrgPayCalculator.summarize(facts, List.of("L3"));
        assertThat(all.equity()).extracting(OrgPayCalculator.EquityRow::group).containsExactly("FEMALE");
        assertThat(all.suppressedGroups()).isEqualTo(1);
        assertThat(all.outsideBandPercent()).isEqualTo("50.0");

        List<OrgPayCalculator.PayFact> germany = facts.stream().filter(fact -> fact.country().equals("DE")).toList();
        OrgPayCalculator.Summary filtered = OrgPayCalculator.summarize(germany, List.of("L3"));
        assertThat(filtered.equity()).isEmpty();
        assertThat(filtered.suppressedGroups()).isEqualTo(1);
    }

    @Test
    void bandPositionIgnoresBonus() {
        SalaryBand band = new SalaryBand(Country.US, JobLevel.L3, "USD", money("80.00"), money("100.00"), money("120.00"));
        PaySnapshot snapshot = PaySnapshot.of(money("100.00"), money("500.00"), money("0.00"), band, BigDecimal.ONE);
        assertThat(snapshot.bandPosition()).isEqualTo(BandPosition.IN_BAND);
        assertThat(snapshot.compaRatio()).isEqualByComparingTo("1.0000");
        assertThat(snapshot.totalCompUsd()).isEqualByComparingTo("600.00");
    }

    private static OrgPayCalculator.PayFact fact(String country, String gender, String base, BandPosition position) {
        return new OrgPayCalculator.PayFact(
                country, country, "ENGINEERING", "Engineering", "L3",
                gender, gender, money(base), money(base), position
        );
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
