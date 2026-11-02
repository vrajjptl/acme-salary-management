package com.acme.salary.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({"/", "/login", "/people", "/people/new", "/audit"})
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping("/people/{id}")
    public String person() {
        return "forward:/index.html";
    }
}
