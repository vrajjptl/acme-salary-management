package com.acme.salary.seed;

import com.acme.salary.domain.ChangeType;
import com.acme.salary.domain.CompensationCommand;
import com.acme.salary.domain.CompensationPolicy;
import com.acme.salary.domain.Country;
import com.acme.salary.domain.Department;
import com.acme.salary.domain.EmployeeStatus;
import com.acme.salary.domain.Gender;
import com.acme.salary.domain.JobLevel;
import com.acme.salary.domain.MoneyText;
import com.acme.salary.domain.PaySnapshot;
import com.acme.salary.domain.ReferenceCatalog;
import com.acme.salary.domain.SalaryBand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SeedFactory {
    public static final String EXCEPTION = "Compensation committee approved a market exception for a scarce skill.";
    private static final CompensationPolicy POLICY = new CompensationPolicy();
    private static final String[] FIRST = {
            "Ava", "Liam", "Mia", "Noah", "Sofia", "Ethan", "Amelia", "Lucas", "Priya", "Arjun",
            "Elena", "Mateo", "Hana", "Omar", "Leila", "Jonah", "Ines", "Hugo", "Nora", "Felix",
            "Aisha", "Diego", "Chiara", "Kenji", "Fatima", "Owen", "Maya", "Soren", "Imani", "Ravi"
    };
    private static final String[] LAST = {
            "Sharma", "Garcia", "Nguyen", "Patel", "Cohen", "Silva", "Keller", "Rossi", "Kim", "Diallo",
            "Nowak", "Ibrahim", "Costa", "Berg", "Sato", "Moreau", "Singh", "Almeida", "Novak", "Haddad",
            "Okafor", "Larsen", "Duarte", "Walsh", "Rahman", "Petrova", "Mensah", "Ito", "Bennett", "Chow"
    };

    private SeedFactory() {
    }

    public record SeedComp(
            LocalDate effectiveDate,
            BigDecimal baseSalary,
            BigDecimal annualBonus,
            BigDecimal allowances,
            String currency,
            ChangeType changeType,
            JobLevel jobLevel,
            String reason,
            String exceptionReason,
            PaySnapshot snapshot
    ) {
    }

    public record SeedEmployee(
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
            LocalDate terminationDate,
            List<SeedComp> history
    ) {
    }

    public record SeedBundle(List<SeedEmployee> employees) {
    }

    public static SeedBundle generate(int count, LocalDate today, Random random) {
        List<Department> departments = weightedDepartments();
        List<JobLevel> levels = weightedLevels();
        List<Gender> genders = weightedGenders();
        List<SeedEmployee> employees = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            employees.add(one(index, today, random, departments, levels, genders));
        }
        return new SeedBundle(List.copyOf(employees));
    }

    private static SeedEmployee one(
            int index,
            LocalDate today,
            Random random,
            List<Department> departments,
            List<JobLevel> levels,
            List<Gender> genders
    ) {
        String number = String.format("E%06d", index);
        Country country = Country.values()[random.nextInt(Country.values().length)];
        Department department = departments.get(random.nextInt(departments.size()));
        JobLevel level = levels.get(random.nextInt(levels.size()));
        Gender gender = genders.get(random.nextInt(genders.size()));
        String first = FIRST[random.nextInt(FIRST.length)];
        String last = LAST[random.nextInt(LAST.length)];
        LocalDate hireDate = hireDate(today, random);
        boolean outlier = random.nextDouble() < 0.04;
        SalaryBand band = ReferenceCatalog.band(country, level);
        BigDecimal base = outlier ? outside(band, random) : inside(band, random);
        BigDecimal bonus = bonus(base, random);
        BigDecimal allowances = random.nextDouble() < 0.25
                ? base.multiply(new BigDecimal("0.04")).setScale(2, RoundingMode.HALF_UP)
                : new BigDecimal("0.00");
        String exception = exceptionFor(null, base, band, ChangeType.HIRE);
        List<SeedComp> history = new ArrayList<>();
        history.add(row(hireDate, base, bonus, allowances, ChangeType.HIRE, level, null, exception, band));
        accept(number, country, hireDate, null, null, null, history.get(0), today);

        JobLevel currentLevel = level;
        BigDecimal currentBase = base;
        LocalDate lastDate = hireDate;
        String title = title(department, currentLevel);

        if (random.nextDouble() < 0.45) {
            LocalDate meritDate = hireDate.plusMonths(18);
            if (!meritDate.isAfter(today) && meritDate.isAfter(lastDate)) {
                BigDecimal multiplier = random.nextDouble() < 0.04
                        ? new BigDecimal("1.32")
                        : new BigDecimal("1.04").add(BigDecimal.valueOf(random.nextInt(8)).movePointLeft(2));
                BigDecimal proposed = money(currentBase.multiply(multiplier));
                SalaryBand same = ReferenceCatalog.band(country, currentLevel);
                if (proposed.compareTo(MoneyText.MAX) <= 0) {
                    String meritException = exceptionFor(currentBase, proposed, same, ChangeType.MERIT);
                    SeedComp merit = row(meritDate, proposed, bonus, allowances, ChangeType.MERIT, currentLevel,
                            "Annual merit review for this cycle.", meritException, same);
                    accept(number, country, hireDate, currentLevel, lastDate, currentBase, merit, today);
                    history.add(merit);
                    currentBase = proposed;
                    lastDate = meritDate;
                }
            }
        }

        int room = JobLevel.M3.rank() - currentLevel.rank();
        if (room >= 1 && random.nextDouble() < 0.18) {
            int steps = room == 1 || random.nextDouble() < 0.85 ? 1 : Math.min(2, room);
            JobLevel promoted = JobLevel.fromRank(currentLevel.rank() + steps);
            LocalDate promotionDate = lastDate.plusMonths(12);
            if (!promotionDate.isAfter(today) && promotionDate.isAfter(lastDate)) {
                SalaryBand nextBand = ReferenceCatalog.band(country, promoted);
                BigDecimal proposed = money(currentBase.multiply(new BigDecimal("1.08")));
                if (proposed.compareTo(nextBand.min()) < 0) {
                    proposed = nextBand.min();
                }
                if (proposed.compareTo(nextBand.max()) > 0) {
                    proposed = nextBand.max();
                }
                String promotionException = exceptionFor(currentBase, proposed, nextBand, ChangeType.PROMOTION);
                SeedComp promotion = row(promotionDate, proposed, bonus, allowances, ChangeType.PROMOTION, promoted,
                        "Promotion following an expanded role.", promotionException, nextBand);
                accept(number, country, hireDate, currentLevel, lastDate, currentBase, promotion, today);
                history.add(promotion);
                currentLevel = promoted;
                currentBase = proposed;
                lastDate = promotionDate;
                title = title(department, currentLevel);
            }
        }

        LocalDate termination = null;
        EmployeeStatus status = EmployeeStatus.ACTIVE;
        if (random.nextDouble() < 0.08 && lastDate.isBefore(today)) {
            long days = today.toEpochDay() - lastDate.toEpochDay();
            if (days >= 1) {
                termination = lastDate.plusDays(1 + random.nextInt((int) Math.min(days, Integer.MAX_VALUE)));
                if (termination.isAfter(today)) {
                    termination = today;
                }
                status = EmployeeStatus.TERMINATED;
            }
        }

        return new SeedEmployee(
                number,
                first,
                last,
                number.toLowerCase() + "@acme.test",
                country,
                department,
                title,
                currentLevel,
                status,
                gender,
                hireDate,
                termination,
                List.copyOf(history)
        );
    }

    private static void accept(
            String number,
            Country country,
            LocalDate hireDate,
            JobLevel currentLevel,
            LocalDate latestDate,
            BigDecimal currentBase,
            SeedComp proposed,
            LocalDate today
    ) {
        JobLevel levelBefore = currentLevel == null ? proposed.jobLevel() : currentLevel;
        JobLevel requested = proposed.changeType() == ChangeType.PROMOTION ? proposed.jobLevel() : null;
        SalaryBand band = ReferenceCatalog.band(country, proposed.jobLevel());
        List<String> errors = POLICY.validate(new CompensationCommand(
                EmployeeStatus.ACTIVE,
                hireDate,
                null,
                levelBefore,
                latestDate,
                currentBase,
                band.currency(),
                proposed.effectiveDate(),
                proposed.baseSalary(),
                proposed.annualBonus(),
                proposed.allowances(),
                proposed.changeType(),
                requested,
                proposed.reason(),
                proposed.exceptionReason(),
                band,
                today
        ));
        if (!errors.isEmpty()) {
            throw new IllegalStateException(number + " " + proposed.changeType() + " " + errors);
        }
    }

    private static String exceptionFor(BigDecimal current, BigDecimal proposed, SalaryBand band, ChangeType type) {
        boolean outside = proposed.compareTo(band.min()) < 0 || proposed.compareTo(band.max()) > 0;
        boolean decrease = current != null && proposed.compareTo(current) < 0;
        boolean large = false;
        if (current != null && current.signum() > 0 && type != ChangeType.PROMOTION) {
            BigDecimal delta = proposed.subtract(current).divide(current, 6, RoundingMode.HALF_UP);
            large = delta.compareTo(CompensationPolicy.INCREASE_CAP) > 0;
        }
        return outside || decrease || large ? EXCEPTION : null;
    }

    private static SeedComp row(
            LocalDate date,
            BigDecimal base,
            BigDecimal bonus,
            BigDecimal allowances,
            ChangeType type,
            JobLevel level,
            String reason,
            String exception,
            SalaryBand band
    ) {
        PaySnapshot snapshot = PaySnapshot.of(base, bonus, allowances, band, ReferenceCatalog.usdPerUnit(band.currency()));
        return new SeedComp(date, base, bonus, allowances, band.currency(), type, level, reason, exception, snapshot);
    }

    private static BigDecimal inside(SalaryBand band, Random random) {
        BigDecimal span = band.max().subtract(band.min());
        BigDecimal point = band.min().add(span.multiply(BigDecimal.valueOf(0.15 + random.nextDouble() * 0.70)));
        return clamp(money(point), band.min(), band.max());
    }

    private static BigDecimal outside(SalaryBand band, Random random) {
        if (random.nextBoolean()) {
            return money(band.min().multiply(new BigDecimal("0.90")));
        }
        BigDecimal high = money(band.max().multiply(new BigDecimal("1.08")));
        return high.compareTo(MoneyText.MAX) > 0 ? MoneyText.MAX : high;
    }

    private static BigDecimal bonus(BigDecimal base, Random random) {
        if (random.nextDouble() < 0.45) {
            return new BigDecimal("0.00");
        }
        int percent = 5 + random.nextInt(11);
        return base.multiply(BigDecimal.valueOf(percent)).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }

    private static LocalDate hireDate(LocalDate today, Random random) {
        long start = today.minusYears(10).toEpochDay();
        long end = today.minusMonths(2).toEpochDay();
        long day = start + (long) (random.nextDouble() * (end - start));
        return LocalDate.ofEpochDay(day);
    }

    private static String title(Department department, JobLevel level) {
        String role = switch (department) {
            case ENGINEERING -> "Engineer";
            case PRODUCT -> "Product Manager";
            case DESIGN -> "Designer";
            case SALES -> "Account Executive";
            case PEOPLE -> "People Partner";
            case FINANCE -> "Financial Analyst";
            case MARKETING -> "Marketer";
            case OPERATIONS -> "Operations Partner";
            case LEGAL -> "Counsel";
            case SUPPORT -> "Support Specialist";
        };
        return switch (level) {
            case L1, L2 -> "Associate " + role;
            case L3, L4 -> role;
            case L5, L6 -> "Senior " + role;
            case M1, M2 -> "Manager, " + department.label();
            case M3 -> "Director, " + department.label();
        };
    }

    private static List<Department> weightedDepartments() {
        return weight(new Department[]{
                Department.ENGINEERING, Department.PRODUCT, Department.DESIGN, Department.SALES, Department.PEOPLE,
                Department.FINANCE, Department.MARKETING, Department.OPERATIONS, Department.LEGAL, Department.SUPPORT
        }, new int[]{28, 10, 7, 14, 6, 8, 8, 10, 3, 6});
    }

    private static List<JobLevel> weightedLevels() {
        return weight(JobLevel.values(), new int[]{8, 12, 18, 18, 14, 10, 10, 6, 4});
    }

    private static List<Gender> weightedGenders() {
        return weight(Gender.values(), new int[]{46, 46, 4, 4});
    }

    private static <T> List<T> weight(T[] values, int[] weights) {
        List<T> weighted = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            for (int n = 0; n < weights[i]; n++) {
                weighted.add(values[i]);
            }
        }
        return List.copyOf(weighted);
    }
}
