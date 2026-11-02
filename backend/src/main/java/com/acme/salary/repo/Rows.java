package com.acme.salary.repo;

import com.acme.salary.domain.BandPosition;
import com.acme.salary.domain.ChangeType;
import com.acme.salary.domain.Country;
import com.acme.salary.domain.Department;
import com.acme.salary.domain.EmployeeStatus;
import com.acme.salary.domain.Gender;
import com.acme.salary.domain.JobLevel;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Rows {
    private Rows() {
    }

    public record UserRow(String username, String passwordHash, String role, boolean enabled) {
    }

    public record EmployeeRow(
            long id,
            String employeeNumber,
            String firstName,
            String lastName,
            String email,
            Country country,
            Department department,
            String jobTitle,
            JobLevel jobLevel,
            EmployeeStatus status,
            Gender gender,
            LocalDate hireDate,
            LocalDate terminationDate
    ) {
    }

    public record CompRow(
            long id,
            long employeeId,
            LocalDate effectiveDate,
            BigDecimal baseSalary,
            String currency,
            BigDecimal annualBonus,
            BigDecimal allowances,
            ChangeType changeType,
            JobLevel jobLevel,
            String reason,
            String exceptionReason,
            String createdAt,
            String createdBy,
            BigDecimal baseSalaryUsd,
            BigDecimal totalCompUsd,
            BigDecimal compaRatio,
            BigDecimal bandMin,
            BigDecimal bandMid,
            BigDecimal bandMax,
            BandPosition bandPosition
    ) {
    }

    public record DirectoryRow(
            EmployeeRow employee,
            BigDecimal baseSalary,
            String currency,
            BigDecimal annualBonus,
            BigDecimal allowances,
            BigDecimal baseSalaryUsd,
            BigDecimal totalCompUsd,
            BigDecimal compaRatio,
            BandPosition bandPosition
    ) {
    }
}
