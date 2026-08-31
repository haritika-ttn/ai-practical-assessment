package com.supporttickets.core.exception;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.supporttickets.core.domain.TicketStatus;

/**
 * Raised when a requested status transition is not allowed by the state machine.
 */
public class InvalidTransitionException extends RuntimeException {

    private final TicketStatus currentStatus;
    private final TicketStatus requestedStatus;
    private final List<TicketStatus> allowedTransitions;

    public InvalidTransitionException(
            TicketStatus currentStatus,
            TicketStatus requestedStatus,
            Set<TicketStatus> allowedTransitions) {
        super(String.format(
                "Cannot transition from %s to %s.",
                currentStatus.name(),
                requestedStatus.name()));
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
        this.allowedTransitions = allowedTransitions.stream()
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }

    public TicketStatus getCurrentStatus() {
        return currentStatus;
    }

    public TicketStatus getRequestedStatus() {
        return requestedStatus;
    }

    public List<TicketStatus> getAllowedTransitions() {
        return allowedTransitions;
    }
}
