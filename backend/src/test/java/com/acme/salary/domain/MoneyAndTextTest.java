package com.acme.salary.domain;

import com.acme.salary.error.ApiExceptions.FieldValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyAndTextTest {
    @Test
    void moneyRejectsSignsScaleAndZeroBase() {
        assertThat(parse("10.50", true)).isEqualByComparingTo("10.50");
        assertThat(parse("0.00", false)).isEqualByComparingTo("0.00");
        assertThat(errors("10.555", true)).containsKey("amount");
        assertThat(errors("-10.00", true)).containsKey("amount");
        assertThat(errors("1e2", true)).containsKey("amount");
        assertThat(errors("0.00", true)).containsEntry("amount", "must be greater than zero");
        assertThat(errors("1000000000.00", true)).containsKey("amount");
    }

    @Test
    void textRejectsMarkupAndShortReasons() {
        Map<String, String> errors = new HashMap<>();
        assertThat(TextRules.required("Ada Lovelace", "name", 1, 80, errors)).isEqualTo("Ada Lovelace");
        assertThat(TextRules.required("Ada <script>", "name", 1, 80, new HashMap<>())).isNull();
        assertThat(TextRules.optional("short", "reason", 8, 500, errors)).isNull();
        assertThat(errors).containsKey("reason");
        assertThat(TextRules.email("Ada@Acme.TEST", new HashMap<>())).isEqualTo("ada@acme.test");
    }

    @Test
    void sortAndPageAndSearchStayInsideTheAllowlist() {
        assertThat(EmployeeSort.parse("name", "desc").descending()).isTrue();
        assertThat(EmployeeSort.sql(EmployeeSort.parse("baseSalaryUsd", "asc"))).contains("CAST(c.base_salary_usd AS REAL)");
        assertThatThrownBy(() -> EmployeeSort.parse("salary; drop table employee", "asc"))
                .isInstanceOf(FieldValidationException.class);
        assertThatThrownBy(() -> Pages.size(101)).isInstanceOf(FieldValidationException.class);
        assertThatThrownBy(() -> Pages.page(-1)).isInstanceOf(FieldValidationException.class);
        assertThat(SearchTerms.contains("%' OR 1=1 --")).isEqualTo("%' or 1=1 --%");
        assertThat(SearchTerms.contains("___")).isNull();
    }

    private static BigDecimal parse(String raw, boolean positive) {
        Map<String, String> errors = new HashMap<>();
        BigDecimal value = MoneyText.parse(raw, "amount", positive, errors);
        assertThat(errors).isEmpty();
        return value;
    }

    private static Map<String, String> errors(String raw, boolean positive) {
        Map<String, String> errors = new HashMap<>();
        MoneyText.parse(raw, "amount", positive, errors);
        return errors;
    }
}
