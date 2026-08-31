package com.supporttickets.core.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.service.ResourceResolverProvider;
import com.supporttickets.core.servlets.SupportTicketsApiServlet;
import com.supporttickets.core.util.JsonUtil;
import com.supporttickets.core.servlets.support.TicketCommentEndpoint;
import com.supporttickets.core.servlets.support.TicketDetailEndpoint;
import com.supporttickets.core.servlets.support.TicketListEndpoint;
import com.supporttickets.core.servlets.support.TicketStatusEndpoint;
import com.supporttickets.core.servlets.support.UserListEndpoint;
import com.supporttickets.core.testcontext.SupportTicketsIntegrationTestBase;

import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * HTTP servlet integration tests for API error handling and status codes.
 */
@ExtendWith(AemContextExtension.class)
class SupportTicketsApiIntegrationTest extends SupportTicketsIntegrationTestBase {

    private SupportTicketsApiServlet servlet;

    @BeforeEach
    void setUpServlet() {
        TicketListEndpoint listEndpoint = new TicketListEndpoint();
        inject(listEndpoint, "ticketSearchService", searchService);
        inject(listEndpoint, "ticketRepository", repository);
        inject(listEndpoint, "ticketValidator", validator);

        TicketDetailEndpoint detailEndpoint = new TicketDetailEndpoint();
        inject(detailEndpoint, "ticketRepository", repository);
        inject(detailEndpoint, "ticketValidator", validator);

        TicketStatusEndpoint statusEndpoint = new TicketStatusEndpoint();
        inject(statusEndpoint, "ticketRepository", repository);
        inject(statusEndpoint, "ticketValidator", validator);

        TicketCommentEndpoint commentEndpoint = new TicketCommentEndpoint();
        inject(commentEndpoint, "ticketRepository", repository);
        inject(commentEndpoint, "ticketValidator", validator);

        UserListEndpoint userListEndpoint = new UserListEndpoint();
        inject(userListEndpoint, "userLookupService", userLookupService);

        ResourceResolver serviceResolver = spy(resolver);
        doNothing().when(serviceResolver).close();

        ResourceResolverProvider resolverProvider = Mockito.mock(ResourceResolverProvider.class);
        when(resolverProvider.getServiceResourceResolver()).thenReturn(serviceResolver);

        servlet = new SupportTicketsApiServlet();
        inject(servlet, "resourceResolverProvider", resolverProvider);
        inject(servlet, "ticketListEndpoint", listEndpoint);
        inject(servlet, "ticketDetailEndpoint", detailEndpoint);
        inject(servlet, "ticketStatusEndpoint", statusEndpoint);
        inject(servlet, "ticketCommentEndpoint", commentEndpoint);
        inject(servlet, "userListEndpoint", userListEndpoint);
    }

    @Test
    @DisplayName("TicketDetail serializes for API responses")
    void ticketDetailSerializesForApiResponses() {
        TicketDetail ticket = createValidatedTicket("Serialize", "Body", Priority.HIGH);
        String json = JsonUtil.toJson(ticket);
        assertTrue(json.contains("\"status\":\"OPEN\""));
    }

    /**
     * Requirement: AC-002 — Valid POST create returns 201 with OPEN status.
     * Setup: Seeded users mocked as existing.
     * Action: POST /bin/support-tickets.json with valid body.
     * Expected: HTTP 201 and response status OPEN.
     */
    @Test
    @DisplayName("AC-002 POST create returns 201 Created")
    void ac002_postCreateReturns201() throws Exception {
        MockSlingHttpServletResponse response = invoke(
                jsonRequest(
                        "POST",
                        "/bin/support-tickets.json",
                        "{\"title\":\"API create\",\"description\":\"Body\",\"priority\":\"HIGH\",\"createdBy\":\""
                                + AGENT1 + "\"}",
                        null));

        assertEquals(201, response.getStatus(), response.getOutputAsString());
        assertEquals("OPEN", parseBody(response).get("status").getAsString());
    }

