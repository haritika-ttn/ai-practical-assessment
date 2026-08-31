package com.supporttickets.core.service.impl;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.InvalidTransitionException;
import com.supporttickets.core.service.TicketStateMachineService;

/**
 * Static transition map for ticket lifecycle rules.
 */
@Component(service = TicketStateMachineService.class)
public class TicketStateMachineServiceImpl implements TicketStateMachineService {

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS;

    static {
        Map<TicketStatus, Set<TicketStatus>> transitions = new EnumMap<>(TicketStatus.class);
        transitions.put(TicketStatus.OPEN, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED));
        transitions.put(TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED));
        transitions.put(TicketStatus.RESOLVED, EnumSet.of(TicketStatus.CLOSED));
        transitions.put(TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class));
        transitions.put(TicketStatus.CANCELLED, EnumSet.noneOf(TicketStatus.class));
        TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    @Override
    public Set<TicketStatus> getAllowedTransitions(TicketStatus current) {
        Set<TicketStatus> allowed = TRANSITIONS.getOrDefault(current, EnumSet.noneOf(TicketStatus.class));
        return Collections.unmodifiableSet(EnumSet.copyOf(allowed));
    }

    @Override
    public void validateTransition(TicketStatus current, TicketStatus requested) {
        Set<TicketStatus> allowed = getAllowedTransitions(current);
        if (!allowed.contains(requested)) {
            throw new InvalidTransitionException(current, requested, allowed);
        }
    }

    @Override
    public TicketStatus applyTransition(TicketStatus current, TicketStatus requested) {
        validateTransition(current, requested);
        return requested;
    }
}
