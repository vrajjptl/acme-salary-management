package com.acme.salary.dto;

import com.acme.salary.domain.OrgPayCalculator;

import java.util.List;

public final class Views {
    private Views() {
    }

    public record UserView(String username, String role) {
    }

    public record EmployeeItem(
            long id,
            String employeeNumber,
            String firstName,
            String lastName,
            String email,
            String country,
            String countryName,
            String department,
            String departmentName,
            String jobTitle,
            String jobLevel,
            String status,
            String gender,
            String genderName,
            String hireDate,
            String terminationDate,
            String currency,
            String baseSalary,
            String annualBonus,
            String allowances,
            String baseSalaryUsd,
            String totalCompUsd,
            String compaRatio,
            String bandPosition
    ) {
    }

    public record EmployeePage(int page, int size, long total, List<EmployeeItem> items) {
    }

    public record HistoryItem(
            long id,
            String effectiveDate,
            String baseSalary,
            String currency,
            String annualBonus,
            String allowances,
            String changeType,
            String jobLevel,
            String reason,
            String exceptionReason,
            String createdAt,
            String createdBy,
            String baseSalaryUsd,
            String compaRatio,
            String bandPosition
    ) {
    }

    public record EmployeeDetail(
            EmployeeItem employee,
            String bandMin,
            String bandMid,
            String bandMax,
            List<HistoryItem> history
    ) {
    }

    public record AuditItem(
            long id,
            String occurredAt,
            String actor,
            String action,
            String entityType,
            String entityId,
            String summary
    ) {
    }

    public record AuditPage(int page, int size, long total, List<AuditItem> items) {
    }

    public record Coded(String code, String label) {
    }

    public record Meta(
            List<Coded> countries,
            List<Coded> departments,
            List<String> levels,
            List<String> changeTypes,
            List<Coded> genders
    ) {
    }

    public record AnalyticsView(
            int activeWithPay,
            int formerHeadcount,
            int scheduledHeadcount,
            OrgPayCalculator.Summary pay,
            String fxAsOf,
            String country,
            String department,
            List<String> notes
    ) {
    }
}
