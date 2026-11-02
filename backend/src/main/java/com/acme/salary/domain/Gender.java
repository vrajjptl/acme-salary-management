package com.acme.salary.domain;

public enum Gender {
    FEMALE("Female"),
    MALE("Male"),
    NON_BINARY("Non-binary"),
    UNDISCLOSED("Undisclosed");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
