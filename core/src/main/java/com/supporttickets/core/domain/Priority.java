package com.supporttickets.core.domain;

import java.util.Locale;

/**
 * Ticket priority values.
 */
public enum Priority {

    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static Priority fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Priority must not be blank");
        }
        return Priority.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
