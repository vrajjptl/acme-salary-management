package com.acme.salary.seed;

import com.acme.salary.domain.ChangeType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SeedFactoryTest {
    @Test
    void generatedPopulationObeysTheSamePolicyAsATypedChange() {
        SeedFactory.SeedBundle bundle = SeedFactory.generate(2_000, LocalDate.of(2026, 11, 2), new Random(42));
        assertThat(bundle.employees()).hasSize(2_000);
        Set<String> numbers = new HashSet<>();
        int promotions = 0;
        int exceptions = 0;
        int terminated = 0;
        for (SeedFactory.SeedEmployee employee : bundle.employees()) {
            assertThat(numbers.add(employee.employeeNumber())).isTrue();
            assertThat(employee.history().get(0).changeType()).isEqualTo(ChangeType.HIRE);
            if (employee.history().stream().anyMatch(row -> row.changeType() == ChangeType.PROMOTION)) {
                promotions++;
            }
            if (employee.history().stream().anyMatch(row -> row.exceptionReason() != null)) {
                exceptions++;
            }
            if (employee.terminationDate() != null) {
                terminated++;
                assertThat(employee.terminationDate()).isAfterOrEqualTo(employee.history().get(employee.history().size() - 1).effectiveDate());
            }
        }
        assertThat(promotions).isPositive();
        assertThat(exceptions).isPositive();
        assertThat(terminated).isPositive();
    }
}
