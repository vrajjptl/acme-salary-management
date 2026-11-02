package com.acme.salary.service;

import com.acme.salary.domain.Country;
import com.acme.salary.domain.Department;
import com.acme.salary.domain.EmployeeStatus;
import com.acme.salary.domain.Enums;
import com.acme.salary.domain.JobLevel;
import com.acme.salary.domain.OrgPayCalculator;
import com.acme.salary.dto.Views;
import com.acme.salary.error.ApiExceptions.FieldValidationException;
import com.acme.salary.repo.AnalyticsRepository;
import com.acme.salary.repo.EmployeeRepository;
import com.acme.salary.repo.ReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {
    private final AnalyticsRepository analytics;
    private final EmployeeRepository employees;
    private final ReferenceRepository references;
    private final CurrentPayProjection projection;
    private final BusinessCalendar calendar;

    public AnalyticsService(
            AnalyticsRepository analytics,
            EmployeeRepository employees,
            ReferenceRepository references,
            CurrentPayProjection projection,
            BusinessCalendar calendar
    ) {
        this.analytics = analytics;
        this.employees = employees;
        this.references = references;
        this.projection = projection;
        this.calendar = calendar;
    }

    @Transactional
    public Views.AnalyticsView overview(String country, String department) {
        Map<String, String> errors = new LinkedHashMap<>();
        Country countryValue = Enums.parseOptional(Country.class, country, "country", errors);
        Department departmentValue = Enums.parseOptional(Department.class, department, "department", errors);
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        projection.ensureCurrent(calendar.today());
        OrgPayCalculator.Summary pay = OrgPayCalculator.summarize(
                analytics.activePay(countryValue, departmentValue),
                Arrays.stream(JobLevel.values()).map(Enum::name).toList()
        );
        String fxAsOf = references.fxAsOf();
        return new Views.AnalyticsView(
                pay.headcount(),
                employees.countStatus(EmployeeStatus.TERMINATED, countryValue, departmentValue),
                employees.countActiveWithoutPay(countryValue, departmentValue),
                pay,
                fxAsOf,
                countryValue == null ? null : countryValue.name(),
                departmentValue == null ? null : departmentValue.name(),
                List.of(
                        "Payroll figures include active employees whose pay is already in effect.",
                        "Former employees stay on file and are excluded from pay statistics.",
                        "USD amounts use the FX snapshot dated " + fxAsOf + ".",
                        "Compa-ratio and band checks use annual base salary. Total compensation includes bonus and allowances.",
                        "Gender groups smaller than 5 people in the current filter are omitted."
                )
        );
    }
}
