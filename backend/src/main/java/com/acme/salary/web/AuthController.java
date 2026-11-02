package com.acme.salary.web;

import com.acme.salary.dto.Requests;
import com.acme.salary.dto.Views;
import com.acme.salary.error.ApiExceptions.FieldValidationException;
import com.acme.salary.error.ApiExceptions.TooManyRequestsException;
import com.acme.salary.error.ApiExceptions.UnauthorizedException;
import com.acme.salary.security.HrPrincipal;
import com.acme.salary.security.LoginThrottle;
import com.acme.salary.service.AuditTrail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationConfiguration authenticationConfiguration;
    private final SecurityContextRepository securityContextRepository;
    private final LoginThrottle throttle;
    private final AuditTrail audit;

    public AuthController(
            AuthenticationConfiguration authenticationConfiguration,
            SecurityContextRepository securityContextRepository,
            LoginThrottle throttle,
            AuditTrail audit
    ) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.securityContextRepository = securityContextRepository;
        this.throttle = throttle;
        this.audit = audit;
    }

    @PostMapping("/login")
    public Views.UserView login(
            @RequestBody Requests.Login body,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {
        Map<String, String> errors = new LinkedHashMap<>();
        if (body == null || body.username() == null || body.username().isBlank()) {
            errors.put("username", "is required");
        }
        if (body == null || body.password() == null || body.password().isBlank()) {
            errors.put("password", "is required");
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        String ip = request.getRemoteAddr();
        if (!throttle.allow(ip)) {
            throw new TooManyRequestsException("Too many sign-in attempts. Try again later.");
        }
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        try {
            AuthenticationManager manager = authenticationConfiguration.getAuthenticationManager();
            Authentication authentication = manager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username().trim(), body.password())
            );
            UserDetails details = (UserDetails) authentication.getPrincipal();
            String role = "HR_MANAGER";
            for (GrantedAuthority authority : details.getAuthorities()) {
                String value = authority.getAuthority();
                role = value.startsWith("ROLE_") ? value.substring("ROLE_".length()) : value;
            }
            HrPrincipal principal = new HrPrincipal(details.getUsername(), role);
            UsernamePasswordAuthenticationToken cleaned = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(cleaned);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            throttle.recordSuccess(ip);
            audit.record(details.getUsername(), "LOGIN_SUCCESS", "session", "auth", "Signed in");
            return new Views.UserView(details.getUsername(), role);
        } catch (AuthenticationException ex) {
            throttle.recordFailure(ip);
            audit.record("anonymous", "LOGIN_FAILURE", "session", "auth", "Failed sign-in attempt");
            throw new UnauthorizedException("Invalid username or password.");
        }
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String actor = "anonymous";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof HrPrincipal principal) {
            actor = principal.getUsername();
        }
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        audit.record(actor, "LOGOUT", "session", "auth", "Signed out");
        response.setStatus(204);
    }

    @GetMapping("/me")
    public Views.UserView me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Sign in required.");
        }
        if (authentication.getPrincipal() instanceof HrPrincipal principal) {
            return new Views.UserView(principal.getUsername(), principal.role());
        }
        return new Views.UserView(authentication.getName(), "HR_MANAGER");
    }
}
