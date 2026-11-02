package com.acme.salary.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class CompensationPolicy {
    public static final BigDecimal INCREASE_CAP = new BigDecimal("0.25");
    public static final int MAX_FUTURE_MONTHS = 3;
    public static final int MIN_REASON = 8;
    public static final int MIN_EXCEPTION = 12;
    public static final int MAX_PROMOTION_STEPS = 2;

    public List<String> validate(CompensationCommand command) {
        List<String> errors = new ArrayList<>();
        if (command.changeType() == null) {
            errors.add("Change type is required.");
            return errors;
        }
        if (command.today() == null) {
            errors.add("Business date is required.");
        }
        if (command.status() != EmployeeStatus.ACTIVE) {
            errors.add("Compensation cannot be changed for a terminated employee.");
        }
        if (command.effectiveDate() == null) {
            errors.add("Effective date is required.");
        } else {
            if (command.hireDate() != null && command.effectiveDate().isBefore(command.hireDate())) {
                errors.add("Effective date cannot be before the hire date.");
            }
            if (command.today() != null && command.effectiveDate().isAfter(command.today().plusMonths(MAX_FUTURE_MONTHS))) {
                errors.add("Effective date cannot be more than 3 months in the future.");
            }
            if (command.terminationDate() != null && command.effectiveDate().isAfter(command.terminationDate())) {
                errors.add("Effective date cannot be after the termination date.");
            }
            if (command.latestEffectiveDate() != null && command.effectiveDate().isBefore(command.latestEffectiveDate())) {
                errors.add("Effective date cannot be earlier than the latest compensation record.");
            }
            if (command.latestEffectiveDate() != null
                    && command.effectiveDate().equals(command.latestEffectiveDate())
                    && command.changeType() != ChangeType.CORRECTION) {
                errors.add("A same-day change must be recorded as a correction.");
            }
            if (command.changeType() == ChangeType.HIRE
                    && command.hireDate() != null
                    && !command.effectiveDate().equals(command.hireDate())) {
                errors.add("Hire compensation must be effective on the hire date.");
            }
        }
        if (command.baseSalary() == null || command.baseSalary().signum() <= 0) {
            errors.add("Base salary must be greater than zero.");
        }
        if (command.annualBonus() == null || command.annualBonus().signum() < 0) {
            errors.add("Annual bonus cannot be negative.");
        }
        if (command.allowances() == null || command.allowances().signum() < 0) {
            errors.add("Allowances cannot be negative.");
        }
        if (command.changeType() == ChangeType.HIRE && command.latestEffectiveDate() != null) {
            errors.add("Hire compensation can only be recorded once.");
        }
        if (command.changeType() != ChangeType.HIRE && command.latestEffectiveDate() == null) {
            errors.add("The first compensation record must be a hire.");
        }

        if (command.changeType() == ChangeType.PROMOTION) {
            if (command.newLevel() == null) {
                errors.add("Promotion requires the new job level.");
            } else if (command.currentLevel() == null) {
                errors.add("Promotion requires the employee's current level.");
            } else {
                int steps = command.newLevel().rank() - command.currentLevel().rank();
                if (steps < 1) {
                    errors.add("Promotion must move the employee to a higher level.");
                } else if (steps > MAX_PROMOTION_STEPS) {
                    errors.add("Promotion cannot skip more than two levels.");
                }
            }
        } else if (command.newLevel() != null
                && command.changeType() != ChangeType.CORRECTION
                && command.currentLevel() != null
                && command.newLevel() != command.currentLevel()) {
            errors.add("Job level can only change on a promotion or a correction.");
        }

        JobLevel bandLevel = bandLevelFor(command.changeType(), command.currentLevel(), command.newLevel());
        if (command.band() == null) {
            errors.add("No salary band is defined for this country and level.");
        } else {
            if (bandLevel != null && command.band().level() != bandLevel) {
                errors.add("Salary band does not match the target job level.");
            }
            if (command.currency() == null || !command.currency().equals(command.band().currency())) {
                errors.add("Currency must match the salary band for this country.");
            }
            if (command.baseSalary() != null && outside(command.baseSalary(), command.band()) && !hasException(command.exceptionReason())) {
                errors.add("Salary is outside the band. An exception reason of at least 12 characters is required.");
            }
        }

        boolean largeIncrease = false;
        boolean decrease = false;
        if (command.currentBase() != null && command.baseSalary() != null && command.currentBase().signum() > 0) {
            BigDecimal delta = command.baseSalary().subtract(command.currentBase())
                    .divide(command.currentBase(), 6, RoundingMode.HALF_UP);
            largeIncrease = delta.compareTo(INCREASE_CAP) > 0;
            decrease = command.baseSalary().compareTo(command.currentBase()) < 0;
        }
        if (largeIncrease && command.changeType() != ChangeType.PROMOTION && !hasException(command.exceptionReason())) {
            errors.add("An increase above 25% requires an exception reason of at least 12 characters.");
        }
        if (decrease && !hasException(command.exceptionReason())) {
            errors.add("A salary decrease requires an exception reason of at least 12 characters.");
        }
        if (command.exceptionReason() != null
                && !command.exceptionReason().isBlank()
                && command.exceptionReason().trim().length() < MIN_EXCEPTION) {
            errors.add("Exception reason must be at least 12 characters when it is provided.");
        }
        if (command.changeType() != ChangeType.HIRE && !hasReason(command.reason())) {
            errors.add("A reason of at least 8 characters is required.");
        }
        if (command.reason() != null && command.reason().trim().length() > 500) {
            errors.add("Reason cannot exceed 500 characters.");
        }
        if (command.exceptionReason() != null && command.exceptionReason().trim().length() > 500) {
            errors.add("Exception reason cannot exceed 500 characters.");
        }
        return errors;
    }

    public static JobLevel bandLevelFor(ChangeType type, JobLevel current, JobLevel requested) {
        if (type == ChangeType.PROMOTION) {
            return requested == null ? current : requested;
        }
        if (type == ChangeType.CORRECTION && requested != null) {
            return requested;
        }
        return current;
    }

    private static boolean outside(BigDecimal base, SalaryBand band) {
        return base.compareTo(band.min()) < 0 || base.compareTo(band.max()) > 0;
    }

    private static boolean hasReason(String reason) {
        return reason != null && reason.trim().length() >= MIN_REASON;
    }

    private static boolean hasException(String reason) {
        return reason != null && reason.trim().length() >= MIN_EXCEPTION;
    }
}
