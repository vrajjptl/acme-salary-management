package com.acme.salary.seed;

import com.acme.salary.repo.EmployeeRepository;
import com.acme.salary.service.BusinessCalendar;
import com.acme.salary.service.CurrentPayProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkforceSeeder {
    private static final Logger log = LoggerFactory.getLogger(WorkforceSeeder.class);
    private static final int BATCH = 500;

    private final JdbcTemplate jdbc;
    private final EmployeeRepository employees;
    private final CurrentPayProjection projection;
    private final BusinessCalendar calendar;

    public WorkforceSeeder(
            JdbcTemplate jdbc,
            EmployeeRepository employees,
            CurrentPayProjection projection,
            BusinessCalendar calendar
    ) {
        this.jdbc = jdbc;
        this.employees = employees;
        this.projection = projection;
        this.calendar = calendar;
    }

    @Transactional
    public void seedIfEmpty(int count) {
        if (employees.count() > 0) {
            return;
        }
        long started = System.nanoTime();
        SeedFactory.SeedBundle bundle = SeedFactory.generate(count, calendar.today(), new java.util.Random(42));
        insertEmployees(bundle.employees());
        insertCompensation(bundle.employees());
        projection.clearDayCache();
        projection.ensureCurrent(calendar.today());
        employees.setNextNumber(count + 1);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        log.info("seeded employees={} elapsedMs={}", count, elapsedMs);
    }

    private void insertEmployees(List<SeedFactory.SeedEmployee> people) {
        for (int offset = 0; offset < people.size(); offset += BATCH) {
            List<SeedFactory.SeedEmployee> chunk = people.subList(offset, Math.min(offset + BATCH, people.size()));
            jdbc.batchUpdate(
                    """
                    insert into employee (
                      employee_number, first_name, last_name, email, country_code, department, job_title,
                      job_level, status, gender, hire_date, termination_date
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(java.sql.PreparedStatement statement, int index) throws java.sql.SQLException {
                            SeedFactory.SeedEmployee employee = chunk.get(index);
                            statement.setString(1, employee.employeeNumber());
                            statement.setString(2, employee.firstName());
                            statement.setString(3, employee.lastName());
                            statement.setString(4, employee.email());
                            statement.setString(5, employee.country().name());
                            statement.setString(6, employee.department().name());
                            statement.setString(7, employee.jobTitle());
                            statement.setString(8, employee.jobLevel().name());
                            statement.setString(9, employee.status().name());
                            statement.setString(10, employee.gender().name());
                            statement.setString(11, employee.hireDate().toString());
                            if (employee.terminationDate() == null) {
                                statement.setString(12, null);
                            } else {
                                statement.setString(12, employee.terminationDate().toString());
                            }
                        }

                        @Override
                        public int getBatchSize() {
                            return chunk.size();
                        }
                    }
            );
        }
    }

    private void insertCompensation(List<SeedFactory.SeedEmployee> people) {
        var ids = new java.util.HashMap<String, Long>();
        jdbc.query("select id, employee_number from employee", rs -> {
            ids.put(rs.getString("employee_number"), rs.getLong("id"));
        });
        List<FlatComp> rows = new java.util.ArrayList<>();
        for (SeedFactory.SeedEmployee employee : people) {
            Long id = ids.get(employee.employeeNumber());
            if (id == null) {
                throw new IllegalStateException("Missing id for " + employee.employeeNumber());
            }
            for (SeedFactory.SeedComp comp : employee.history()) {
                rows.add(new FlatComp(id, comp));
            }
        }
        String createdAt = calendar.today() + "T00:00:00Z";
        for (int offset = 0; offset < rows.size(); offset += BATCH) {
            List<FlatComp> chunk = rows.subList(offset, Math.min(offset + BATCH, rows.size()));
            jdbc.batchUpdate(
                    """
                    insert into compensation_record (
                      employee_id, effective_date, base_salary, currency, annual_bonus, allowances, change_type,
                      job_level, reason, exception_reason, created_at, created_by, base_salary_usd, total_comp_usd,
                      compa_ratio, band_min, band_mid, band_max, band_position
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'seed', ?, ?, ?, ?, ?, ?, ?)
                    """,
                    new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(java.sql.PreparedStatement statement, int index) throws java.sql.SQLException {
                            FlatComp row = chunk.get(index);
                            SeedFactory.SeedComp comp = row.comp();
                            statement.setLong(1, row.employeeId());
                            statement.setString(2, comp.effectiveDate().toString());
                            statement.setString(3, com.acme.salary.domain.MoneyText.format(comp.baseSalary()));
                            statement.setString(4, comp.currency());
                            statement.setString(5, com.acme.salary.domain.MoneyText.format(comp.annualBonus()));
                            statement.setString(6, com.acme.salary.domain.MoneyText.format(comp.allowances()));
                            statement.setString(7, comp.changeType().name());
                            statement.setString(8, comp.jobLevel().name());
                            statement.setString(9, comp.reason());
                            statement.setString(10, comp.exceptionReason());
                            statement.setString(11, createdAt);
                            statement.setString(12, com.acme.salary.domain.MoneyText.format(comp.snapshot().baseSalaryUsd()));
                            statement.setString(13, com.acme.salary.domain.MoneyText.format(comp.snapshot().totalCompUsd()));
                            statement.setString(14, com.acme.salary.domain.MoneyText.formatRatio(comp.snapshot().compaRatio()));
                            statement.setString(15, com.acme.salary.domain.MoneyText.format(comp.snapshot().bandMin()));
                            statement.setString(16, com.acme.salary.domain.MoneyText.format(comp.snapshot().bandMid()));
                            statement.setString(17, com.acme.salary.domain.MoneyText.format(comp.snapshot().bandMax()));
                            statement.setString(18, comp.snapshot().bandPosition().name());
                        }

                        @Override
                        public int getBatchSize() {
                            return chunk.size();
                        }
                    }
            );
        }
    }

    private record FlatComp(long employeeId, SeedFactory.SeedComp comp) {
    }
}
