package com.acme.salary.web;

import com.acme.salary.error.ApiExceptions.BusinessRuleException;
import com.acme.salary.error.ApiExceptions.ConflictException;
import com.acme.salary.error.ApiExceptions.FieldValidationException;
import com.acme.salary.error.ApiExceptions.NotFoundException;
import com.acme.salary.error.ApiExceptions.TooManyRequestsException;
import com.acme.salary.error.ApiExceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<Map<String, Object>> validation(FieldValidationException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "VALIDATION");
        body.put("message", "Check the highlighted fields.");
        body.put("fields", exception.fields());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> rules(BusinessRuleException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "BUSINESS_RULE");
        body.put("message", "The change was rejected.");
        body.put("violations", exception.violations());
        return ResponseEntity.unprocessableEntity().body(body);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> missing(NotFoundException exception) {
        return ResponseEntity.status(404).body(Map.of("error", "NOT_FOUND", "message", exception.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ConflictException exception) {
        return ResponseEntity.status(409).body(Map.of("error", "CONFLICT", "message", exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> duplicate(DataIntegrityViolationException exception) {
        String specific = exception.getMostSpecificCause().getMessage();
        String message = "That record conflicts with an existing one.";
        if (specific != null && specific.contains("employee.email")) {
            message = "An employee with that email already exists.";
        } else if (specific != null && specific.contains("employee.employee_number")) {
            message = "That employee number is already in use.";
        }
        return ResponseEntity.status(409).body(Map.of("error", "CONFLICT", "message", message));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> unauthorized(UnauthorizedException exception) {
        return ResponseEntity.status(401).body(Map.of("error", "UNAUTHENTICATED", "message", exception.getMessage()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> tooMany(TooManyRequestsException exception) {
        return ResponseEntity.status(429).body(Map.of("error", "TOO_MANY_REQUESTS", "message", exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION",
                "message", "Request body contains an unsupported or invalid value.",
                "fields", Map.of()
        ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> method(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(405).body(Map.of("error", "METHOD_NOT_ALLOWED", "message", "That action is not available."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception exception) {
        String errorId = UUID.randomUUID().toString();
        log.error("errorId={}", errorId, exception);
        return ResponseEntity.internalServerError().body(Map.of(
                "error", "INTERNAL",
                "message", "Something went wrong.",
                "errorId", errorId
        ));
    }
}
