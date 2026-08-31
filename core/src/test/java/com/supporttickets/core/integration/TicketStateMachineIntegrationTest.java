package com.supporttickets.core.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.InvalidTransitionException;
import com.supporttickets.core.testcontext.SupportTicketsIntegrationTestBase;

/**
 * Repository-level state machine integration tests.
 *
 * <p>Verifies that valid transitions persist to JCR and invalid transitions leave status unchanged.</p>
 */
class TicketStateMachineIntegrationTest extends SupportTicketsIntegrationTestBase {

    /**
     * Requirement: AC-040 — OPEN → IN_PROGRESS must succeed and refresh allowed transitions.
     * Setup: Ticket created in OPEN.
     * Action: updateStatus to IN_PROGRESS.
     * Expected: Persisted status IN_PROGRESS; allowed transitions are RESOLVED and CANCELLED.
     */
    @Test
    @DisplayName("AC-040 OPEN to IN_PROGRESS persists and exposes next transitions")
    void ac040_openToInProgress() {
        TicketDetail created = createValidatedTicket("AC-040", "Transition test", com.supporttickets.core.domain.Priority.HIGH);

        TicketDetail updated = repository.updateStatus(resolver, created.getId(), TicketStatus.IN_PROGRESS);

        assertEquals(TicketStatus.IN_PROGRESS, updated.getStatus());
        assertTrue(updated.getAllowedTransitions().contains(TicketStatus.RESOLVED));
        assertTrue(updated.getAllowedTransitions().contains(TicketStatus.CANCELLED));
        assertEquals(TicketStatus.IN_PROGRESS, repository.findById(resolver, created.getId()).getStatus());
    }

    /**
     * Requirement: AC-041 — IN_PROGRESS → RESOLVED must succeed.
     * Setup: Ticket advanced to IN_PROGRESS.
     * Action: updateStatus to RESOLVED.
     * Expected: Persisted status RESOLVED; allowed transition CLOSED only.
     */
    @Test
    @DisplayName("AC-041 IN_PROGRESS to RESOLVED persists terminal-next state")
    void ac041_inProgressToResolved() {
        TicketDetail ticket = requireTicketInStatus(TicketStatus.IN_PROGRESS);

        TicketDetail updated = repository.updateStatus(resolver, ticket.getId(), TicketStatus.RESOLVED);

        assertEquals(TicketStatus.RESOLVED, updated.getStatus());
        assertEquals(List.of(TicketStatus.CLOSED), updated.getAllowedTransitions());
    }

    /**
     * Requirement: AC-042 — RESOLVED → CLOSED must succeed and become terminal.
     * Setup: Ticket advanced to RESOLVED.
     * Action: updateStatus to CLOSED.
     * Expected: Persisted status CLOSED with no further transitions.
     */
    @Test
    @DisplayName("AC-042 RESOLVED to CLOSED reaches terminal state")
    void ac042_resolvedToClosed() {
        TicketDetail ticket = requireTicketInStatus(TicketStatus.RESOLVED);

        TicketDetail updated = repository.updateStatus(resolver, ticket.getId(), TicketStatus.CLOSED);

        assertEquals(TicketStatus.CLOSED, updated.getStatus());
        assertTrue(updated.getAllowedTransitions().isEmpty());
    }

    /**
     * Requirement: AC-043 — OPEN → CANCELLED must succeed.
     * Setup: Ticket in OPEN.
     * Action: updateStatus to CANCELLED.
     * Expected: Persisted status CANCELLED with no further transitions.
     */
    @Test
    @DisplayName("AC-043 OPEN to CANCELLED reaches terminal state")
    void ac043_openToCancelled() {
        TicketDetail ticket = createValidatedTicket("AC-043", "Cancel early", com.supporttickets.core.domain.Priority.LOW);

        TicketDetail updated = repository.updateStatus(resolver, ticket.getId(), TicketStatus.CANCELLED);

        assertEquals(TicketStatus.CANCELLED, updated.getStatus());
        assertTrue(updated.getAllowedTransitions().isEmpty());
    }

