package com.acme.salary.web;

import com.acme.salary.dto.Requests;
import com.acme.salary.dto.Views;
import com.acme.salary.security.HrPrincipal;
import com.acme.salary.service.EmployeeService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService employees;

    public EmployeeController(EmployeeService employees) {
        this.employees = employees;
    }

    @GetMapping
    public Views.EmployeePage list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String level,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false, defaultValue = "asc") String direction,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int size
    ) {
        return employees.list(q, country, department, level, status, sort, direction, page, size);
    }

    @GetMapping("/{id}")
    public Views.EmployeeDetail get(@PathVariable long id) {
        return employees.get(id);
    }

    @PostMapping
    public Views.EmployeeDetail create(@RequestBody Requests.CreateEmployee request, Authentication authentication) {
        return employees.create(actor(authentication), request);
    }

    @PostMapping("/{id}/compensation")
    public Views.EmployeeDetail change(
            @PathVariable long id,
            @RequestBody Requests.ChangeCompensation request,
            Authentication authentication
    ) {
        return employees.change(actor(authentication), id, request);
    }

    @PatchMapping("/{id}")
    public Views.EmployeeDetail patch(
            @PathVariable long id,
            @RequestBody Requests.PatchEmployee request,
            Authentication authentication
    ) {
        return employees.patch(actor(authentication), id, request);
    }

    @PostMapping("/{id}/terminate")
    public Views.EmployeeDetail terminate(
            @PathVariable long id,
            @RequestBody Requests.Terminate request,
            Authentication authentication
    ) {
        return employees.terminate(actor(authentication), id, request);
    }

    private static String actor(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof HrPrincipal principal) {
            return principal.getUsername();
        }
        return authentication == null ? "unknown" : authentication.getName();
    }
}
