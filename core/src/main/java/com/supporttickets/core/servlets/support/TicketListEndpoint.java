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
import com.supporttickets.core.api.dto.CreateTicketRequest;
import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.ValidationException;
import com.supporttickets.core.repository.TicketRepository;
import com.supporttickets.core.service.TicketSearchService;
import com.supporttickets.core.validation.TicketValidator;

/**
 * Handles list and create ticket operations.
 */
@Component(service = TicketListEndpoint.class)
public class TicketListEndpoint {

    @Reference
    private TicketSearchService ticketSearchService;

    @Reference
    private TicketRepository ticketRepository;

    @Reference
    private TicketValidator ticketValidator;

    public void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            ResourceResolver resolver) throws IOException {
        String keyword = request.getParameter("q");
        String statusValue = request.getParameter("status");

        ticketSearchService.validateListQueryParameters(keyword, statusValue);

        TicketStatus status = null;
        if (statusValue != null && !statusValue.isBlank()) {
            status = TicketStatus.fromString(statusValue);
        }

        ApiPathParser.writeJson(
                response,
                200,
                ticketSearchService.search(resolver, keyword, status));
    }

    public void doPost(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            ResourceResolver resolver) throws IOException {
        if (!isJsonRequest(request, response)) {
            return;
        }

        CreateTicketRequest createRequest = parseCreateRequest(request);
        ticketValidator.validateCreate(createRequest, resolver);
        TicketDetail created = ticketRepository.create(resolver, createRequest);
        created.setComments(List.of());
        ApiPathParser.writeJson(response, 201, created);
    }

    private CreateTicketRequest parseCreateRequest(SlingHttpServletRequest request) throws IOException {
        JsonObject body = parseJsonObject(request);
        CreateTicketRequest createRequest = new CreateTicketRequest();
        createRequest.setTitle(readString(body, "title"));
        createRequest.setDescription(readString(body, "description"));
        createRequest.setCreatedBy(readString(body, "createdBy"));
        createRequest.setAssignedTo(readString(body, "assignedTo"));
        createRequest.setPriority(parsePriority(body.get("priority")));
        return createRequest;
    }

    private Priority parsePriority(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            return Priority.fromString(element.getAsString());
        } catch (IllegalArgumentException | UnsupportedOperationException ex) {
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
