package com.acme.salary.repo;

import com.acme.salary.domain.BandPosition;
import com.acme.salary.domain.ChangeType;
import com.acme.salary.domain.JobLevel;
import com.acme.salary.domain.MoneyText;
import com.acme.salary.domain.PaySnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class CompensationRepository {
    private static final String SELECT = """
            select id, employee_id, effective_date, base_salary, currency, annual_bonus, allowances,
                   change_type, job_level, reason, exception_reason, created_at, created_by,
                   base_salary_usd, total_comp_usd, compa_ratio, band_min, band_mid, band_max, band_position
            from compensation_record
            """;

    private final JdbcTemplate jdbc;

    public CompensationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Rows.CompRow> findLatest(long employeeId) {
        return jdbc.query(
                SELECT + " where employee_id = ? order by effective_date desc, id desc limit 1",
                (rs, row) -> map(rs),
                employeeId
        ).stream().findFirst();
    }

    public Optional<Rows.CompRow> findEffectiveOn(long employeeId, LocalDate today) {
        return jdbc.query(
                SELECT + " where employee_id = ? and effective_date <= ? order by effective_date desc, id desc limit 1",
                (rs, row) -> map(rs),
                employeeId,
                today.toString()
        ).stream().findFirst();
    }

    public boolean existsAfter(long employeeId, LocalDate date) {
        Integer count = jdbc.queryForObject(
                "select count(*) from compensation_record where employee_id = ? and effective_date > ?",
                Integer.class,
                employeeId,
                date.toString()
        );
        return count != null && count > 0;
    }

    public List<Rows.CompRow> history(long employeeId) {
        return jdbc.query(
                SELECT + " where employee_id = ? order by effective_date desc, id desc limit 200",
                (rs, row) -> map(rs),
                employeeId
        );
    }

    public long insert(
            long employeeId,
            LocalDate effectiveDate,
            BigDecimal base,
            String currency,
            BigDecimal bonus,
            BigDecimal allowances,
            ChangeType changeType,
            JobLevel level,
            String reason,
            String exceptionReason,
            String createdAt,
            String createdBy,
            PaySnapshot snapshot
    ) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    insert into compensation_record (
                      employee_id, effective_date, base_salary, currency, annual_bonus, allowances,
                      change_type, job_level, reason, exception_reason, created_at, created_by,
                      base_salary_usd, total_comp_usd, compa_ratio, band_min, band_mid, band_max, band_position
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, employeeId);
            statement.setString(2, effectiveDate.toString());
            statement.setString(3, MoneyText.format(base));
            statement.setString(4, currency);
            statement.setString(5, MoneyText.format(bonus));
            statement.setString(6, MoneyText.format(allowances));
            statement.setString(7, changeType.name());
            statement.setString(8, level.name());
            statement.setString(9, reason);
            statement.setString(10, exceptionReason);
            statement.setString(11, createdAt);
            statement.setString(12, createdBy);
            statement.setString(13, MoneyText.format(snapshot.baseSalaryUsd()));
            statement.setString(14, MoneyText.format(snapshot.totalCompUsd()));
            statement.setString(15, MoneyText.formatRatio(snapshot.compaRatio()));
            statement.setString(16, MoneyText.format(snapshot.bandMin()));
            statement.setString(17, MoneyText.format(snapshot.bandMid()));
            statement.setString(18, MoneyText.format(snapshot.bandMax()));
            statement.setString(19, snapshot.bandPosition().name());
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("Compensation insert did not return an id");
        }
        return key.longValue();
    }

    private static Rows.CompRow map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Rows.CompRow(
                rs.getLong("id"),
                rs.getLong("employee_id"),
                LocalDate.parse(rs.getString("effective_date")),
                new BigDecimal(rs.getString("base_salary")),
                rs.getString("currency"),
                new BigDecimal(rs.getString("annual_bonus")),
                new BigDecimal(rs.getString("allowances")),
                ChangeType.valueOf(rs.getString("change_type")),
                JobLevel.valueOf(rs.getString("job_level")),
                rs.getString("reason"),
                rs.getString("exception_reason"),
                rs.getString("created_at"),
                rs.getString("created_by"),
                new BigDecimal(rs.getString("base_salary_usd")),
                new BigDecimal(rs.getString("total_comp_usd")),
                new BigDecimal(rs.getString("compa_ratio")),
                new BigDecimal(rs.getString("band_min")),
                new BigDecimal(rs.getString("band_mid")),
                new BigDecimal(rs.getString("band_max")),
                BandPosition.valueOf(rs.getString("band_position"))
        );
    }
}
