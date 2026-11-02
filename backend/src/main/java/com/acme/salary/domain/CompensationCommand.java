package com.acme.salary.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompensationCommand(
        EmployeeStatus status,
        LocalDate hireDate,
        LocalDate terminationDate,
        JobLevel currentLevel,
        LocalDate latestEffectiveDate,
        BigDecimal currentBase,
        String currency,
        LocalDate effectiveDate,
        BigDecimal baseSalary,
        BigDecimal annualBonus,
        BigDecimal allowances,
        ChangeType changeType,
        JobLevel newLevel,
        String reason,
        String exceptionReason,
        SalaryBand band,
        LocalDate today
) {
}
