package com.acme.salary.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            @Value("${app.cors.allowed-origins:}") String allowedOrigins
    ) throws Exception {
        if (StringUtils.hasText(allowedOrigins)) {
            List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
            http.cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(origins);
                configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Requested-With"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);
                return configuration;
            }));
        } else {
            http.cors(AbstractHttpConfigurer::disable);
        }

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(csrfCookieFilter(), CsrfFilter.class)
                .addFilterAfter(noStoreFilter(), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).sessionFixation().changeSessionId())
                .securityContext(context -> context.securityContextRepository(securityContextRepository).requireExplicitSave(true))
                .anonymous(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; style-src 'self' 'unsafe-inline'; frame-ancestors 'none'; form-action 'self'; base-uri 'self'"))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, authException) -> write(response, 401, "UNAUTHENTICATED", "Sign in required."))
                        .accessDeniedHandler((request, response, accessDeniedException) -> write(response, 403, "FORBIDDEN", "You cannot do that.")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.svg", "/assets/**", "/login", "/people", "/people/**", "/audit").permitAll()
                        .requestMatchers("/api/**").hasRole("HR_MANAGER")
                        .anyRequest().permitAll());
        return http.build();
    }

    private static OncePerRequestFilter csrfCookieFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    jakarta.servlet.FilterChain filterChain
            ) throws jakarta.servlet.ServletException, java.io.IOException {
                CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (token != null) {
                    token.getToken();
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    private static OncePerRequestFilter noStoreFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    jakarta.servlet.FilterChain filterChain
            ) throws jakarta.servlet.ServletException, java.io.IOException {
                if (request.getRequestURI().startsWith("/api/")) {
                    response.setHeader("Cache-Control", "no-store");
                    response.setHeader("Pragma", "no-cache");
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    private static void write(HttpServletResponse response, int status, String error, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }

    static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            this.xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(jakarta.servlet.http.HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
