package com.supporttickets.core.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.supporttickets.core.api.dto.CreateCommentRequest;
import com.supporttickets.core.api.dto.Comment;
import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.exception.TicketNotFoundException;
import com.supporttickets.core.exception.ValidationException;
import com.supporttickets.core.testcontext.SupportTicketsIntegrationTestBase;

/**
 * Comment persistence integration tests.
 */
class TicketCommentIntegrationTest extends SupportTicketsIntegrationTestBase {

    /**
     * Requirement: AC-061 — Add comment via validated repository call persists comment.
     * Setup: Existing ticket.
     * Action: Validate and add comment.
     * Expected: Comment returned with id, message, createdBy, createdAt, and parent ticketId.
     */
    @Test
    @DisplayName("AC-061 add comment persists comment payload")
    void ac061_addCommentPersistsComment() {
        TicketDetail ticket = createValidatedTicket("Comment parent", "Body", Priority.MEDIUM);
        CreateCommentRequest request = new CreateCommentRequest();
        request.setMessage("Customer confirmed issue is resolved.");
        request.setCreatedBy(AGENT1);

        validator.validateCommentCreate(request, resolver);
        Comment created = repository.addComment(resolver, ticket.getId(), request);

        assertNotNull(created.getId());
        assertEquals(ticket.getId(), created.getTicketId());
        assertEquals("Customer confirmed issue is resolved.", created.getMessage());
        assertEquals(AGENT1, created.getCreatedBy());
        assertNotNull(created.getCreatedAt());
    }

    /**
     * Requirement: AC-061/AC-020 — Comments appear on ticket detail in createdAt order.
     * Setup: Ticket with two comments.
     * Action: findById.
     * Expected: Two comments sorted chronologically.
     */
    @Test
    @DisplayName("AC-061 comments are returned on ticket detail")
    void commentsAppearOnTicketDetail() {
        TicketDetail ticket = createValidatedTicket("Thread", "Body", Priority.LOW);

        CreateCommentRequest first = new CreateCommentRequest();
        first.setMessage("First");
        first.setCreatedBy(AGENT1);
        validator.validateCommentCreate(first, resolver);
        repository.addComment(resolver, ticket.getId(), first);

        CreateCommentRequest second = new CreateCommentRequest();
        second.setMessage("Second");
        second.setCreatedBy(AGENT2);
        validator.validateCommentCreate(second, resolver);
        repository.addComment(resolver, ticket.getId(), second);

        TicketDetail loaded = repository.findById(resolver, ticket.getId());
        assertEquals(2, loaded.getComments().size());
        assertTrue(loaded.getComments().get(0).getCreatedAt()
                .compareTo(loaded.getComments().get(1).getCreatedAt()) <= 0);
    }

    /**
     * Requirement: AC-063 — Adding comment refreshes parent ticket updatedAt.
     * Setup: Ticket with known updatedAt.
     * Action: Add comment and reload ticket.
     * Expected: Parent updatedAt is greater than or equal to comment createdAt.
     */
    @Test
    @DisplayName("AC-063 add comment refreshes parent updatedAt")
    void ac063_addCommentRefreshesParentUpdatedAt() {
        TicketDetail ticket = createValidatedTicket("UpdatedAt", "Body", Priority.HIGH);
        String beforeUpdatedAt = ticket.getUpdatedAt();

        CreateCommentRequest request = new CreateCommentRequest();
        request.setMessage("Bump updatedAt");
        request.setCreatedBy(AGENT1);
        validator.validateCommentCreate(request, resolver);
        Comment comment = repository.addComment(resolver, ticket.getId(), request);

        TicketDetail reloaded = repository.findById(resolver, ticket.getId());
        assertTrue(reloaded.getUpdatedAt().compareTo(beforeUpdatedAt) >= 0);
        assertTrue(reloaded.getUpdatedAt().compareTo(comment.getCreatedAt()) >= 0);
    }

    /**
     * Requirement: AC-062 — Comment on missing ticket must not create orphan comment.
     * Setup: Non-existent ticket id.
     * Action: addComment.
     * Expected: TicketNotFoundException.
     */
    @Test
    @DisplayName("AC-062 comment on missing ticket fails with not found")
    void ac062_commentOnMissingTicketFails() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setMessage("Orphan");
        request.setCreatedBy(AGENT1);
        validator.validateCommentCreate(request, resolver);

        assertThrows(
                TicketNotFoundException.class,
                () -> repository.addComment(
                        resolver,
                        "550e8400-e29b-41d4-a716-446655440000",
                        request));
    }

    /**
     * Requirement: AC-061 validation — blank comment message rejected before persistence.
     * Setup: Existing ticket.
     * Action: validateCommentCreate with blank message.
     * Expected: ValidationException on field message.
     */
    @Test
    @DisplayName("AC-061 blank comment message is rejected")
    void blankCommentMessageRejected() {
        createValidatedTicket("Validation", "Body", Priority.LOW);
        CreateCommentRequest request = new CreateCommentRequest();
        request.setMessage("   ");
        request.setCreatedBy(AGENT1);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> validator.validateCommentCreate(request, resolver));
        assertTrue(ex.getFields().contains("message"));
    }
}
