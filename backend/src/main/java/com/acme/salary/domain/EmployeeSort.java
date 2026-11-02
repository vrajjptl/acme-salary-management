package com.acme.salary.domain;

import com.acme.salary.error.ApiExceptions.FieldValidationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EmployeeSort {
    public record Order(List<String> columns, boolean descending) {
    }

    private EmployeeSort() {
    }

    public static Order parse(String sort, String direction) {
        Map<String, String> errors = new LinkedHashMap<>();
        boolean descending;
        if (direction == null || direction.isBlank() || direction.equalsIgnoreCase("asc")) {
            descending = false;
        } else if (direction.equalsIgnoreCase("desc")) {
            descending = true;
        } else {
            descending = false;
            errors.put("direction", "must be asc or desc");
        }
        String key = sort == null || sort.isBlank() ? "name" : sort.trim();
        List<String> columns = switch (key) {
            case "name" -> List.of("e.last_name", "e.first_name");
            case "employeeNumber" -> List.of("e.employee_number");
            case "country" -> List.of("e.country_code");
            case "department" -> List.of("e.department");
            case "level" -> List.of("e.job_level");
            case "hireDate" -> List.of("e.hire_date");
            case "baseSalaryUsd" -> List.of("CAST(c.base_salary_usd AS REAL)");
            default -> null;
        };
        if (columns == null) {
            errors.put("sort", "is not a supported sort field");
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        return new Order(columns, descending);
    }

    public static String sql(Order order) {
        String direction = order.descending() ? " desc" : " asc";
        StringBuilder builder = new StringBuilder();
        for (String column : order.columns()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(column).append(direction);
        }
        builder.append(", e.employee_number asc");
        return builder.toString();
    }
}
