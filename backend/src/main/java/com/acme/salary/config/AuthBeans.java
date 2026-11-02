package com.acme.salary.config;

import com.acme.salary.repo.UserRepository;
import com.acme.salary.security.LoginThrottle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Duration;
import java.time.Instant;

@Configuration
public class AuthBeans {
    @Bean
    public LoginThrottle loginThrottle(
            @Value("${app.auth.max-failures}") int maxFailures,
            @Value("${app.auth.window-seconds}") long windowSeconds
    ) {
        return new LoginThrottle(maxFailures, Duration.ofSeconds(windowSeconds), Instant::now);
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByUsername(username)
                .map(user -> User.withUsername(user.username())
                        .password(user.passwordHash())
                        .roles(user.role())
                        .disabled(!user.enabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));
    }
}
