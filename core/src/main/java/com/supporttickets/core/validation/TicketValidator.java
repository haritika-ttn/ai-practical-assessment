package com.supporttickets.core.validation;

import java.util.Set;

import org.apache.sling.api.resource.ResourceResolver;

import com.supporttickets.core.api.dto.CreateCommentRequest;
import com.supporttickets.core.api.dto.CreateTicketRequest;
import com.supporttickets.core.api.dto.UpdateTicketRequest;
import com.supporttickets.core.domain.TicketStatus;

/**
 * Validates ticket and comment input before persistence.
 */
public interface TicketValidator {

    void validateCreate(CreateTicketRequest request, ResourceResolver resolver);

    void validateUpdate(UpdateTicketRequest request, ResourceResolver resolver);

    void validateForbiddenUpdateFields(Set<String> presentFields);

    TicketStatus validateStatusValue(String status);

    void validateCommentCreate(CreateCommentRequest request, ResourceResolver resolver);
}
