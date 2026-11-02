package com.acme.salary.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class AuditRepository {
    private final JdbcTemplate jdbc;

    public AuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(String actor, String action, String entityType, String entityId, String summary) {
        jdbc.update(
                "insert into audit_event (occurred_at, actor, action, entity_type, entity_id, summary) values (?, ?, ?, ?, ?, ?)",
                Instant.now().toString(),
                actor,
                action,
                entityType,
                entityId,
                summary
        );
    }

    public long count() {
        Long count = jdbc.queryForObject("select count(*) from audit_event", Long.class);
        return count == null ? 0 : count;
    }

    public List<AuditRow> page(int limit, int offset) {
        return jdbc.query(
                """
                select id, occurred_at, actor, action, entity_type, entity_id, summary
                from audit_event
                order by id desc
                limit ? offset ?
                """,
                (rs, row) -> new AuditRow(
                        rs.getLong("id"),
                        rs.getString("occurred_at"),
                        rs.getString("actor"),
                        rs.getString("action"),
                        rs.getString("entity_type"),
                        rs.getString("entity_id"),
                        rs.getString("summary")
                ),
                limit,
                offset
        );
    }

    public record AuditRow(
            long id,
            String occurredAt,
            String actor,
            String action,
            String entityType,
            String entityId,
            String summary
    ) {
    }
}
