package com.supporttickets.core.servlets.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.supporttickets.core.api.error.ApiErrorResponse;
import com.supporttickets.core.api.error.ErrorCode;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.InvalidTransitionException;
import com.supporttickets.core.exception.TicketNotFoundException;
import com.supporttickets.core.exception.ValidationException;

/**
 * Maps domain exceptions to API error envelopes.
 */
public final class ApiErrorMapper {

    private static final String STATUS_ENUM_MESSAGE =
            "Status must be one of: OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED.";

    private ApiErrorMapper() {
    }

    public static ApiErrorResponse validationError(ValidationException exception) {
        ApiErrorResponse response = new ApiErrorResponse();
        response.setCode(ErrorCode.VALIDATION_ERROR.name());
        response.setMessage(resolveValidationMessage(exception));
        response.setFields(mapValidationFields(exception));
        return response;
    }

    public static ApiErrorResponse notFound(TicketNotFoundException exception) {
        ApiErrorResponse response = new ApiErrorResponse();
        response.setCode(ErrorCode.NOT_FOUND.name());
        response.setMessage("Ticket not found.");
        return response;
    }

    public static ApiErrorResponse invalidTransition(InvalidTransitionException exception) {
        ApiErrorResponse response = new ApiErrorResponse();
        response.setCode(ErrorCode.INVALID_TRANSITION.name());
        response.setMessage(exception.getMessage());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("currentStatus", exception.getCurrentStatus().name());
        details.put("requestedStatus", exception.getRequestedStatus().name());
        details.put(
                "allowedTransitions",
                exception.getAllowedTransitions().stream().map(TicketStatus::name).toList());
        response.setDetails(details);
        return response;
    }

    public static ApiErrorResponse internalError() {
        return new ApiErrorResponse(
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    public static ApiErrorResponse unsupportedMediaType() {
        return new ApiErrorResponse(
                ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type must be application/json.");
    }

    public static ApiErrorResponse methodNotAllowed() {
        return new ApiErrorResponse(
                ErrorCode.METHOD_NOT_ALLOWED,
                "HTTP method is not supported for this endpoint.");
    }

    public static ApiErrorResponse malformedTicketId() {
        ApiErrorResponse response = new ApiErrorResponse();
        response.setCode(ErrorCode.VALIDATION_ERROR.name());
        response.setMessage("Request validation failed.");
        response.addField("ticketId", "Ticket id must be a valid UUID.");
        return response;
    }

    public static ApiErrorResponse malformedJson() {
        ApiErrorResponse response = new ApiErrorResponse();
        response.setCode(ErrorCode.VALIDATION_ERROR.name());
        response.setMessage("Request validation failed.");
        response.addField("body", "Request body must be valid JSON.");
        return response;
    }

    private static String resolveValidationMessage(ValidationException exception) {
        if ("Invalid query parameter.".equals(exception.getMessage())) {
            return exception.getMessage();
        }
        return "Request validation failed.";
    }

    private static Map<String, String> mapValidationFields(ValidationException exception) {
        if (!exception.getFieldMessages().isEmpty()) {
            return new LinkedHashMap<>(exception.getFieldMessages());
        }

        Map<String, String> mapped = new LinkedHashMap<>();
        for (String field : exception.getFields()) {
            if ("status".equals(field) && "Invalid query parameter.".equals(exception.getMessage())) {
                mapped.put(field, STATUS_ENUM_MESSAGE);
            } else {
                mapped.put(field, messageForField(field));
            }
        }
        return mapped;
    }

    private static String messageForField(String field) {
        return switch (field) {
            case "title" -> "Title is required and must be between 1 and 200 characters.";
            case "description" -> "Description must not exceed 5000 characters.";
            case "priority" -> "Priority must be one of: LOW, MEDIUM, HIGH, CRITICAL.";
            case "createdBy" -> "createdBy must reference an existing seeded user.";
            case "assignedTo" -> "assignedTo must reference an existing seeded user.";
            case "status" -> "Status cannot be updated via this endpoint. Use PATCH /bin/support-tickets/{ticketId}/status.json.";
            case "message" -> "Message is required and must be between 1 and 2000 characters.";
            case "q" -> "Search keyword must not exceed 200 characters.";
            case "body" -> "Request body must be valid JSON.";
            case "ticketId" -> "Ticket id must be a valid UUID.";
            default -> "Invalid value.";
        };
    }

    public static String statusQueryMessage() {
        return STATUS_ENUM_MESSAGE;
    }
}
