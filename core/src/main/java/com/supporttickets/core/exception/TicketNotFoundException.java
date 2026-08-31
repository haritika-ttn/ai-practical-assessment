package com.supporttickets.core.exception;

/**
 * Raised when a ticket node does not exist.
 */
public class TicketNotFoundException extends RuntimeException {

    private final String ticketId;

    public TicketNotFoundException(String ticketId) {
        super("Ticket not found: " + ticketId);
        this.ticketId = ticketId;
    }

    public String getTicketId() {
        return ticketId;
    }
}
