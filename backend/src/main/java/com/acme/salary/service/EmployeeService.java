package com.acme.salary.service;

import com.acme.salary.domain.ChangeType;
import com.acme.salary.domain.CompensationCommand;
import com.acme.salary.domain.CompensationPolicy;
import com.acme.salary.domain.Country;
import com.acme.salary.domain.Dates;
import com.acme.salary.domain.Department;
import com.acme.salary.domain.EmployeeSort;
import com.acme.salary.domain.EmployeeStatus;
import com.acme.salary.domain.Enums;
import com.acme.salary.domain.FxRate;
import com.acme.salary.domain.Gender;
import com.acme.salary.domain.JobLevel;
import com.acme.salary.domain.MoneyText;
import com.acme.salary.domain.Pages;
import com.acme.salary.domain.PaySnapshot;
import com.acme.salary.domain.SalaryBand;
import com.acme.salary.domain.SearchTerms;
import com.acme.salary.domain.TextRules;
import com.acme.salary.dto.Requests;
import com.acme.salary.dto.Views;
import com.acme.salary.error.ApiExceptions.BusinessRuleException;
import com.acme.salary.error.ApiExceptions.ConflictException;
import com.acme.salary.error.ApiExceptions.FieldValidationException;
import com.acme.salary.error.ApiExceptions.NotFoundException;
import com.acme.salary.repo.CompensationRepository;
import com.acme.salary.repo.EmployeeRepository;
import com.acme.salary.repo.ReferenceRepository;
import com.acme.salary.repo.Rows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employees;
    private final CompensationRepository compensation;
    private final ReferenceRepository references;
    private final CurrentPayProjection projection;
    private final AuditTrail audit;
    private final BusinessCalendar calendar;
    private final CompensationPolicy policy = new CompensationPolicy();

    public EmployeeService(
            EmployeeRepository employees,
            CompensationRepository compensation,
            ReferenceRepository references,
            CurrentPayProjection projection,
            AuditTrail audit,
            BusinessCalendar calendar
    ) {
        this.employees = employees;
        this.compensation = compensation;
        this.references = references;
        this.projection = projection;
        this.audit = audit;
        this.calendar = calendar;
    }

    @Transactional
    public Views.EmployeePage list(
            String query,
            String country,
            String department,
            String level,
            String status,
            String sort,
            String direction,
            int page,
            int size
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        Country countryValue = Enums.parseOptional(Country.class, country, "country", errors);
        Department departmentValue = Enums.parseOptional(Department.class, department, "department", errors);
        JobLevel levelValue = Enums.parseOptional(JobLevel.class, level, "level", errors);
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        projection.ensureCurrent(calendar.today());
        EmployeeRepository.DirectoryPage result = employees.directory(
                SearchTerms.contains(query),
                countryValue,
                departmentValue,
                levelValue,
                parseStatus(status),
                EmployeeSort.parse(sort, direction),
                Pages.page(page),
                Pages.size(size)
        );
        List<Views.EmployeeItem> items = new ArrayList<>();
        for (Rows.DirectoryRow row : result.items()) {
            items.add(item(row.employee(), row.baseSalary(), row.currency(), row.annualBonus(), row.allowances(),
                    row.baseSalaryUsd(), row.totalCompUsd(), row.compaRatio(), row.bandPosition()));
        }
        return new Views.EmployeePage(page, size, result.total(), items);
    }

    @Transactional
    public Views.EmployeeDetail get(long id) {
        projection.refreshEmployee(id, calendar.today());
        return detail(id);
    }

    @Transactional
    public Views.EmployeeDetail create(String actor, Requests.CreateEmployee request) {
        if (request == null) {
            throw new FieldValidationException(Map.of("body", "is required"));
        }
        Map<String, String> errors = new LinkedHashMap<>();
        String firstName = TextRules.required(request.firstName(), "firstName", 1, 80, errors);
        String lastName = TextRules.required(request.lastName(), "lastName", 1, 80, errors);
        String email = TextRules.email(request.email(), errors);
        Country country = Enums.parse(Country.class, request.country(), "country", errors);
        Department department = Enums.parse(Department.class, request.department(), "department", errors);
        String jobTitle = TextRules.required(request.jobTitle(), "jobTitle", 2, 80, errors);
        JobLevel level = Enums.parse(JobLevel.class, request.jobLevel(), "jobLevel", errors);
        Gender gender = Enums.parse(Gender.class, request.gender(), "gender", errors);
        LocalDate hireDate = Dates.parse(request.hireDate(), "hireDate", errors);
        BigDecimal base = MoneyText.parse(request.baseSalary(), "baseSalary", true, errors);
        BigDecimal bonus = MoneyText.parse(request.annualBonus(), "annualBonus", false, errors);
        BigDecimal allowances = MoneyText.parse(request.allowances(), "allowances", false, errors);
        String reason = TextRules.optional(request.reason(), "reason", CompensationPolicy.MIN_REASON, 500, errors);
        String exception = TextRules.optional(request.exceptionReason(), "exceptionReason", CompensationPolicy.MIN_EXCEPTION, 500, errors);
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        SalaryBand band = references.findBand(country, level).orElse(null);
        FxRate fx = band == null ? null : references.findFx(band.currency()).orElse(null);
        List<String> violations = policy.validate(new CompensationCommand(
                EmployeeStatus.ACTIVE,
                hireDate,
                null,
                level,
                null,
                null,
                band == null ? null : band.currency(),
                hireDate,
                base,
                bonus,
                allowances,
                ChangeType.HIRE,
                null,
                reason,
                exception,
                band,
                calendar.today()
        ));
        if (band != null && fx == null) {
            violations.add("No FX rate is configured for " + band.currency() + ".");
        }
        if (!violations.isEmpty()) {
            throw new BusinessRuleException(violations);
        }
        if (employees.emailExists(email)) {
            throw new ConflictException("An employee with that email already exists.");
        }
        String number = employees.allocateNumber();
        long id;
        try {
            id = employees.insert(number, firstName, lastName, email, country, department, jobTitle, level, gender, hireDate);
        } catch (DataAccessException dataException) {
            throw conflict(dataException);
        }
        PaySnapshot snapshot = PaySnapshot.of(base, bonus, allowances, band, fx.usdPerUnit());
        compensation.insert(id, hireDate, base, band.currency(), bonus, allowances, ChangeType.HIRE, level, reason, exception,
                Instant.now().toString(), actor, snapshot);
        projection.refreshEmployee(id, calendar.today());
        audit.record(actor, "EMPLOYEE_CREATED", "employee", Long.toString(id),
                "Hired " + number + " at " + MoneyText.format(base) + " " + band.currency());
        log.info("employee created id={} actor={}", id, actor);
        return detail(id);
    }

    @Transactional
    public Views.EmployeeDetail change(String actor, long id, Requests.ChangeCompensation request) {
        if (request == null) {
            throw new FieldValidationException(Map.of("body", "is required"));
        }
        Rows.EmployeeRow employee = employees.findById(id).orElseThrow(() -> new NotFoundException("Employee was not found."));
        Map<String, String> errors = new LinkedHashMap<>();
        ChangeType type = Enums.parse(ChangeType.class, request.changeType(), "changeType", errors);
        if (type == ChangeType.HIRE) {
            errors.put("changeType", "hire compensation is recorded when the employee is created");
        }
        LocalDate effective = Dates.parse(request.effectiveDate(), "effectiveDate", errors);
        BigDecimal base = MoneyText.parse(request.baseSalary(), "baseSalary", true, errors);
        BigDecimal bonus = MoneyText.parse(request.annualBonus(), "annualBonus", false, errors);
        BigDecimal allowances = MoneyText.parse(request.allowances(), "allowances", false, errors);
        JobLevel requestedLevel = Enums.parseOptional(JobLevel.class, request.jobLevel(), "jobLevel", errors);
        String reason = TextRules.optional(request.reason(), "reason", CompensationPolicy.MIN_REASON, 500, errors);
        String exception = TextRules.optional(request.exceptionReason(), "exceptionReason", CompensationPolicy.MIN_EXCEPTION, 500, errors);
        String title = TextRules.optional(request.jobTitle(), "jobTitle", 2, 80, errors);
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        Rows.CompRow latest = compensation.findLatest(id).orElse(null);
        JobLevel bandLevel = CompensationPolicy.bandLevelFor(type, employee.jobLevel(), requestedLevel);
        SalaryBand band = bandLevel == null ? null : references.findBand(employee.country(), bandLevel).orElse(null);
        FxRate fx = band == null ? null : references.findFx(band.currency()).orElse(null);
        List<String> violations = policy.validate(new CompensationCommand(
                employee.status(),
                employee.hireDate(),
                employee.terminationDate(),
                employee.jobLevel(),
                latest == null ? null : latest.effectiveDate(),
                latest == null ? null : latest.baseSalary(),
                band == null ? null : band.currency(),
                effective,
                base,
                bonus,
                allowances,
                type,
                requestedLevel,
                reason,
                exception,
                band,
                calendar.today()
        ));
        if (band != null && fx == null) {
            violations.add("No FX rate is configured for " + band.currency() + ".");
        }
        if (!violations.isEmpty()) {
            throw new BusinessRuleException(violations);
        }
        String storedTitle = title == null ? employee.jobTitle() : title;
        if (bandLevel != employee.jobLevel() || !storedTitle.equals(employee.jobTitle())) {
            employees.updateRole(id, bandLevel, storedTitle);
        }
        PaySnapshot snapshot = PaySnapshot.of(base, bonus, allowances, band, fx.usdPerUnit());
        compensation.insert(id, effective, base, band.currency(), bonus, allowances, type, bandLevel, reason, exception,
                Instant.now().toString(), actor, snapshot);
        projection.refreshEmployee(id, calendar.today());
        audit.record(actor, "COMPENSATION_" + type.name(), "employee", Long.toString(id),
                type + " effective " + effective + " base " + MoneyText.format(base) + " " + band.currency()
                        + (reason == null ? "" : " reason: " + reason)
                        + (exception == null ? "" : " exception: " + exception));
        log.info("compensation recorded employeeId={} changeType={} actor={}", id, type, actor);
        return detail(id);
    }

    @Transactional
    public Views.EmployeeDetail patch(String actor, long id, Requests.PatchEmployee request) {
        if (request == null) {
            throw new FieldValidationException(Map.of("body", "is required"));
        }
        Rows.EmployeeRow employee = employees.findById(id).orElseThrow(() -> new NotFoundException("Employee was not found."));
        if (request.firstName() == null && request.lastName() == null && request.jobTitle() == null
                && request.department() == null && request.gender() == null) {
            throw new FieldValidationException(Map.of("body", "provide at least one field to update"));
        }
        Map<String, String> errors = new LinkedHashMap<>();
        String firstName = request.firstName() == null
                ? employee.firstName()
                : TextRules.required(request.firstName(), "firstName", 1, 80, errors);
        String lastName = request.lastName() == null
                ? employee.lastName()
                : TextRules.required(request.lastName(), "lastName", 1, 80, errors);
        String title = request.jobTitle() == null
                ? employee.jobTitle()
                : TextRules.required(request.jobTitle(), "jobTitle", 2, 80, errors);
        Department department = request.department() == null
                ? employee.department()
                : Enums.parse(Department.class, request.department(), "department", errors);
        Gender gender = request.gender() == null
                ? employee.gender()
                : Enums.parse(Gender.class, request.gender(), "gender", errors);
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        employees.updateProfile(id, firstName, lastName, title, department, gender);
        audit.record(actor, "PROFILE_UPDATED", "employee", Long.toString(id), "Updated profile fields");
        log.info("profile updated employeeId={} actor={}", id, actor);
        return detail(id);
    }

    @Transactional
    public Views.EmployeeDetail terminate(String actor, long id, Requests.Terminate request) {
        if (request == null) {
            throw new FieldValidationException(Map.of("body", "is required"));
        }
        Rows.EmployeeRow employee = employees.findById(id).orElseThrow(() -> new NotFoundException("Employee was not found."));
        Map<String, String> errors = new LinkedHashMap<>();
        LocalDate date = Dates.parse(request.terminationDate(), "terminationDate", errors);
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        List<String> violations = new ArrayList<>();
        if (employee.status() == EmployeeStatus.TERMINATED) {
            violations.add("This employee is already terminated.");
        }
        if (date.isBefore(employee.hireDate())) {
            violations.add("Termination date cannot be before the hire date.");
        }
        if (date.isAfter(calendar.today())) {
            violations.add("Termination date cannot be in the future.");
        }
        if (compensation.existsAfter(id, date)) {
            violations.add("Compensation exists after the termination date. Correct that history first.");
        }
        if (!violations.isEmpty()) {
            throw new BusinessRuleException(violations);
        }
        employees.terminate(id, date);
        audit.record(actor, "EMPLOYEE_TERMINATED", "employee", Long.toString(id), "Terminated effective " + date);
        log.info("employee terminated employeeId={} actor={}", id, actor);
        return detail(id);
    }

    private Views.EmployeeDetail detail(long id) {
        Rows.EmployeeRow employee = employees.findById(id).orElseThrow(() -> new NotFoundException("Employee was not found."));
        Rows.CompRow current = compensation.findEffectiveOn(id, calendar.today()).orElse(null);
        SalaryBand band = references.findBand(employee.country(), employee.jobLevel()).orElse(null);
        List<Views.HistoryItem> history = compensation.history(id).stream().map(EmployeeService::history).toList();
        return new Views.EmployeeDetail(
                item(
                        employee,
                        current == null ? null : current.baseSalary(),
                        current == null ? (band == null ? null : band.currency()) : current.currency(),
                        current == null ? null : current.annualBonus(),
                        current == null ? null : current.allowances(),
                        current == null ? null : current.baseSalaryUsd(),
                        current == null ? null : current.totalCompUsd(),
                        current == null ? null : current.compaRatio(),
                        current == null ? null : current.bandPosition()
                ),
                band == null ? null : MoneyText.format(band.min()),
                band == null ? null : MoneyText.format(band.mid()),
                band == null ? null : MoneyText.format(band.max()),
                history
        );
    }

    private static Views.EmployeeItem item(
            Rows.EmployeeRow employee,
            BigDecimal base,
            String currency,
            BigDecimal bonus,
            BigDecimal allowances,
            BigDecimal baseUsd,
            BigDecimal totalUsd,
            BigDecimal compa,
            com.acme.salary.domain.BandPosition position
    ) {
        return new Views.EmployeeItem(
                employee.id(),
                employee.employeeNumber(),
                employee.firstName(),
                employee.lastName(),
                employee.email(),
                employee.country().name(),
                employee.country().label(),
                employee.department().name(),
                employee.department().label(),
                employee.jobTitle(),
                employee.jobLevel().name(),
                employee.status().name(),
                employee.gender().name(),
                employee.gender().label(),
                employee.hireDate().toString(),
                employee.terminationDate() == null ? null : employee.terminationDate().toString(),
                currency,
                MoneyText.format(base),
                MoneyText.format(bonus),
                MoneyText.format(allowances),
                MoneyText.format(baseUsd),
                MoneyText.format(totalUsd),
                MoneyText.formatRatio(compa),
                position == null ? null : position.name()
        );
    }

    private static Views.HistoryItem history(Rows.CompRow row) {
        return new Views.HistoryItem(
                row.id(),
                row.effectiveDate().toString(),
                MoneyText.format(row.baseSalary()),
                row.currency(),
                MoneyText.format(row.annualBonus()),
                MoneyText.format(row.allowances()),
                row.changeType().name(),
                row.jobLevel().name(),
                row.reason(),
                row.exceptionReason(),
                row.createdAt(),
                row.createdBy(),
                MoneyText.format(row.baseSalaryUsd()),
                MoneyText.formatRatio(row.compaRatio()),
                row.bandPosition().name()
        );
    }

    private static RuntimeException conflict(DataAccessException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("unique")) {
            if (message.contains("email")) {
                return new ConflictException("An employee with that email already exists.");
            }
            if (message.contains("employee_number")) {
                return new ConflictException("That employee number is already in use.");
            }
            return new ConflictException("That record conflicts with an existing one.");
        }
        return exception;
    }

    private static EmployeeStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("ACTIVE")) {
            return EmployeeStatus.ACTIVE;
        }
        if (raw.equalsIgnoreCase("ALL")) {
            return null;
        }
        if (raw.equalsIgnoreCase("TERMINATED")) {
            return EmployeeStatus.TERMINATED;
        }
        throw new FieldValidationException(Map.of("status", "must be ACTIVE, TERMINATED, or ALL"));
    }
}
