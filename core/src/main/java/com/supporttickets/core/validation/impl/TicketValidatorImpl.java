package com.supporttickets.core.validation.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.supporttickets.core.api.dto.CreateCommentRequest;
import com.supporttickets.core.api.dto.CreateTicketRequest;
import com.supporttickets.core.api.dto.UpdateTicketRequest;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.ValidationException;
import com.supporttickets.core.service.UserLookupService;
import com.supporttickets.core.util.JcrPathUtil;
import com.supporttickets.core.validation.TicketValidator;

/**
 * Field-level validation for ticket and comment writes.
 */
@Component(service = TicketValidator.class)
public class TicketValidatorImpl implements TicketValidator {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int DESCRIPTION_MAX_LENGTH = 5000;
    private static final int MESSAGE_MAX_LENGTH = 2000;

    @Reference
    private UserLookupService userLookupService;

    @Override
    public void validateCreate(CreateTicketRequest request, ResourceResolver resolver) {
        List<String> fields = new ArrayList<>();

        String title = trimToNull(request.getTitle());
        if (title == null || title.isEmpty()) {
            fields.add("title");
        } else if (title.length() > TITLE_MAX_LENGTH) {
            fields.add("title");
        }

        String description = request.getDescription();
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            fields.add("description");
        }

        if (request.getPriority() == null) {
            fields.add("priority");
        }

        String createdBy = trimToNull(request.getCreatedBy());
        if (createdBy == null) {
            fields.add("createdBy");
        } else if (!userLookupService.userExists(resolver, createdBy)) {
            fields.add("createdBy");
        }

        String assignedTo = trimToNull(request.getAssignedTo());
        if (assignedTo != null && !userLookupService.userExists(resolver, assignedTo)) {
            fields.add("assignedTo");
        }

        if (!fields.isEmpty()) {
            throw new ValidationException("Ticket create validation failed", fields);
        }
    }

    @Override
    public void validateUpdate(UpdateTicketRequest request, ResourceResolver resolver) {
        List<String> fields = new ArrayList<>();

        if (request.getTitle() != null) {
            String title = trimToNull(request.getTitle());
            if (title == null || title.isEmpty() || title.length() > TITLE_MAX_LENGTH) {
                fields.add("title");
            }
        }

        if (request.getDescription() != null && request.getDescription().length() > DESCRIPTION_MAX_LENGTH) {
            fields.add("description");
        }

        if (request.getPriority() != null) {
            try {
                Priority.valueOf(request.getPriority().name());
            } catch (IllegalArgumentException ex) {
                fields.add("priority");
            }
        }

        if (request.isAssignedToProvided()) {
            String assignedTo = trimToNull(request.getAssignedTo());
            if (assignedTo != null && !userLookupService.userExists(resolver, assignedTo)) {
                fields.add("assignedTo");
            }
        }

        if (!fields.isEmpty()) {
            throw new ValidationException("Ticket update validation failed", fields);
        }
    }

    @Override
    public void validateForbiddenUpdateFields(Set<String> presentFields) {
        Map<String, String> fieldMessages = new LinkedHashMap<>();
        if (presentFields.contains("status")) {
            fieldMessages.put(
                    "status",
                    "Status cannot be updated via this endpoint. Use PATCH /bin/support-tickets/{ticketId}/status.json.");
        }
        if (presentFields.contains("createdBy")) {
            fieldMessages.put("createdBy", "createdBy is immutable.");
        }
        if (!fieldMessages.isEmpty()) {
            throw ValidationException.withFieldMessages("Request validation failed.", fieldMessages);
        }
    }

    @Override
    public TicketStatus validateStatusValue(String status) {
        if (status == null || status.isBlank()) {
            throw new ValidationException("Status is required", List.of("status"));
        }
        try {
            return TicketStatus.fromString(status);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Unknown status value: " + status, List.of("status"));
        }
    }

    @Override
    public void validateCommentCreate(CreateCommentRequest request, ResourceResolver resolver) {
        List<String> fields = new ArrayList<>();

        String message = trimToNull(request.getMessage());
        if (message == null || message.isEmpty()) {
            fields.add("message");
        } else if (message.length() > MESSAGE_MAX_LENGTH) {
            fields.add("message");
        }

        String createdBy = trimToNull(request.getCreatedBy());
        if (createdBy == null) {
            fields.add("createdBy");
        } else if (!userLookupService.userExists(resolver, createdBy)) {
            fields.add("createdBy");
        }

        if (!fields.isEmpty()) {
            throw new ValidationException("Comment create validation failed", fields);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