    /**
     * Requirement: AC-003 — Invalid create returns 400 VALIDATION_ERROR.
     * Setup: POST body missing title.
     * Action: POST /bin/support-tickets.json.
     * Expected: HTTP 400 with VALIDATION_ERROR and title field error.
     */
    @Test
    @DisplayName("AC-003 POST create validation error returns 400")
    void ac003_postCreateValidationErrorReturns400() throws Exception {
        MockSlingHttpServletResponse response = invoke(
                jsonRequest(
                        "POST",
                        "/bin/support-tickets.json",
                        "{\"description\":\"No title\",\"priority\":\"LOW\",\"createdBy\":\"" + AGENT1 + "\"}",
                        null));

        JsonObject body = parseBody(response);
        assertEquals(400, response.getStatus());
        assertEquals("VALIDATION_ERROR", body.get("code").getAsString());
        assertTrue(body.getAsJsonObject("fields").has("title"));
    }

    /**
     * Requirement: AC-034 — PUT with status field returns 400 and does not transition.
     * Setup: Ticket in OPEN.
     * Action: PUT with status IN_PROGRESS in body.
     * Expected: HTTP 400; persisted status remains OPEN.
     */
    @Test
    @DisplayName("AC-034 PUT with status field returns 400")
    void ac034_putWithStatusReturns400() throws Exception {
        TicketDetail ticket = createValidatedTicket("Status PUT", "Body", Priority.MEDIUM);

        MockSlingHttpServletResponse response = invoke(
                jsonRequest(
                        "PUT",
                        "/bin/support-tickets/" + ticket.getId() + ".json",
                        "{\"status\":\"IN_PROGRESS\",\"title\":\"Status PUT\"}",
                        null));

        JsonObject body = parseBody(response);
        assertEquals(400, response.getStatus());
        assertEquals("VALIDATION_ERROR", body.get("code").getAsString());
        assertTrue(body.getAsJsonObject("fields").has("status"));
        assertEquals(TicketStatus.OPEN, repository.findById(resolver, ticket.getId()).getStatus());
    }

    /**
     * Requirement: AC-057 — PATCH with unknown status returns 400.
     * Setup: Ticket in OPEN.
     * Action: PATCH status with INVALID_STATUS.
     * Expected: HTTP 400 VALIDATION_ERROR; status unchanged.
     */
    @Test
    @DisplayName("AC-057 PATCH unknown status returns 400")
    void ac057_patchUnknownStatusReturns400() throws Exception {
        TicketDetail ticket = createValidatedTicket("Enum test", "Body", Priority.LOW);

        MockSlingHttpServletResponse response = invoke(
                jsonRequest(
                        "PATCH",
                        "/bin/support-tickets/" + ticket.getId() + "/status.json",
                        "{\"status\":\"INVALID_STATUS\"}",
                        null));

        JsonObject body = parseBody(response);
        assertEquals(400, response.getStatus());
        assertEquals("VALIDATION_ERROR", body.get("code").getAsString());
        assertEquals(TicketStatus.OPEN, repository.findById(resolver, ticket.getId()).getStatus());
    }

    /**
     * Requirement: AC-050 — Invalid transition returns 409 INVALID_TRANSITION.
     * Setup: Ticket in OPEN.
     * Action: PATCH status CLOSED.
     * Expected: HTTP 409 with transition details; status remains OPEN.
     */
    @Test
    @DisplayName("AC-050 invalid transition returns 409")
    void ac050_invalidTransitionReturns409() throws Exception {
        TicketDetail ticket = createValidatedTicket("Transition API", "Body", Priority.HIGH);

        MockSlingHttpServletResponse response = invoke(
                jsonRequest(
                        "PATCH",
                        "/bin/support-tickets/" + ticket.getId() + "/status.json",
                        "{\"status\":\"CLOSED\"}",
                        null));

        JsonObject body = parseBody(response);
        assertEquals(409, response.getStatus());
        assertEquals("INVALID_TRANSITION", body.get("code").getAsString());
        assertEquals("OPEN", body.getAsJsonObject("details").get("currentStatus").getAsString());
        assertEquals(TicketStatus.OPEN, repository.findById(resolver, ticket.getId()).getStatus());
    }

    /**
     * Requirement: AC-040 — Valid transition returns 200 with updated status.
     * Setup: Ticket in OPEN.
     * Action: PATCH status IN_PROGRESS.
     * Expected: HTTP 200 and response status IN_PROGRESS.
     */
    @Test
    @DisplayName("AC-040 valid transition returns 200")
    void ac040_validTransitionReturns200() throws Exception {
        TicketDetail ticket = createValidatedTicket("Valid transition", "Body", Priority.MEDIUM);

        MockSlingHttpServletResponse response = invoke(
                jsonRequest(
                        "PATCH",
                        "/bin/support-tickets/" + ticket.getId() + "/status.json",
                        "{\"status\":\"IN_PROGRESS\"}",
                        null));

        assertEquals(200, response.getStatus());
        assertEquals("IN_PROGRESS", parseBody(response).get("status").getAsString());
    }

