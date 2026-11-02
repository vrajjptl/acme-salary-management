package com.acme.salary.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class SqliteConfig {
    @Bean
    public DataSource dataSource(DataSourceProperties properties) throws Exception {
        ensureParent(properties.getUrl());
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setMaximumPoolSize(1);
        dataSource.setMinimumIdle(1);
        dataSource.setPoolName("acme-sqlite");
        dataSource.setConnectionTimeout(10_000);
        dataSource.setConnectionInitSql("PRAGMA foreign_keys=ON");
        return dataSource;
    }

    @Bean
    ApplicationRunner sqlitePragmas(DataSource dataSource, @Value("${spring.datasource.url}") String url) {
        return args -> {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys=ON");
                statement.execute("PRAGMA busy_timeout=5000");
                if (url == null || !url.contains("mode=memory")) {
                    statement.execute("PRAGMA journal_mode=WAL");
                }
            }
        };
    }

    private static void ensureParent(String url) throws Exception {
        if (url == null || !url.startsWith("jdbc:sqlite:") || url.contains("mode=memory")) {
            return;
        }
        String path = url.substring("jdbc:sqlite:".length());
        if (path.startsWith("file:")) {
            return;
        }
        Path database = Path.of(path);
        Path parent = database.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
