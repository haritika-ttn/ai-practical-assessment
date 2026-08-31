package com.supporttickets.core.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.api.dto.TicketSummary;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.ValidationException;
import com.supporttickets.core.testcontext.SupportTicketsIntegrationTestBase;

/**
 * Search and status-filter integration tests.
 */
class TicketSearchIntegrationTest extends SupportTicketsIntegrationTestBase {

    /**
     * Requirement: AC-070 — Keyword search matches ticket title.
     * Setup: Ticket with unique title keyword.
     * Action: search with q=ZEBRA-UNIQUE.
     * Expected: Matching ticket returned exclusively.
     */
    @Test
    @DisplayName("AC-070 search matches ticket title")
    void ac070_searchMatchesTitle() {
        TicketDetail match = createValidatedTicket("ZEBRA-UNIQUE-123", "Other", Priority.HIGH);
        createValidatedTicket("Unrelated ticket", "Nothing special", Priority.LOW);

        List<TicketSummary> results = searchService.search(resolver, "ZEBRA-UNIQUE", null);

        assertEquals(1, results.size());
        assertEquals(match.getId(), results.get(0).getId());
    }

    /**
     * Requirement: AC-071 — Keyword search matches ticket description.
     * Setup: Unique keyword only in description.
     * Action: search with that keyword.
     * Expected: Ticket returned.
     */
    @Test
    @DisplayName("AC-071 search matches ticket description")
    void ac071_searchMatchesDescription() {
        TicketDetail match = createValidatedTicket(
                "Billing issue",
                "Contains PANDA-UNIQUE-TOKEN in description",
                Priority.MEDIUM);
        createValidatedTicket("Other", "No match here", Priority.LOW);

        List<TicketSummary> results = searchService.search(resolver, "PANDA-UNIQUE", null);

        assertEquals(1, results.size());
        assertEquals(match.getId(), results.get(0).getId());
    }

    /**
     * Requirement: AC-072 — Search is case-insensitive.
     * Setup: Ticket title with mixed case phrase.
     * Action: search with lowercase term.
     * Expected: Ticket returned.
     */
    @Test
    @DisplayName("AC-072 search is case-insensitive")
    void ac072_searchIsCaseInsensitive() {
        TicketDetail match = createValidatedTicket("Password Reset Request", "Body", Priority.HIGH);

        List<TicketSummary> results = searchService.search(resolver, "password", null);

        assertEquals(1, results.size());
        assertEquals(match.getId(), results.get(0).getId());
    }

    /**
     * Requirement: AC-073 — Search with no matches returns empty list, not error.
     * Setup: Tickets without target keyword.
     * Action: search q=XYZNOMATCH999.
     * Expected: Empty list.
     */
    @Test
    @DisplayName("AC-073 search with no matches returns empty list")
    void ac073_searchNoMatchesReturnsEmptyList() {
        createValidatedTicket("Alpha", "Beta", Priority.LOW);

        List<TicketSummary> results = searchService.search(resolver, "XYZNOMATCH999", null);

        assertTrue(results.isEmpty());
    }

    /**
     * Requirement: AC-080 — Status filter returns only matching tickets.
     * Setup: One OPEN and one CLOSED ticket.
     * Action: search with status=OPEN.
     * Expected: Only OPEN ticket returned.
     */
    @Test
    @DisplayName("AC-080 filter by status returns matching tickets only")
    void ac080_filterByStatus() {
        TicketDetail open = createValidatedTicket("Open ticket", "Body", Priority.MEDIUM);
        TicketDetail closed = advanceTicketToStatus(
                createValidatedTicket("Closed ticket", "Body", Priority.MEDIUM).getId(),
                TicketStatus.CLOSED);

        List<TicketSummary> openResults = searchService.search(resolver, null, TicketStatus.OPEN);

        assertEquals(1, openResults.size());
        assertEquals(open.getId(), openResults.get(0).getId());
        assertTrue(openResults.stream().noneMatch(ticket -> ticket.getId().equals(closed.getId())));
    }

    /**
     * Requirement: AC-081 — Each valid status filter returns only tickets in that status.
     * Setup: Tickets advanced to OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED.
     * Action: Filter by each status.
     * Expected: All returned items match requested status.
     */
    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    @DisplayName("AC-081 filter by each status value")
    void ac081_filterByEachStatus(TicketStatus status) {
        TicketDetail seeded = advanceTicketToStatus(
                createValidatedTicket("Seed " + status, "Body", Priority.LOW).getId(),
                status);

        List<TicketSummary> results = searchService.search(resolver, null, status);

        assertTrue(results.stream().anyMatch(ticket -> ticket.getId().equals(seeded.getId())));
        assertTrue(results.stream().allMatch(ticket -> ticket.getStatus() == status));
    }

    /**
     * Requirement: AC-082 — Combined keyword and status filter uses AND logic.
     * Setup: OPEN billing ticket and CLOSED billing ticket.
     * Action: search q=billing status=OPEN.
     * Expected: Only OPEN billing ticket returned.
     */
    @Test
    @DisplayName("AC-082 combined search and status filter")
    void ac082_combinedSearchAndStatusFilter() {
        TicketDetail openBilling = createValidatedTicket("billing issue open", "Body", Priority.HIGH);
        advanceTicketToStatus(
                createValidatedTicket("billing issue closed", "Body", Priority.HIGH).getId(),
                TicketStatus.CLOSED);

        List<TicketSummary> results = searchService.search(resolver, "billing", TicketStatus.OPEN);

        assertEquals(1, results.size());
        assertEquals(openBilling.getId(), results.get(0).getId());
    }

    /**
     * Requirement: AC-010 — List all tickets without filters returns every persisted ticket.
     * Setup: Two tickets created.
     * Action: search with no filters.
     * Expected: Both ticket ids present.
     */
    @Test
    @DisplayName("AC-010 list all tickets without filters")
    void ac010_listAllTickets() {
        TicketDetail one = createValidatedTicket("One", "Body", Priority.LOW);
        TicketDetail two = createValidatedTicket("Two", "Body", Priority.MEDIUM);

        List<String> ids = searchService.search(resolver, null, null).stream()
                .map(TicketSummary::getId)
                .collect(Collectors.toList());

        assertTrue(ids.contains(one.getId()));
        assertTrue(ids.contains(two.getId()));
    }

    /**
     * Requirement: AC list validation — invalid status query parameter rejected.
     * Setup: N/A.
     * Action: validateListQueryParameters with invalid status.
     * Expected: ValidationException on status field.
     */
    @Test
    @DisplayName("Invalid status filter query is rejected")
    void invalidStatusFilterRejected() {
        ValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ValidationException.class,
                () -> searchService.validateListQueryParameters(null, "NOT_A_STATUS"));
        assertTrue(ex.getFields().contains("status"));
    }
}
