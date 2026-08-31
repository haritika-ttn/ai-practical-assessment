package com.supporttickets.core.validation.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.supporttickets.core.api.dto.CreateCommentRequest;
import com.supporttickets.core.api.dto.CreateTicketRequest;
import com.supporttickets.core.api.dto.UpdateTicketRequest;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.ValidationException;
import com.supporttickets.core.service.UserLookupService;

@ExtendWith(MockitoExtension.class)
class TicketValidatorImplTest {

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private ResourceResolver resolver;

    @InjectMocks
    private TicketValidatorImpl validator;

    @Test
    void validateCreateRejectsMissingTitle() {
        CreateTicketRequest request = validCreateRequest();
        request.setTitle("   ");
        when(userLookupService.userExists(eq(resolver), any())).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(request, resolver));
        assertTrue(ex.getFields().contains("title"));
    }

    @Test
    void validateCreateRejectsUnknownUser() {
        CreateTicketRequest request = validCreateRequest();
        when(userLookupService.userExists(eq(resolver), any())).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validateCreate(request, resolver));
        assertTrue(ex.getFields().contains("createdBy"));
    }

    @Test
    void validateForbiddenUpdateFieldsRejectsStatus() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> validator.validateForbiddenUpdateFields(Set.of("status")));
        assertEquals("status", ex.getFields().get(0));
    }

    @Test
    void validateStatusValueRejectsUnknownEnum() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> validator.validateStatusValue("NOT_A_STATUS"));
        assertEquals("status", ex.getFields().get(0));
    }

    @Test
    void validateStatusValueParsesEnum() {
        TicketStatus status = validator.validateStatusValue("in_progress");
        assertEquals(TicketStatus.IN_PROGRESS, status);
    }

    @Test
    void validateCommentCreateRejectsBlankMessage() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setMessage(" ");
        request.setCreatedBy("/home/users/support/agent1");
        when(userLookupService.userExists(resolver, request.getCreatedBy())).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validateCommentCreate(request, resolver));
        assertEquals("message", ex.getFields().get(0));
    }

    @Test
    void validateUpdateAllowsUnassign() {
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setAssignedTo("");

        validator.validateUpdate(request, resolver);
    }

    private CreateTicketRequest validCreateRequest() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Valid title");
        request.setDescription("Description");
        request.setPriority(Priority.LOW);
        request.setCreatedBy("/home/users/support/agent1");
        return request;
    }
}
