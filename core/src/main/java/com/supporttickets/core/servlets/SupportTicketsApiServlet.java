package com.supporttickets.core.servlets;

import java.io.IOException;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.supporttickets.core.exception.InternalServiceException;
import com.supporttickets.core.exception.InvalidTransitionException;
import com.supporttickets.core.exception.TicketNotFoundException;
import com.supporttickets.core.exception.ValidationException;
import com.supporttickets.core.domain.TicketConstants;
import com.supporttickets.core.service.ResourceResolverProvider;
import com.supporttickets.core.servlets.support.ApiErrorMapper;
import com.supporttickets.core.servlets.support.ApiPathParser;
import com.supporttickets.core.servlets.support.TicketCommentEndpoint;
import com.supporttickets.core.servlets.support.TicketDetailEndpoint;
import com.supporttickets.core.servlets.support.TicketListEndpoint;
import com.supporttickets.core.servlets.support.TicketStatusEndpoint;
import com.supporttickets.core.servlets.support.UserListEndpoint;

/**
 * JSON API entry point for all Core support-ticket endpoints.
 */
@Component(
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Support Tickets JSON API Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + ApiPathParser.API_ROOT,
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + TicketConstants.API_RESOURCE_TYPE,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_PUT,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=PATCH",
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json"
        })
public class SupportTicketsApiServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(SupportTicketsApiServlet.class);

    @Reference
    private transient ResourceResolverProvider resourceResolverProvider;

    @Reference
    private transient TicketListEndpoint ticketListEndpoint;

    @Reference
    private transient TicketDetailEndpoint ticketDetailEndpoint;

    @Reference
    private transient TicketStatusEndpoint ticketStatusEndpoint;

    @Reference
    private transient TicketCommentEndpoint ticketCommentEndpoint;

    @Reference
    private transient UserListEndpoint userListEndpoint;

    @Override
    protected void service(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        ApiPathParser.ParsedRoute route = ApiPathParser.parse(request);
        if (route.getRoute() == ApiPathParser.Route.UNKNOWN) {
            ApiPathParser.writeError(response, 404, notFound("Resource not found."));
            return;
        }

        try (ResourceResolver resolver = resourceResolverProvider.getServiceResourceResolver()) {
            handle(request, response, route, resolver);
        } catch (ValidationException ex) {
            LOG.warn("Validation failure on {}: {}", request.getRequestURI(), ex.getFields());
            ApiPathParser.writeError(response, 400, ApiErrorMapper.validationError(ex));
        } catch (TicketNotFoundException ex) {
            LOG.debug("Ticket not found: {}", ex.getTicketId());
            ApiPathParser.writeError(response, 404, ApiErrorMapper.notFound(ex));
        } catch (InvalidTransitionException ex) {
            LOG.warn(
                    "Invalid transition from {} to {}",
                    ex.getCurrentStatus(),
                    ex.getRequestedStatus());
            ApiPathParser.writeError(response, 409, ApiErrorMapper.invalidTransition(ex));
        } catch (InternalServiceException ex) {
            LOG.error("Internal service error on {}", request.getRequestURI(), ex);
            ApiPathParser.writeError(response, 500, ApiErrorMapper.internalError());
        } catch (Exception ex) {
            LOG.error("Unexpected error on {}", request.getRequestURI(), ex);
            ApiPathParser.writeError(response, 500, ApiErrorMapper.internalError());
        }
    }

    private void handle(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            ApiPathParser.ParsedRoute route,
            ResourceResolver resolver) throws IOException {
        switch (route.getRoute()) {
            case LIST:
                dispatchList(request, response, resolver);
                break;
            case USERS:
                dispatchUsers(request, response, resolver);
                break;
            case DETAIL:
                dispatchDetail(request, response, route.getTicketId(), resolver);
                break;
            case STATUS:
                dispatchStatus(request, response, route.getTicketId(), resolver);
                break;
            case COMMENTS:
                dispatchComments(request, response, route.getTicketId(), resolver);
                break;
            default:
                ApiPathParser.writeError(response, 404, notFound("Resource not found."));
        }
    }

    private void dispatchList(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            ResourceResolver resolver) throws IOException {
        String method = request.getMethod();
        if (HttpConstants.METHOD_GET.equals(method)) {
            ticketListEndpoint.doGet(request, response, resolver);
            return;
        }
        if (HttpConstants.METHOD_POST.equals(method)) {
            ticketListEndpoint.doPost(request, response, resolver);
            return;
        }
        ApiPathParser.writeError(response, 405, ApiErrorMapper.methodNotAllowed());
    }

    private void dispatchUsers(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            ResourceResolver resolver) throws IOException {
        if (!HttpConstants.METHOD_GET.equals(request.getMethod())) {
            ApiPathParser.writeError(response, 405, ApiErrorMapper.methodNotAllowed());
            return;
        }
        userListEndpoint.doGet(response, resolver);
    }

    private void dispatchDetail(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            String ticketId,
            ResourceResolver resolver) throws IOException {
        String method = request.getMethod();
        if (HttpConstants.METHOD_GET.equals(method)) {
            ticketDetailEndpoint.doGet(request, response, ticketId, resolver);
            return;
        }
        if (HttpConstants.METHOD_PUT.equals(method)) {
            ticketDetailEndpoint.doPut(request, response, ticketId, resolver);
            return;
        }
        ApiPathParser.writeError(response, 405, ApiErrorMapper.methodNotAllowed());
    }

    private void dispatchStatus(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            String ticketId,
            ResourceResolver resolver) throws IOException {
        if (!"PATCH".equals(request.getMethod())) {
            ApiPathParser.writeError(response, 405, ApiErrorMapper.methodNotAllowed());
            return;
        }
        ticketStatusEndpoint.doPatch(request, response, ticketId, resolver);
    }

    private void dispatchComments(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response,
            String ticketId,
            ResourceResolver resolver) throws IOException {
        if (!HttpConstants.METHOD_POST.equals(request.getMethod())) {
            ApiPathParser.writeError(response, 405, ApiErrorMapper.methodNotAllowed());
            return;
        }
        ticketCommentEndpoint.doPost(request, response, ticketId, resolver);
    }

    private static com.supporttickets.core.api.error.ApiErrorResponse notFound(String message) {
        com.supporttickets.core.api.error.ApiErrorResponse error = new com.supporttickets.core.api.error.ApiErrorResponse();
        error.setCode(com.supporttickets.core.api.error.ErrorCode.NOT_FOUND.name());
        error.setMessage(message);
        return error;
    }
}
