package com.acme.salary.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class BusinessCalendar {
    private final LocalDate fixed;

    public BusinessCalendar(@Value("${app.business-date:}") String fixed) {
        this.fixed = fixed == null || fixed.isBlank() ? null : LocalDate.parse(fixed);
    }

    public LocalDate today() {
        return fixed != null ? fixed : LocalDate.now(ZoneOffset.UTC);
    }
}
