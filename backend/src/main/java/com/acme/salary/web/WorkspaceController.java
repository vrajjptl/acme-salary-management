package com.acme.salary.web;

import com.acme.salary.domain.ChangeType;
import com.acme.salary.domain.Country;
import com.acme.salary.domain.Department;
import com.acme.salary.domain.Gender;
import com.acme.salary.domain.JobLevel;
import com.acme.salary.dto.Views;
import com.acme.salary.service.AnalyticsService;
import com.acme.salary.service.AuditTrail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class WorkspaceController {
    private final AnalyticsService analytics;
    private final AuditTrail audit;

    public WorkspaceController(AnalyticsService analytics, AuditTrail audit) {
        this.analytics = analytics;
        this.audit = audit;
    }

    @GetMapping("/api/health")
    public java.util.Map<String, String> health() {
        return java.util.Map.of("status", "UP");
    }

    @GetMapping("/api/analytics/overview")
    public Views.AnalyticsView overview(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String department
    ) {
        return analytics.overview(country, department);
    }

    @GetMapping("/api/audit")
    public Views.AuditPage audit(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int size
    ) {
        return audit.page(page, size);
    }

    @GetMapping("/api/reference/meta")
    public Views.Meta meta() {
        List<Views.Coded> countries = Arrays.stream(Country.values())
                .map(country -> new Views.Coded(country.name(), country.label()))
                .toList();
        List<Views.Coded> departments = Arrays.stream(Department.values())
                .map(department -> new Views.Coded(department.name(), department.label()))
                .toList();
        List<Views.Coded> genders = Arrays.stream(Gender.values())
                .map(gender -> new Views.Coded(gender.name(), gender.label()))
                .toList();
        List<String> changes = Arrays.stream(ChangeType.values())
                .filter(type -> type != ChangeType.HIRE)
                .map(Enum::name)
                .toList();
        return new Views.Meta(
                countries,
                departments,
                Arrays.stream(JobLevel.values()).map(Enum::name).toList(),
                changes,
                genders
        );
    }
}
