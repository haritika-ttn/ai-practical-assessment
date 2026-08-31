package com.supporttickets.core.servlets.support;

import java.io.IOException;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.api.dto.UpdateTicketRequest;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.exception.ValidationException;
import com.supporttickets.core.repository.TicketRepository;
import com.supporttickets.core.util.JcrPathUtil;
import com.supporttickets.core.validation.TicketValidator;

/**
 * Handles ticket detail read and update operations.
 */
@Component(service = TicketDetailEndpoint.class)
public class TicketDetailEndpoint {

    @Reference
    private TicketRepository ticketRepository;

    @Reference
    private TicketValidator ticketValidator;

    public void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            String ticketId,
            ResourceResolver resolver) throws IOException {
        requireValidTicketId(ticketId);
        TicketDetail detail = ticketRepository.findById(resolver, ticketId);
        ApiPathParser.writeJson(response, 200, detail);
    }

    public void doPut(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            String ticketId,
            ResourceResolver resolver) throws IOException {
        requireValidTicketId(ticketId);
        if (!isJsonRequest(request, response)) {
            return;
        }

        JsonObject body = parseJsonObject(request);
        ticketValidator.validateForbiddenUpdateFields(body.keySet());
        UpdateTicketRequest updateRequest = toUpdateRequest(body);
        ticketValidator.validateUpdate(updateRequest, resolver);
        TicketDetail updated = ticketRepository.update(resolver, ticketId, updateRequest);
        ApiPathParser.writeJson(response, 200, updated);
    }

    private UpdateTicketRequest toUpdateRequest(JsonObject body) {
        UpdateTicketRequest request = new UpdateTicketRequest();
        if (body.has("title")) {
            request.setTitle(readString(body, "title"));
        }
        if (body.has("description")) {
            request.setDescription(readString(body, "description"));
        }
        if (body.has("priority")) {
            request.setPriority(parsePriority(body.get("priority")));
        }
        if (body.has("assignedTo")) {
            JsonElement assignedTo = body.get("assignedTo");
            request.setAssignedTo(assignedTo.isJsonNull() ? null : assignedTo.getAsString());
        }
        return request;
    }

    private Priority parsePriority(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            throw new ValidationException("Request validation failed.", List.of("priority"));
        }
        try {
            return Priority.fromString(element.getAsString());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Request validation failed.", List.of("priority"));
        }
    }

    private String readString(JsonObject body, String field) {
        if (!body.has(field) || body.get(field).isJsonNull()) {
            return null;
        }
        return body.get(field).getAsString();
    }

    private JsonObject parseJsonObject(SlingHttpServletRequest request) throws IOException {
        try {
            return JsonParser.parseReader(request.getReader()).getAsJsonObject();
        } catch (Exception ex) {
            throw new ValidationException("Request validation failed.", List.of("body"));
        }
    }

    private void requireValidTicketId(String ticketId) {
        if (ticketId == null || !JcrPathUtil.isValidUuid(ticketId)) {
            throw new ValidationException("Request validation failed.", List.of("ticketId"));
        }
    }

    private boolean isJsonRequest(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            ApiPathParser.writeError(response, 415, ApiErrorMapper.unsupportedMediaType());
            return false;
        }
        return true;
    }
}
