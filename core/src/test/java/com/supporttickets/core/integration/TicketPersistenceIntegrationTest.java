package com.supporttickets.core.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.supporttickets.core.api.dto.CreateCommentRequest;
import com.supporttickets.core.api.dto.CreateTicketRequest;
import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.api.dto.UpdateTicketRequest;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.TicketNotFoundException;
import com.supporttickets.core.testcontext.SupportTicketsIntegrationTestBase;

/**
 * Ticket create, retrieve, and update integration tests.
 */
class TicketPersistenceIntegrationTest extends SupportTicketsIntegrationTestBase {

    /**
     * Requirement: AC-002 — Valid create persists ticket with OPEN status and generated id.
     * Setup: Empty tickets folder; valid create payload.
     * Action: Validate and create ticket.
     * Expected: Ticket returned with id, OPEN status, timestamps, and submitted fields.
     */
    @Test
    @DisplayName("AC-002 create ticket persists OPEN ticket with metadata")
    void ac002_createTicketPersistsOpenTicket() {
        TicketDetail created = createValidatedTicket(
                "Login issue",
                "Cannot reset password",
                Priority.HIGH,
                AGENT1,
                AGENT2);

        assertNotNull(created.getId());
        assertEquals("Login issue", created.getTitle());
        assertEquals("Cannot reset password", created.getDescription());
        assertEquals(Priority.HIGH, created.getPriority());
        assertEquals(TicketStatus.OPEN, created.getStatus());
        assertEquals(AGENT1, created.getCreatedBy());
        assertEquals(AGENT2, created.getAssignedTo());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
    }

    /**
     * Requirement: AC-006 — Client-supplied status on create must be ignored; repository forces OPEN.
     * Setup: Valid create request (status not settable on DTO; repository enforces OPEN).
     * Action: Create ticket.
     * Expected: Persisted status is always OPEN.
     */
    @Test
    @DisplayName("AC-006 create always forces OPEN status")
    void ac006_createAlwaysForcesOpenStatus() {
        TicketDetail created = createValidatedTicket("Forced open", "Status ignored", Priority.MEDIUM);
        assertEquals(TicketStatus.OPEN, repository.findById(resolver, created.getId()).getStatus());
    }

    /**
     * Requirement: AC-020 — Retrieve ticket detail by id.
     * Setup: Existing ticket with comments-eligible structure.
     * Action: findById.
     * Expected: Detail includes fields, allowedTransitions, and empty comments list.
     */
    @Test
    @DisplayName("AC-020 retrieve ticket detail by id")
    void ac020_retrieveTicketDetail() {
        TicketDetail created = createValidatedTicket("Detail ticket", "Detail body", Priority.LOW);

        TicketDetail loaded = repository.findById(resolver, created.getId());

        assertEquals(created.getId(), loaded.getId());
        assertEquals("Detail ticket", loaded.getTitle());
        assertEquals(TicketStatus.OPEN, loaded.getStatus());
        assertTrue(loaded.getAllowedTransitions().contains(TicketStatus.IN_PROGRESS));
        assertNotNull(loaded.getComments());
    }

    /**
     * Requirement: AC-022 — Missing ticket returns not found.
     * Setup: Known UUID with no node.
     * Action: findById.
     * Expected: TicketNotFoundException.
     */
    @Test
    @DisplayName("AC-022 missing ticket throws not found")
    void ac022_missingTicketThrowsNotFound() {
        assertThrows(
                TicketNotFoundException.class,
                () -> repository.findById(resolver, "550e8400-e29b-41d4-a716-446655440000"));
    }

    /**
     * Requirement: AC-030 — Update title persists new value and refreshes updatedAt.
     * Setup: Existing ticket.
     * Action: PUT-equivalent update via repository.
     * Expected: Title changed; status unchanged.
     */
    @Test
    @DisplayName("AC-030 update title persists change")
    void ac030_updateTitle() {
        TicketDetail created = createValidatedTicket("Old title", "Body", Priority.MEDIUM);
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setTitle("New title");

        validator.validateUpdate(update, resolver);
        TicketDetail updated = repository.update(resolver, created.getId(), update);

        assertEquals("New title", updated.getTitle());
        assertEquals(TicketStatus.OPEN, updated.getStatus());
        assertEquals("New title", repository.findById(resolver, created.getId()).getTitle());
    }

