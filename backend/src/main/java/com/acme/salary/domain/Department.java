package com.acme.salary.domain;

public enum Department {
    ENGINEERING("Engineering"),
    PRODUCT("Product"),
    DESIGN("Design"),
    SALES("Sales"),
    PEOPLE("People"),
    FINANCE("Finance"),
    MARKETING("Marketing"),
    OPERATIONS("Operations"),
    LEGAL("Legal"),
    SUPPORT("Support");

    private final String label;

    Department(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