    /**
     * Requirement: AC-044 — IN_PROGRESS → CANCELLED must succeed.
     * Setup: Ticket in IN_PROGRESS.
     * Action: updateStatus to CANCELLED.
     * Expected: Persisted status CANCELLED.
     */
    @Test
    @DisplayName("AC-044 IN_PROGRESS to CANCELLED persists cancellation")
    void ac044_inProgressToCancelled() {
        TicketDetail ticket = requireTicketInStatus(TicketStatus.IN_PROGRESS);

        TicketDetail updated = repository.updateStatus(resolver, ticket.getId(), TicketStatus.CANCELLED);

        assertEquals(TicketStatus.CANCELLED, updated.getStatus());
        assertTrue(updated.getAllowedTransitions().isEmpty());
    }

    @ParameterizedTest(name = "AC-invalid {0} -> {1} rejected without persistence")
    @MethodSource("meaningfulInvalidTransitions")
    @DisplayName("Invalid transitions are rejected and status remains unchanged")
    void invalidTransitionsAreRejected(
            String acceptanceId,
            TicketStatus currentStatus,
            TicketStatus requestedStatus) {
        TicketDetail ticket = requireTicketInStatus(currentStatus);
        TicketDetail before = repository.findById(resolver, ticket.getId());

        InvalidTransitionException ex = assertThrows(
                InvalidTransitionException.class,
                () -> repository.updateStatus(resolver, ticket.getId(), requestedStatus));

        TicketDetail after = repository.findById(resolver, ticket.getId());
        assertEquals(currentStatus, ex.getCurrentStatus());
        assertEquals(requestedStatus, ex.getRequestedStatus());
        assertEquals(currentStatus, after.getStatus());
        assertEquals(before.getUpdatedAt(), after.getUpdatedAt());
    }

    private static Stream<Arguments> meaningfulInvalidTransitions() {
        return Stream.of(
                Arguments.of("AC-050", TicketStatus.OPEN, TicketStatus.RESOLVED),
                Arguments.of("AC-051", TicketStatus.OPEN, TicketStatus.CLOSED),
                Arguments.of("AC-052", TicketStatus.IN_PROGRESS, TicketStatus.OPEN),
                Arguments.of("AC-053", TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS),
                Arguments.of("AC-054", TicketStatus.RESOLVED, TicketStatus.CANCELLED),
                Arguments.of("AC-055", TicketStatus.CLOSED, TicketStatus.OPEN),
                Arguments.of("AC-056", TicketStatus.CANCELLED, TicketStatus.IN_PROGRESS),
                Arguments.of("AC-noop", TicketStatus.OPEN, TicketStatus.OPEN),
                Arguments.of("AC-skip", TicketStatus.IN_PROGRESS, TicketStatus.CLOSED),
                Arguments.of("AC-terminal", TicketStatus.CLOSED, TicketStatus.RESOLVED),
                Arguments.of("AC-terminal", TicketStatus.CANCELLED, TicketStatus.OPEN));
    }

    /**
     * Requirement: AC-040/invalid — successful transition must change updatedAt.
     * Setup: Ticket in OPEN.
     * Action: Valid transition to IN_PROGRESS.
     * Expected: updatedAt changes after transition.
     */
    @Test
    @DisplayName("Valid transition updates updatedAt timestamp")
    void validTransitionUpdatesTimestamp() {
        TicketDetail created = createValidatedTicket("Timestamp", "Check updatedAt", com.supporttickets.core.domain.Priority.MEDIUM);

        TicketDetail updated = repository.updateStatus(resolver, created.getId(), TicketStatus.IN_PROGRESS);

        assertNotEquals(created.getUpdatedAt(), updated.getUpdatedAt());
    }
}
