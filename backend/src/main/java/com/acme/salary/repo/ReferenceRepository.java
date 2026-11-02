package com.acme.salary.repo;

import com.acme.salary.domain.Country;
import com.acme.salary.domain.FxRate;
import com.acme.salary.domain.JobLevel;
import com.acme.salary.domain.MoneyText;
import com.acme.salary.domain.ReferenceCatalog;
import com.acme.salary.domain.SalaryBand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public class ReferenceRepository {
    private final JdbcTemplate jdbc;

    public ReferenceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensureCatalog() {
        Integer bands = jdbc.queryForObject("select count(*) from salary_band", Integer.class);
        if (bands != null && bands > 0) {
            return;
        }
        for (FxRate rate : ReferenceCatalog.rates()) {
            jdbc.update(
                    "insert into fx_rate (currency, usd_per_unit, as_of_date) values (?, ?, ?)",
                    rate.currency(),
                    MoneyText.formatRate(rate.usdPerUnit()),
                    rate.asOf().toString()
            );
        }
        for (SalaryBand band : ReferenceCatalog.bands()) {
            jdbc.update(
                    "insert into salary_band (country_code, job_level, currency, band_min, band_mid, band_max) values (?, ?, ?, ?, ?, ?)",
                    band.country().name(),
                    band.level().name(),
                    band.currency(),
                    MoneyText.format(band.min()),
                    MoneyText.format(band.mid()),
                    MoneyText.format(band.max())
            );
        }
    }

    public Optional<SalaryBand> findBand(Country country, JobLevel level) {
        return jdbc.query(
                "select country_code, job_level, currency, band_min, band_mid, band_max from salary_band where country_code = ? and job_level = ?",
                (rs, row) -> new SalaryBand(
                        Country.valueOf(rs.getString(1)),
                        JobLevel.valueOf(rs.getString(2)),
                        rs.getString(3),
                        new BigDecimal(rs.getString(4)),
                        new BigDecimal(rs.getString(5)),
                        new BigDecimal(rs.getString(6))
                ),
                country.name(),
                level.name()
        ).stream().findFirst();
    }

    public Optional<FxRate> findFx(String currency) {
        return jdbc.query(
                "select currency, usd_per_unit, as_of_date from fx_rate where currency = ?",
                (rs, row) -> new FxRate(rs.getString(1), new BigDecimal(rs.getString(2)), LocalDate.parse(rs.getString(3))),
                currency
        ).stream().findFirst();
    }

    public String fxAsOf() {
        return jdbc.query("select min(as_of_date) from fx_rate", rs -> rs.next() ? rs.getString(1) : null);
    }
}
