package com.acme.salary.seed;

import com.acme.salary.repo.ReferenceRepository;
import com.acme.salary.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedRunner {
    @Bean
    ApplicationRunner prepareDatabase(
            ReferenceRepository references,
            UserRepository users,
            PasswordEncoder passwordEncoder,
            WorkforceSeeder seeder,
            @Value("${app.seed.enabled}") boolean seedEnabled,
            @Value("${app.seed.employees}") int employees,
            @Value("${app.demo.username}") String username,
            @Value("${app.demo.password}") String password
    ) {
        return args -> {
            references.ensureCatalog();
            if (!users.exists(username)) {
                users.insert(username, passwordEncoder.encode(password), "HR_MANAGER");
            }
            if (seedEnabled) {
                seeder.seedIfEmpty(employees);
            }
        };
    }
}
