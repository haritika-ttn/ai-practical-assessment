package com.supporttickets.core.servlets.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.jupiter.api.Test;

import com.supporttickets.core.servlets.support.ApiPathParser.ParsedRoute;
import com.supporttickets.core.servlets.support.ApiPathParser.Route;

class ApiPathParserTest {

    @Test
    void parsesListEndpoint() {
        ParsedRoute route = ApiPathParser.parse(requestFor("/bin/support-tickets.json"));
        assertEquals(Route.LIST, route.getRoute());
    }

    @Test
    void parsesUsersEndpoint() {
        ParsedRoute route = ApiPathParser.parse(requestFor("/bin/support-tickets/users.json"));
        assertEquals(Route.USERS, route.getRoute());
    }

    @Test
    void parsesDetailEndpoint() {
        ParsedRoute route = ApiPathParser.parse(
                requestFor("/bin/support-tickets/550e8400-e29b-41d4-a716-446655440000.json"));
        assertEquals(Route.DETAIL, route.getRoute());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", route.getTicketId());
    }

    @Test
    void parsesStatusEndpoint() {
        ParsedRoute route = ApiPathParser.parse(
                requestFor("/bin/support-tickets/550e8400-e29b-41d4-a716-446655440000/status.json"));
        assertEquals(Route.STATUS, route.getRoute());
    }

    @Test
    void parsesCommentsEndpoint() {
        ParsedRoute route = ApiPathParser.parse(
                requestFor("/bin/support-tickets/550e8400-e29b-41d4-a716-446655440000/comments.json"));
        assertEquals(Route.COMMENTS, route.getRoute());
    }

    @Test
    void recognizesUsersResourcePath() {
        assertTrue(ApiPathParser.isApiResourcePath("/bin/support-tickets/users"));
    }

    @Test
    void recognizesDetailResourcePath() {
        assertTrue(ApiPathParser.isApiResourcePath(
                "/bin/support-tickets/550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    void recognizesStatusResourcePath() {
        assertTrue(ApiPathParser.isApiResourcePath(
                "/bin/support-tickets/550e8400-e29b-41d4-a716-446655440000/status"));
    }

    @Test
    void listPathIsNotApiResourcePath() {
        assertFalse(ApiPathParser.isApiResourcePath("/bin/support-tickets"));
    }

    private SlingHttpServletRequest requestFor(String uri) {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn("");
        return request;
    }
}
