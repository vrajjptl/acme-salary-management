package com.acme.salary.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public final class Requests {
    private Requests() {
    }

    public record Login(String username, String password) {
    }

    public record CreateEmployee(
            String firstName,
            String lastName,
            String email,
            String country,
            String department,
            String jobTitle,
            String jobLevel,
            String gender,
            String hireDate,
            @JsonDeserialize(using = StrictMoneyDeserializer.class) String baseSalary,
            @JsonDeserialize(using = StrictMoneyDeserializer.class) String annualBonus,
            @JsonDeserialize(using = StrictMoneyDeserializer.class) String allowances,
            String reason,
            String exceptionReason
    ) {
    }

    public record ChangeCompensation(
            String changeType,
            String effectiveDate,
            @JsonDeserialize(using = StrictMoneyDeserializer.class) String baseSalary,
            @JsonDeserialize(using = StrictMoneyDeserializer.class) String annualBonus,
            @JsonDeserialize(using = StrictMoneyDeserializer.class) String allowances,
            String jobLevel,
            String jobTitle,
            String reason,
            String exceptionReason
    ) {
    }

    public record PatchEmployee(
            String firstName,
            String lastName,
            String jobTitle,
            String department,
            String gender
    ) {
    }

    public record Terminate(String terminationDate) {
    }
}
