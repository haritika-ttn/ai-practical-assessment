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
import com.supporttickets.core.api.dto.CreateCommentRequest;
import com.supporttickets.core.exception.ValidationException;
import com.supporttickets.core.repository.TicketRepository;
import com.supporttickets.core.util.JcrPathUtil;
import com.supporttickets.core.validation.TicketValidator;

/**
 * Handles comment creation.
 */
@Component(service = TicketCommentEndpoint.class)
public class TicketCommentEndpoint {

    @Reference
    private TicketRepository ticketRepository;

    @Reference
    private TicketValidator ticketValidator;

    public void doPost(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            String ticketId,
            ResourceResolver resolver) throws IOException {
        requireValidTicketId(ticketId);
        if (!isJsonRequest(request, response)) {
            return;
        }

        CreateCommentRequest createRequest = parseCreateCommentRequest(request);
        ticketValidator.validateCommentCreate(createRequest, resolver);
        ApiPathParser.writeJson(
                response,
                201,
                ticketRepository.addComment(resolver, ticketId, createRequest));
    }

    private CreateCommentRequest parseCreateCommentRequest(SlingHttpServletRequest request) throws IOException {
        JsonObject body = parseJsonObject(request);
        CreateCommentRequest createRequest = new CreateCommentRequest();
        if (!body.has("message") || body.get("message").isJsonNull()) {
            throw new ValidationException("Request validation failed.", List.of("message"));
        }
        createRequest.setMessage(body.get("message").getAsString());
        if (!body.has("createdBy") || body.get("createdBy").isJsonNull()) {
            throw new ValidationException("Request validation failed.", List.of("createdBy"));
        }
        createRequest.setCreatedBy(body.get("createdBy").getAsString());
        return createRequest;
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
