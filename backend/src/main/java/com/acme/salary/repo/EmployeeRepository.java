package com.acme.salary.repo;

import com.acme.salary.domain.BandPosition;
import com.acme.salary.domain.Country;
import com.acme.salary.domain.Department;
import com.acme.salary.domain.EmployeeSort;
import com.acme.salary.domain.EmployeeStatus;
import com.acme.salary.domain.Gender;
import com.acme.salary.domain.JobLevel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class EmployeeRepository {
    private final JdbcTemplate jdbc;

    public EmployeeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean emailExists(String email) {
        Integer count = jdbc.queryForObject("select count(*) from employee where email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public long count() {
        Long count = jdbc.queryForObject("select count(*) from employee", Long.class);
        return count == null ? 0 : count;
    }

    public int countStatus(EmployeeStatus status, Country country, Department department) {
        Query query = filters("e", status, country, department);
        Integer count = jdbc.queryForObject("select count(*) from employee e " + query.where(), Integer.class, query.args().toArray());
        return count == null ? 0 : count;
    }

    public int countActiveWithoutPay(Country country, Department department) {
        Query query = filters("e", EmployeeStatus.ACTIVE, country, department);
        Integer count = jdbc.queryForObject(
                "select count(*) from employee e left join current_compensation c on c.employee_id = e.id "
                        + query.where() + " and c.employee_id is null",
                Integer.class,
                query.args().toArray()
        );
        return count == null ? 0 : count;
    }

    public Optional<Rows.EmployeeRow> findById(long id) {
        return jdbc.query(selectEmployee() + " where id = ?", (rs, row) -> mapEmployee(rs), id).stream().findFirst();
    }

    public long insert(
            String employeeNumber,
            String firstName,
            String lastName,
            String email,
            Country country,
            Department department,
            String jobTitle,
            JobLevel level,
            Gender gender,
            LocalDate hireDate
    ) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    insert into employee (
                      employee_number, first_name, last_name, email, country_code, department,
                      job_title, job_level, status, gender, hire_date, termination_date
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, null)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, employeeNumber);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.setString(4, email);
            statement.setString(5, country.name());
            statement.setString(6, department.name());
            statement.setString(7, jobTitle);
            statement.setString(8, level.name());
            statement.setString(9, gender.name());
            statement.setString(10, hireDate.toString());
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("Employee insert did not return an id");
        }
        return key.longValue();
    }

    public String allocateNumber() {
        Integer next = jdbc.queryForObject("select next_value from employee_number_seq where id = 1", Integer.class);
        if (next == null) {
            throw new IllegalStateException("Employee number sequence is missing");
        }
        jdbc.update("update employee_number_seq set next_value = next_value + 1 where id = 1");
        return String.format("E%06d", next);
    }

    public void setNextNumber(int next) {
        jdbc.update("update employee_number_seq set next_value = ? where id = 1", next);
    }

    public void updateRole(long id, JobLevel level, String jobTitle) {
        jdbc.update("update employee set job_level = ?, job_title = ? where id = ?", level.name(), jobTitle, id);
    }

    public void updateProfile(long id, String firstName, String lastName, String jobTitle, Department department, Gender gender) {
        jdbc.update(
                "update employee set first_name = ?, last_name = ?, job_title = ?, department = ?, gender = ? where id = ?",
                firstName,
                lastName,
                jobTitle,
                department.name(),
                gender.name(),
                id
        );
    }

    public void terminate(long id, LocalDate date) {
        jdbc.update(
                "update employee set status = 'TERMINATED', termination_date = ? where id = ?",
                date.toString(),
                id
        );
    }

    public DirectoryPage directory(
            String search,
            Country country,
            Department department,
            JobLevel level,
            EmployeeStatus status,
            EmployeeSort.Order order,
            int page,
            int size
    ) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1 = 1 ");
        if (status != null) {
            where.append(" and e.status = ? ");
            args.add(status.name());
        }
        if (country != null) {
            where.append(" and e.country_code = ? ");
            args.add(country.name());
        }
        if (department != null) {
            where.append(" and e.department = ? ");
            args.add(department.name());
        }
        if (level != null) {
            where.append(" and e.job_level = ? ");
            args.add(level.name());
        }
        if (search != null) {
            where.append("""
                     and (
                       lower(e.employee_number) like ?
                       or lower(e.first_name) like ?
                       or lower(e.last_name) like ?
                       or lower(e.email) like ?
                       or lower(e.first_name || ' ' || e.last_name) like ?
                     )
                    """);
            for (int i = 0; i < 5; i++) {
                args.add(search);
            }
        }
        Integer total = jdbc.queryForObject("select count(*) from employee e " + where, Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add(page * size);
        List<Rows.DirectoryRow> items = jdbc.query(
                """
                select e.id, e.employee_number, e.first_name, e.last_name, e.email, e.country_code, e.department,
                       e.job_title, e.job_level, e.status, e.gender, e.hire_date, e.termination_date,
                       c.base_salary, c.currency, c.annual_bonus, c.allowances, c.base_salary_usd,
                       c.total_comp_usd, c.compa_ratio, c.band_position
                from employee e
                left join current_compensation c on c.employee_id = e.id
                """ + where + " order by " + EmployeeSort.sql(order) + " limit ? offset ?",
                (rs, row) -> new Rows.DirectoryRow(
                        new Rows.EmployeeRow(
                                rs.getLong("id"),
                                rs.getString("employee_number"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("email"),
                                Country.valueOf(rs.getString("country_code")),
                                Department.valueOf(rs.getString("department")),
                                rs.getString("job_title"),
                                JobLevel.valueOf(rs.getString("job_level")),
                                EmployeeStatus.valueOf(rs.getString("status")),
                                Gender.valueOf(rs.getString("gender")),
                                LocalDate.parse(rs.getString("hire_date")),
                                rs.getString("termination_date") == null ? null : LocalDate.parse(rs.getString("termination_date"))
                        ),
                        money(rs.getString("base_salary")),
                        rs.getString("currency"),
                        money(rs.getString("annual_bonus")),
                        money(rs.getString("allowances")),
                        money(rs.getString("base_salary_usd")),
                        money(rs.getString("total_comp_usd")),
                        rs.getString("compa_ratio") == null ? null : new BigDecimal(rs.getString("compa_ratio")),
                        rs.getString("band_position") == null ? null : BandPosition.valueOf(rs.getString("band_position"))
                ),
                pageArgs.toArray()
        );
        return new DirectoryPage(total == null ? 0 : total, items);
    }

    private static Query filters(String alias, EmployeeStatus status, Country country, Department department) {
        StringBuilder where = new StringBuilder(" where 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (status != null) {
            where.append(" and ").append(alias).append(".status = ? ");
            args.add(status.name());
        }
        if (country != null) {
            where.append(" and ").append(alias).append(".country_code = ? ");
            args.add(country.name());
        }
        if (department != null) {
            where.append(" and ").append(alias).append(".department = ? ");
            args.add(department.name());
        }
        return new Query(where.toString(), args);
    }

    private static String selectEmployee() {
        return """
                select id, employee_number, first_name, last_name, email, country_code, department,
                       job_title, job_level, status, gender, hire_date, termination_date
                from employee
                """;
    }

    private static Rows.EmployeeRow mapEmployee(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Rows.EmployeeRow(
                rs.getLong("id"),
                rs.getString("employee_number"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                Country.valueOf(rs.getString("country_code")),
                Department.valueOf(rs.getString("department")),
                rs.getString("job_title"),
                JobLevel.valueOf(rs.getString("job_level")),
                EmployeeStatus.valueOf(rs.getString("status")),
                Gender.valueOf(rs.getString("gender")),
                LocalDate.parse(rs.getString("hire_date")),
                rs.getString("termination_date") == null ? null : LocalDate.parse(rs.getString("termination_date"))
        );
    }

    private static BigDecimal money(String raw) {
        return raw == null ? null : new BigDecimal(raw);
    }

    public record DirectoryPage(int total, List<Rows.DirectoryRow> items) {
    }

    private record Query(String where, List<Object> args) {
    }
}