    /**
     * Requirement: AC-022 — GET missing ticket returns 404 NOT_FOUND.
     * Setup: Unknown ticket id.
     * Action: GET detail endpoint.
     * Expected: HTTP 404 NOT_FOUND.
     */
    @Test
    @DisplayName("AC-022 GET missing ticket returns 404")
    void ac022_getMissingTicketReturns404() throws Exception {
        MockSlingHttpServletResponse response = invoke(
                jsonRequest(
                        "GET",
                        "/bin/support-tickets/550e8400-e29b-41d4-a716-446655440000.json",
                        null,
                        null));

        JsonObject body = parseBody(response);
        assertEquals(404, response.getStatus());
        assertEquals("NOT_FOUND", body.get("code").getAsString());
    }

    /**
     * Requirement: AC-073 — GET list with no matches returns 200 and empty array.
     * Setup: Ticket exists but search term does not match.
     * Action: GET list with q=XYZNOMATCH999.
     * Expected: HTTP 200 and [].
     */
    @Test
    @DisplayName("AC-073 GET search no matches returns empty array")
    void ac073_getSearchNoMatchesReturnsEmptyArray() throws Exception {
        createValidatedTicket("Existing", "Body", Priority.LOW);

        MockSlingHttpServletResponse response = invoke(
                jsonRequest("GET", "/bin/support-tickets.json", null, "q=XYZNOMATCH999"));

        assertEquals(200, response.getStatus());
        assertEquals(0, JsonParser.parseString(response.getOutputAsString()).getAsJsonArray().size());
    }

    /**
     * Requirement: AC-120 — GET users returns seeded user list.
     * Setup: User lookup service returns seeded users.
     * Action: GET /bin/support-tickets/users.json.
     * Expected: HTTP 200 and JSON array response.
     */
    @Test
    @DisplayName("AC-120 GET users returns 200")
    void ac120_getUsersReturns200() throws Exception {
        when(userLookupService.listSeededUsers(any())).thenReturn(List.of());

        MockSlingHttpServletResponse response = invoke(
                jsonRequest("GET", "/bin/support-tickets/users.json", null, null));

        assertEquals(200, response.getStatus(), response.getOutputAsString());
        assertTrue(JsonParser.parseString(response.getOutputAsString()).isJsonArray());
    }

    /**
     * Requirement: AC-061 — POST comment returns 201 with comment payload.
     * Setup: Existing ticket.
     * Action: POST comment.
     * Expected: HTTP 201 and comment id present.
     */
    @Test
    @DisplayName("AC-061 POST comment returns 201")
    void ac061_postCommentReturns201() throws Exception {
        TicketDetail ticket = createValidatedTicket("Comment API", "Body", Priority.MEDIUM);

        MockSlingHttpServletResponse response = invoke(
                jsonRequest(
                        "POST",
                        "/bin/support-tickets/" + ticket.getId() + "/comments.json",
                        "{\"message\":\"API comment\",\"createdBy\":\"" + AGENT1 + "\"}",
                        null));

        assertEquals(201, response.getStatus());
        assertTrue(parseBody(response).has("id"));
    }

    private SlingHttpServletRequest jsonRequest(String method, String uri, String body, String queryString)
            throws Exception {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn("");
        when(request.getContentType()).thenReturn(body == null ? null : "application/json");
        if (body != null) {
            when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
        }
        if (queryString != null) {
            for (String pair : queryString.split("&")) {
                String[] parts = pair.split("=", 2);
                when(request.getParameter(parts[0])).thenReturn(parts.length > 1 ? parts[1] : "");
            }
        }
        return request;
    }

    private MockSlingHttpServletResponse invoke(SlingHttpServletRequest request) throws Exception {
        MockSlingHttpServletResponse response = context.response();
        servlet.service(request, response);
        return response;
    }

    private JsonObject parseBody(MockSlingHttpServletResponse response) {
        return JsonParser.parseString(response.getOutputAsString()).getAsJsonObject();
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
