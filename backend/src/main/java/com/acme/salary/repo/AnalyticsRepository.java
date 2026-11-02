package com.acme.salary.repo;

import com.acme.salary.domain.BandPosition;
import com.acme.salary.domain.Country;
import com.acme.salary.domain.Department;
import com.acme.salary.domain.Gender;
import com.acme.salary.domain.OrgPayCalculator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AnalyticsRepository {
    private final JdbcTemplate jdbc;

    public AnalyticsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<OrgPayCalculator.PayFact> activePay(Country country, Department department) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where e.status = 'ACTIVE' ");
        if (country != null) {
            where.append(" and e.country_code = ? ");
            args.add(country.name());
        }
        if (department != null) {
            where.append(" and e.department = ? ");
            args.add(department.name());
        }
        return jdbc.query(
                """
                select e.country_code, e.department, e.job_level, e.gender, c.base_salary_usd, c.total_comp_usd, c.band_position
                from employee e
                join current_compensation c on c.employee_id = e.id
                """ + where,
                (rs, row) -> {
                    Country personCountry = Country.valueOf(rs.getString("country_code"));
                    Department personDepartment = Department.valueOf(rs.getString("department"));
                    Gender gender = Gender.valueOf(rs.getString("gender"));
                    return new OrgPayCalculator.PayFact(
                            personCountry.name(),
                            personCountry.label(),
                            personDepartment.name(),
                            personDepartment.label(),
                            rs.getString("job_level"),
                            gender.name(),
                            gender.label(),
                            new BigDecimal(rs.getString("base_salary_usd")),
                            new BigDecimal(rs.getString("total_comp_usd")),
                            BandPosition.valueOf(rs.getString("band_position"))
                    );
                },
                args.toArray()
        );
    }
}
