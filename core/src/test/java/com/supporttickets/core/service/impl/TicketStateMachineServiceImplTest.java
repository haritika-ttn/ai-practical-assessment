package com.supporttickets.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.InvalidTransitionException;

/**
 * Complete transition matrix for {@link TicketStateMachineServiceImpl}.
 */
class TicketStateMachineServiceImplTest {

    private TicketStateMachineServiceImpl stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new TicketStateMachineServiceImpl();
    }

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @MethodSource("validTransitions")
    void applyTransition_validTransitions(TicketStatus current, TicketStatus requested) {
        assertEquals(requested, stateMachine.applyTransition(current, requested));
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @MethodSource("invalidTransitions")
    void applyTransition_invalidTransitions_throwInvalidTransitionException(
            TicketStatus current, TicketStatus requested) {
        InvalidTransitionException ex = assertThrows(
                InvalidTransitionException.class,
                () -> stateMachine.applyTransition(current, requested));

        assertEquals(current, ex.getCurrentStatus());
        assertEquals(requested, ex.getRequestedStatus());
        assertEquals(
                stateMachine.getAllowedTransitions(current).stream().sorted().toList(),
                ex.getAllowedTransitions());
    }

    @ParameterizedTest
    @MethodSource("allowedTransitionsBySource")
    void getAllowedTransitions_returnsExpectedSet(TicketStatus current, Set<TicketStatus> expected) {
        assertEquals(expected, stateMachine.getAllowedTransitions(current));
    }

    @Test
    void getAllowedTransitions_returnsUnmodifiableCopy() {
        Set<TicketStatus> allowed = stateMachine.getAllowedTransitions(TicketStatus.OPEN);
        assertThrows(UnsupportedOperationException.class, () -> allowed.add(TicketStatus.CLOSED));
    }

    private static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.OPEN, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CLOSED));
    }

    private static Stream<Arguments> invalidTransitions() {
        Set<String> validKeys = Set.of(
                key(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                key(TicketStatus.OPEN, TicketStatus.CANCELLED),
                key(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
                key(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
                key(TicketStatus.RESOLVED, TicketStatus.CLOSED));

        return Stream.of(TicketStatus.values())
                .flatMap(current -> Stream.of(TicketStatus.values())
                        .filter(requested -> !validKeys.contains(key(current, requested)))
                        .map(requested -> Arguments.of(current, requested)));
    }

    private static Stream<Arguments> allowedTransitionsBySource() {
        return Stream.of(
                Arguments.of(TicketStatus.OPEN, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED)),
                Arguments.of(TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED)),
                Arguments.of(TicketStatus.RESOLVED, EnumSet.of(TicketStatus.CLOSED)),
                Arguments.of(TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class)),
                Arguments.of(TicketStatus.CANCELLED, EnumSet.noneOf(TicketStatus.class)));
    }

    private static String key(TicketStatus current, TicketStatus requested) {
        return current.name() + "->" + requested.name();
    }
}