    /**
     * Requirement: AC-031 — Update description persists.
     * Setup: Existing ticket.
     * Action: Update description.
     * Expected: Description changed in persisted ticket.
     */
    @Test
    @DisplayName("AC-031 update description persists change")
    void ac031_updateDescription() {
        TicketDetail created = createValidatedTicket("Title", "Old description", Priority.LOW);
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setDescription("Updated description");

        validator.validateUpdate(update, resolver);
        TicketDetail updated = repository.update(resolver, created.getId(), update);

        assertEquals("Updated description", updated.getDescription());
    }

    /**
     * Requirement: AC-032 — Update priority persists.
     * Setup: Existing ticket.
     * Action: Update priority to CRITICAL.
     * Expected: Priority persisted.
     */
    @Test
    @DisplayName("AC-032 update priority persists change")
    void ac032_updatePriority() {
        TicketDetail created = createValidatedTicket("Priority ticket", "Body", Priority.LOW);
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setPriority(Priority.CRITICAL);

        validator.validateUpdate(update, resolver);
        TicketDetail updated = repository.update(resolver, created.getId(), update);

        assertEquals(Priority.CRITICAL, updated.getPriority());
    }

    /**
     * Requirement: AC-033 — Reassign ticket by updating assignee.
     * Setup: Ticket assigned to agent1.
     * Action: Update assignedTo to agent2.
     * Expected: Assignee path updated.
     */
    @Test
    @DisplayName("AC-033 reassign ticket updates assignee")
    void ac033_reassignTicket() {
        TicketDetail created = createValidatedTicket(
                "Reassign", "Body", Priority.MEDIUM, AGENT1, AGENT1);
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setAssignedTo(AGENT2);

        validator.validateUpdate(update, resolver);
        TicketDetail updated = repository.update(resolver, created.getId(), update);

        assertEquals(AGENT2, updated.getAssignedTo());
    }

    /**
     * Requirement: AC-033 — Unassign by clearing assignee.
     * Setup: Ticket with assignee.
     * Action: Update assignedTo to empty string.
     * Expected: Assignee becomes null in API representation.
     */
    @Test
    @DisplayName("AC-033 unassign clears assignee")
    void ac033_unassignTicket() {
        TicketDetail created = createValidatedTicket(
                "Unassign", "Body", Priority.MEDIUM, AGENT1, AGENT2);
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setAssignedTo("");

        validator.validateUpdate(update, resolver);
        TicketDetail updated = repository.update(resolver, created.getId(), update);

        assertNull(updated.getAssignedTo());
    }

    /**
     * Requirement: AC-030 — General update must not change status.
     * Setup: Ticket in OPEN.
     * Action: Update title only.
     * Expected: Status remains OPEN.
     */
    @Test
    @DisplayName("AC-030 update does not change ticket status")
    void updateDoesNotChangeStatus() {
        TicketDetail created = createValidatedTicket("Status guard", "Body", Priority.HIGH);
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setTitle("Updated");

        validator.validateUpdate(update, resolver);
        TicketDetail updated = repository.update(resolver, created.getId(), update);

        assertEquals(TicketStatus.OPEN, updated.getStatus());
    }

    /**
     * Requirement: AC-011 — Newly created ticket is retrievable immediately.
     * Setup: Empty repository then create.
     * Action: Create then findById.
     * Expected: Same ticket id returned.
     */
    @Test
    @DisplayName("AC-011 newly created ticket is retrievable")
    void ac011_createdTicketIsRetrievable() {
        TicketDetail created = createValidatedTicket("List visibility", "Body", Priority.MEDIUM);
        assertEquals(created.getId(), repository.findById(resolver, created.getId()).getId());
    }

    /**
     * Requirement: AC-003 — Invalid create must not increase ticket count.
     * Setup: Baseline ticket count.
     * Action: Attempt validateCreate with missing title.
     * Expected: Validation fails before persistence; count unchanged.
     */
    @Test
    @DisplayName("AC-003 invalid create does not persist ticket")
    void ac003_invalidCreateDoesNotPersist() {
        int before = countTicketNodes();
        CreateTicketRequest request = new CreateTicketRequest();
        request.setDescription("No title");
        request.setPriority(Priority.LOW);
        request.setCreatedBy(AGENT1);

        assertThrows(com.supporttickets.core.exception.ValidationException.class,
                () -> validator.validateCreate(request, resolver));
        assertEquals(before, countTicketNodes());
    }
}
