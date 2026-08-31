package com.supporttickets.core.domain;

import java.util.Locale;

/**
 * Ticket lifecycle status values.
 */
public enum TicketStatus {

    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    public static TicketStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status must not be blank");
        }
        return TicketStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
