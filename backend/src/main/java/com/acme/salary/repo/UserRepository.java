package com.acme.salary.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Rows.UserRow> findByUsername(String username) {
        return jdbc.query(
                "select username, password_hash, role, enabled from app_user where username = ?",
                (rs, row) -> new Rows.UserRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4) == 1),
                username
        ).stream().findFirst();
    }

    public boolean exists(String username) {
        Integer count = jdbc.queryForObject("select count(*) from app_user where username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    public void insert(String username, String passwordHash, String role) {
        jdbc.update(
                "insert into app_user (username, password_hash, role, enabled) values (?, ?, ?, 1)",
                username,
                passwordHash,
                role
        );
    }
}
