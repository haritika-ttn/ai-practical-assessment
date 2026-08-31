package com.supporttickets.core.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.supporttickets.core.api.dto.CreateCommentRequest;
import com.supporttickets.core.api.dto.CreateTicketRequest;
import com.supporttickets.core.api.dto.UpdateTicketRequest;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.ValidationException;
import com.supporttickets.core.testcontext.SupportTicketsIntegrationTestBase;

/**
 * Backend validation integration tests.
 */
class TicketValidationIntegrationTest extends SupportTicketsIntegrationTestBase {

    /**
     * Requirement: AC-003 — Missing title rejected on create.
     * Setup: Create request without title.
     * Action: validateCreate.
     * Expected: ValidationException with title field.
     */
    @Test
    @DisplayName("AC-003 reject create without title")
    void ac003_rejectMissingTitle() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setDescription("No title");
        request.setPriority(Priority.LOW);
        request.setCreatedBy(AGENT1);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(request, resolver));
        assertTrue(ex.getFields().contains("title"));
    }

    /**
     * Requirement: AC-004 — Invalid priority rejected on create.
     * Setup: Create request with null priority.
     * Action: validateCreate.
     * Expected: ValidationException with priority field.
     */
    @Test
    @DisplayName("AC-004 reject create with missing priority")
    void ac004_rejectMissingPriority() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Valid title");
        request.setCreatedBy(AGENT1);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(request, resolver));
        assertTrue(ex.getFields().contains("priority"));
    }

    /**
     * Requirement: AC-005 — Unknown createdBy rejected on create.
     * Setup: Create request with unknown user path.
     * Action: validateCreate.
     * Expected: ValidationException with createdBy field.
     */
    @Test
    @DisplayName("AC-005 reject create with unknown createdBy")
    void ac005_rejectUnknownCreatedBy() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Valid title");
        request.setPriority(Priority.HIGH);
        request.setCreatedBy(UNKNOWN_USER);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(request, resolver));
        assertTrue(ex.getFields().contains("createdBy"));
    }

    /**
     * Requirement: AC-005 — Unknown assignedTo rejected on create.
     * Setup: Create request with unknown assignee.
     * Action: validateCreate.
     * Expected: ValidationException with assignedTo field.
     */
    @Test
    @DisplayName("AC-005 reject create with unknown assignedTo")
    void ac005_rejectUnknownAssignedTo() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Valid title");
        request.setPriority(Priority.HIGH);
        request.setCreatedBy(AGENT1);
        request.setAssignedTo(UNKNOWN_USER);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(request, resolver));
        assertTrue(ex.getFields().contains("assignedTo"));
    }

    /**
     * Requirement: AC-034 — status in PUT body rejected before update.
     * Setup: N/A.
     * Action: validateForbiddenUpdateFields with status present.
     * Expected: ValidationException with status field message.
     */
    @Test
    @DisplayName("AC-034 reject status field on update")
    void ac034_rejectStatusOnUpdate() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> validator.validateForbiddenUpdateFields(Set.of("status")));
        assertTrue(ex.getFields().contains("status"));
        assertTrue(ex.getFieldMessages().get("status").contains("PATCH"));
    }

    /**
     * Requirement: AC-007 — createdBy in PUT body rejected.
     * Setup: N/A.
     * Action: validateForbiddenUpdateFields with createdBy present.
     * Expected: ValidationException with createdBy field.
     */
    @Test
    @DisplayName("AC-007 reject createdBy field on update")
    void ac007_rejectCreatedByOnUpdate() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> validator.validateForbiddenUpdateFields(Set.of("createdBy")));
        assertTrue(ex.getFields().contains("createdBy"));
    }

    /**
     * Requirement: AC-057 — Unknown status enum rejected before state machine.
     * Setup: N/A.
     * Action: validateStatusValue with invalid enum.
     * Expected: ValidationException with status field.
     */
    @Test
    @DisplayName("AC-057 reject unknown status enum")
    void ac057_rejectUnknownStatusEnum() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> validator.validateStatusValue("INVALID_STATUS"));
        assertTrue(ex.getFields().contains("status"));
    }

    /**
     * Requirement: AC-057 — Missing status value rejected.
     * Setup: N/A.
     * Action: validateStatusValue with blank status.
     * Expected: ValidationException with status field.
     */
    @Test
    @DisplayName("AC-057 reject missing status value")
    void ac057_rejectMissingStatusValue() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> validator.validateStatusValue(" "));
        assertTrue(ex.getFields().contains("status"));
    }

    /**
     * Requirement: AC-057 — Valid status enum accepted by validator.
     * Setup: N/A.
     * Action: validateStatusValue in_progress.
     * Expected: Parsed TicketStatus.IN_PROGRESS returned.
     */
    @Test
    @DisplayName("AC-057 accept valid status enum")
    void ac057_acceptValidStatusEnum() {
        assertEquals(TicketStatus.IN_PROGRESS, validator.validateStatusValue("in_progress"));
    }

    /**
     * Requirement: AC-030 validation — blank title on update rejected.
     * Setup: N/A.
     * Action: validateUpdate with blank title.
     * Expected: ValidationException with title field.
     */
    @Test
    @DisplayName("AC-030 reject blank title on update")
    void rejectBlankTitleOnUpdate() {
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setTitle("   ");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> validator.validateUpdate(request, resolver));
        assertTrue(ex.getFields().contains("title"));
    }

    /**
     * Requirement: AC-061 validation — unknown comment author rejected.
     * Setup: N/A.
     * Action: validateCommentCreate with unknown createdBy.
     * Expected: ValidationException with createdBy field.
     */
    @Test
    @DisplayName("AC-061 reject comment with unknown createdBy")
    void rejectCommentWithUnknownAuthor() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setMessage("Valid message");
        request.setCreatedBy(UNKNOWN_USER);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> validator.validateCommentCreate(request, resolver));
        assertTrue(ex.getFields().contains("createdBy"));
    }
}
