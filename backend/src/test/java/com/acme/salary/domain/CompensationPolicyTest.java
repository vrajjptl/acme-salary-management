package com.acme.salary.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompensationPolicyTest {
    private final CompensationPolicy policy = new CompensationPolicy();
    private final LocalDate today = LocalDate.of(2026, 11, 2);
    private final LocalDate hired = LocalDate.of(2024, 1, 15);
    private final SalaryBand band = new SalaryBand(
            Country.US, JobLevel.L3, "USD",
            new BigDecimal("80000.00"), new BigDecimal("100000.00"), new BigDecimal("120000.00")
    );
    private final SalaryBand nextBand = new SalaryBand(
            Country.US, JobLevel.L4, "USD",
            new BigDecimal("90000.00"), new BigDecimal("115000.00"), new BigDecimal("140000.00")
    );

    @Test
    void meritAtExactlyTwentyFivePercentIsAllowed() {
        assertThat(policy.validate(merit(new BigDecimal("80000.00"), new BigDecimal("100000.00"), null))).isEmpty();
    }

    @Test
    void meritAboveTwentyFivePercentNeedsAnException() {
        List<String> errors = policy.validate(merit(new BigDecimal("80000.00"), new BigDecimal("100800.00"), null));
        assertThat(errors).anyMatch(error -> error.contains("25%"));
        assertThat(policy.validate(merit(new BigDecimal("80000.00"), new BigDecimal("100800.00"), "Scarce skill exception"))).isEmpty();
    }

    @Test
    void adjustmentCannotBypassTheIncreaseCap() {
        CompensationCommand command = copy(merit(new BigDecimal("80000.00"), new BigDecimal("100800.00"), null), ChangeType.ADJUSTMENT, null, band);
        assertThat(policy.validate(command)).anyMatch(error -> error.contains("25%"));
    }

    @Test
    void promotionMayExceedTheCapInsideTheNewBand() {
        CompensationCommand command = new CompensationCommand(
                EmployeeStatus.ACTIVE, hired, null, JobLevel.L3, hired, new BigDecimal("80000.00"),
                "USD", today, new BigDecimal("115000.00"), money("0.00"), money("0.00"),
                ChangeType.PROMOTION, JobLevel.L4, "Promotion following an expanded role.", null, nextBand, today
        );
        assertThat(policy.validate(command)).isEmpty();
    }

    @Test
    void promotionCannotSkipMoreThanTwoLevels() {
        CompensationCommand command = new CompensationCommand(
                EmployeeStatus.ACTIVE, hired, null, JobLevel.L3, hired, new BigDecimal("80000.00"),
                "USD", today, new BigDecimal("190000.00"), money("0.00"), money("0.00"),
                ChangeType.PROMOTION, JobLevel.M1, "Promotion following an expanded role.", null,
                new SalaryBand(Country.US, JobLevel.M1, "USD", money("150000.00"), money("190000.00"), money("230000.00")),
                today
        );
        assertThat(policy.validate(command)).anyMatch(error -> error.contains("two levels"));
    }

    @Test
    void promotionMustMoveUp() {
        CompensationCommand command = new CompensationCommand(
                EmployeeStatus.ACTIVE, hired, null, JobLevel.L4, hired, new BigDecimal("100000.00"),
                "USD", today, new BigDecimal("100000.00"), money("0.00"), money("0.00"),
                ChangeType.PROMOTION, JobLevel.L3, "Promotion following an expanded role.", null, band, today
        );
        assertThat(policy.validate(command)).anyMatch(error -> error.contains("higher level"));
    }

    @Test
    void decreaseNeedsAnException() {
        assertThat(policy.validate(merit(new BigDecimal("100000.00"), new BigDecimal("90000.00"), null)))
                .anyMatch(error -> error.contains("decrease"));
        assertThat(policy.validate(merit(new BigDecimal("100000.00"), new BigDecimal("90000.00"), "Correcting a duplicate allowance"))).isEmpty();
    }

    @Test
    void salaryOutsideTheBandNeedsAnException() {
        assertThat(policy.validate(merit(new BigDecimal("100000.00"), new BigDecimal("130000.00"), null)))
                .anyMatch(error -> error.contains("outside the band"));
        assertThat(policy.validate(merit(new BigDecimal("100000.00"), new BigDecimal("130000.00"), "Market exception for a scarce skill"))).isEmpty();
    }

    @Test
    void sameDayChangeMustBeACorrection() {
        CompensationCommand sameDay = new CompensationCommand(
                EmployeeStatus.ACTIVE, hired, null, JobLevel.L3, hired, new BigDecimal("80000.00"),
                "USD", hired, new BigDecimal("84000.00"), money("0.00"), money("0.00"),
                ChangeType.MERIT, null, "Annual merit review", null, band, today
        );
        assertThat(policy.validate(sameDay)).anyMatch(error -> error.contains("same-day"));
        CompensationCommand correction = new CompensationCommand(
                EmployeeStatus.ACTIVE, hired, null, JobLevel.L3, hired, new BigDecimal("80000.00"),
                "USD", hired, new BigDecimal("84000.00"), money("0.00"), money("0.00"),
                ChangeType.CORRECTION, null, "Correcting the hire amount", null, band, today
        );
        assertThat(policy.validate(correction)).isEmpty();
    }

    @Test
    void rejectsBackdatingTerminatedStaffAndAFarFutureDate() {
        CompensationCommand backdated = new CompensationCommand(
                EmployeeStatus.ACTIVE, hired, null, JobLevel.L3, today, new BigDecimal("80000.00"),
                "USD", hired, new BigDecimal("84000.00"), money("0.00"), money("0.00"),
                ChangeType.MERIT, null, "Annual merit review", null, band, today
        );
        assertThat(policy.validate(backdated)).anyMatch(error -> error.contains("earlier than the latest"));

        CompensationCommand terminated = new CompensationCommand(
                EmployeeStatus.TERMINATED, hired, today, JobLevel.L3, hired, new BigDecimal("80000.00"),
                "USD", today, new BigDecimal("84000.00"), money("0.00"), money("0.00"),
                ChangeType.MERIT, null, "Annual merit review", null, band, today
        );
        assertThat(policy.validate(terminated)).anyMatch(error -> error.contains("terminated"));

        CompensationCommand distant = new CompensationCommand(
                EmployeeStatus.ACTIVE, hired, null, JobLevel.L3, hired, new BigDecimal("80000.00"),
                "USD", today.plusMonths(4), new BigDecimal("84000.00"), money("0.00"), money("0.00"),
                ChangeType.MERIT, null, "Annual merit review", null, band, today
        );
        assertThat(policy.validate(distant)).anyMatch(error -> error.contains("3 months"));
    }

    @Test
    void hireIsOnceAndMatchesTheHireDate() {
        CompensationCommand secondHire = new CompensationCommand(
                EmployeeStatus.ACTIVE, hired, null, JobLevel.L3, hired, new BigDecimal("80000.00"),
                "USD", today, new BigDecimal("80000.00"), money("0.00"), money("0.00"),
                ChangeType.HIRE, null, null, null, band, today
        );
        assertThat(policy.validate(secondHire)).anyMatch(error -> error.contains("only be recorded once"));

        CompensationCommand wrongDay = new CompensationCommand(
                EmployeeStatus.ACTIVE, hired, null, JobLevel.L3, null, null,
                "USD", today, new BigDecimal("80000.00"), money("0.00"), money("0.00"),
                ChangeType.HIRE, null, null, null, band, today
        );
        assertThat(policy.validate(wrongDay)).anyMatch(error -> error.contains("hire date"));

        CompensationCommand beforeHire = new CompensationCommand(
                EmployeeStatus.ACTIVE, null, null, null, null, null,
                "USD", today, new BigDecimal("80000.00"), money("0.00"), money("0.00"),
                ChangeType.MERIT, null, "Annual merit review", null, band, today
        );
        assertThat(policy.validate(beforeHire)).anyMatch(error -> error.contains("must be a hire"));
    }

    private CompensationCommand merit(BigDecimal current, BigDecimal proposed, String exception) {
        return new CompensationCommand(
                EmployeeStatus.ACTIVE, hired, null, JobLevel.L3, hired, current,
                "USD", today, proposed, money("0.00"), money("0.00"),
                ChangeType.MERIT, null, "Annual merit review", exception, band, today
        );
    }

    private CompensationCommand copy(CompensationCommand command, ChangeType type, JobLevel level, SalaryBand salaryBand) {
        return new CompensationCommand(
                command.status(), command.hireDate(), command.terminationDate(), command.currentLevel(),
                command.latestEffectiveDate(), command.currentBase(), salaryBand.currency(), command.effectiveDate(),
                command.baseSalary(), command.annualBonus(), command.allowances(), type, level,
                command.reason(), command.exceptionReason(), salaryBand, command.today()
        );
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
