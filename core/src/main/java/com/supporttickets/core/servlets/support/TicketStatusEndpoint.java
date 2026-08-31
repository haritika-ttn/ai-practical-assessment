package com.supporttickets.core.servlets.support;

import java.io.IOException;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.ValidationException;
import com.supporttickets.core.repository.TicketRepository;
import com.supporttickets.core.util.JcrPathUtil;
import com.supporttickets.core.validation.TicketValidator;

/**
 * Handles ticket status transitions.
 */
@Component(service = TicketStatusEndpoint.class)
public class TicketStatusEndpoint {

    @Reference
    private TicketRepository ticketRepository;

    @Reference
    private TicketValidator ticketValidator;

    public void doPatch(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            String ticketId,
            ResourceResolver resolver) throws IOException {
        requireValidTicketId(ticketId);
        if (!isJsonRequest(request, response)) {
            return;
        }

        JsonObject body = parseJsonObject(request);
        if (!body.has("status") || body.get("status").isJsonNull()) {
            throw new ValidationException("Status is required", List.of("status"));
        }

        TicketStatus status = ticketValidator.validateStatusValue(body.get("status").getAsString());
        TicketDetail updated = ticketRepository.updateStatus(resolver, ticketId, status);
        ApiPathParser.writeJson(response, 200, updated);
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
