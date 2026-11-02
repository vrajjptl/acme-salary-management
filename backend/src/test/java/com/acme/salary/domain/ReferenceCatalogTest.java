package com.acme.salary.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceCatalogTest {
    @Test
    void everyCountryAndLevelHasAnOrderedBandAndARate() {
        assertThat(ReferenceCatalog.bands()).hasSize(Country.values().length * JobLevel.values().length);
        Set<String> currencies = new HashSet<>();
        for (SalaryBand band : ReferenceCatalog.bands()) {
            assertThat(band.min()).isLessThan(band.mid());
            assertThat(band.mid()).isLessThan(band.max());
            assertThat(band.currency()).isEqualTo(band.country().currency());
            currencies.add(band.currency());
        }
        assertThat(ReferenceCatalog.rates()).extracting(FxRate::currency).containsAll(currencies);
    }
}
