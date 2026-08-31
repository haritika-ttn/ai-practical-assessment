package com.supporttickets.core.servlets.support;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import com.supporttickets.core.api.error.ApiErrorResponse;
import com.supporttickets.core.util.JsonUtil;

/**
 * Parses API paths under {@code /bin/support-tickets}.
 */
public final class ApiPathParser {

    public static final String API_ROOT = "/bin/support-tickets";

    private static final Pattern LIST_PATTERN = Pattern.compile("^/bin/support-tickets\\.json$");
    private static final Pattern USERS_PATTERN = Pattern.compile("^/bin/support-tickets/users\\.json$");
    private static final Pattern DETAIL_PATTERN =
            Pattern.compile("^/bin/support-tickets/([0-9a-fA-F-]{36})\\.json$");
    private static final Pattern STATUS_PATTERN =
            Pattern.compile("^/bin/support-tickets/([0-9a-fA-F-]{36})/status\\.json$");
    private static final Pattern COMMENTS_PATTERN =
            Pattern.compile("^/bin/support-tickets/([0-9a-fA-F-]{36})/comments\\.json$");
    private static final Pattern API_RESOURCE_PATH_PATTERN = Pattern.compile(
            "^/bin/support-tickets/(users|[0-9a-fA-F-]{36}(/status|/comments)?)$");

    private ApiPathParser() {
    }

    /**
     * Returns whether {@code path} is a nested API resource path resolved by
     * {@link com.supporttickets.core.resource.SupportTicketsApiResourceProvider}.
     *
     * <p>The list endpoint ({@code /bin/support-tickets}) is served by path servlet registration.</p>
     */
    public static boolean isApiResourcePath(String path) {
        return path != null && API_RESOURCE_PATH_PATTERN.matcher(path).matches();
    }

    public enum Route {
        LIST,
        USERS,
        DETAIL,
        STATUS,
        COMMENTS,
        UNKNOWN
    }

    public static ParsedRoute parse(SlingHttpServletRequest request) {
        String requestUri = stripContextPath(request);
        Matcher listMatcher = LIST_PATTERN.matcher(requestUri);
        if (listMatcher.matches()) {
            return new ParsedRoute(Route.LIST, null);
        }

        Matcher usersMatcher = USERS_PATTERN.matcher(requestUri);
        if (usersMatcher.matches()) {
            return new ParsedRoute(Route.USERS, null);
        }

        Matcher statusMatcher = STATUS_PATTERN.matcher(requestUri);
        if (statusMatcher.matches()) {
            return new ParsedRoute(Route.STATUS, statusMatcher.group(1));
        }

        Matcher commentsMatcher = COMMENTS_PATTERN.matcher(requestUri);
        if (commentsMatcher.matches()) {
            return new ParsedRoute(Route.COMMENTS, commentsMatcher.group(1));
        }

        Matcher detailMatcher = DETAIL_PATTERN.matcher(requestUri);
        if (detailMatcher.matches()) {
            return new ParsedRoute(Route.DETAIL, detailMatcher.group(1));
        }

        return new ParsedRoute(Route.UNKNOWN, null);
    }

    public static void writeJson(SlingHttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getWriter().write(JsonUtil.toJson(body));
    }

    public static void writeError(SlingHttpServletResponse response, int status, ApiErrorResponse error)
            throws IOException {
        writeJson(response, status, error);
    }

    private static String stripContextPath(SlingHttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    public static final class ParsedRoute {
        private final Route route;
        private final String ticketId;

        public ParsedRoute(Route route, String ticketId) {
            this.route = route;
            this.ticketId = ticketId;
        }

        public Route getRoute() {
            return route;
        }

        public String getTicketId() {
            return ticketId;
        }
    }
}
