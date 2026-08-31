package com.supporttickets.core.service;

import java.util.Set;

import com.supporttickets.core.domain.TicketStatus;

/**
 * Domain service for ticket lifecycle transitions.
 */
public interface TicketStateMachineService {

    Set<TicketStatus> getAllowedTransitions(TicketStatus current);

    void validateTransition(TicketStatus current, TicketStatus requested);

    TicketStatus applyTransition(TicketStatus current, TicketStatus requested);
}
