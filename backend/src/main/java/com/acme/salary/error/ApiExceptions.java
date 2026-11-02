package com.acme.salary.error;

import java.util.List;
import java.util.Map;

public final class ApiExceptions {
    private ApiExceptions() {
    }

    public static final class FieldValidationException extends RuntimeException {
        private final Map<String, String> fields;

        public FieldValidationException(Map<String, String> fields) {
            super("Validation failed");
            this.fields = Map.copyOf(fields);
        }

        public Map<String, String> fields() {
            return fields;
        }
    }

    public static final class BusinessRuleException extends RuntimeException {
        private final List<String> violations;

        public BusinessRuleException(List<String> violations) {
            super("Business rule rejected the change");
            this.violations = List.copyOf(violations);
        }

        public List<String> violations() {
            return violations;
        }
    }

    public static final class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static final class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    public static final class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    public static final class TooManyRequestsException extends RuntimeException {
        public TooManyRequestsException(String message) {
            super(message);
        }
    }
}
