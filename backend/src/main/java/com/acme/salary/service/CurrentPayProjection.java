package com.acme.salary.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class CurrentPayProjection {
    private static final String INSERT_WINNERS = """
            insert into current_compensation (
              employee_id, compensation_id, effective_date, base_salary, currency, annual_bonus, allowances,
              base_salary_usd, total_comp_usd, compa_ratio, band_min, band_mid, band_max, band_position
            )
            select employee_id, id, effective_date, base_salary, currency, annual_bonus, allowances,
                   base_salary_usd, total_comp_usd, compa_ratio, band_min, band_mid, band_max, band_position
            from (
              select employee_id, id, effective_date, base_salary, currency, annual_bonus, allowances,
                     base_salary_usd, total_comp_usd, compa_ratio, band_min, band_mid, band_max, band_position,
                     row_number() over (partition by employee_id order by effective_date desc, id desc) as rn
              from compensation_record
              where effective_date <= ?
            ) ranked
            where rn = 1
            """;

    private final JdbcTemplate jdbc;
    private final Object lock = new Object();
    private LocalDate builtFor;

    public CurrentPayProjection(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void ensureCurrent(LocalDate today) {
        synchronized (lock) {
            if (today.equals(builtFor)) {
                return;
            }
            rebuild(today);
            builtFor = today;
        }
    }

    @Transactional
    public void refreshEmployee(long employeeId, LocalDate today) {
        jdbc.update("delete from current_compensation where employee_id = ?", employeeId);
        jdbc.update(
                """
                insert into current_compensation (
                  employee_id, compensation_id, effective_date, base_salary, currency, annual_bonus, allowances,
                  base_salary_usd, total_comp_usd, compa_ratio, band_min, band_mid, band_max, band_position
                )
                select employee_id, id, effective_date, base_salary, currency, annual_bonus, allowances,
                       base_salary_usd, total_comp_usd, compa_ratio, band_min, band_mid, band_max, band_position
                from compensation_record
                where employee_id = ? and effective_date <= ?
                order by effective_date desc, id desc
                limit 1
                """,
                employeeId,
                today.toString()
        );
    }

    public void clearDayCache() {
        synchronized (lock) {
            builtFor = null;
        }
    }

    private void rebuild(LocalDate today) {
        jdbc.update("delete from current_compensation");
        jdbc.update(INSERT_WINNERS, today.toString());
    }
}
