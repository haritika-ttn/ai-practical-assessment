package com.supporttickets.core.testcontext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import com.day.cq.search.QueryBuilder;
import com.supporttickets.core.api.dto.CreateTicketRequest;
import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.domain.TicketConstants;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.repository.TicketRepository;
import com.supporttickets.core.repository.impl.TicketRepositoryImpl;
import com.supporttickets.core.service.TicketSearchService;
import com.supporttickets.core.service.TicketStateMachineService;
import com.supporttickets.core.service.UserLookupService;
import com.supporttickets.core.service.impl.TicketSearchServiceImpl;
import com.supporttickets.core.service.impl.TicketStateMachineServiceImpl;
import com.supporttickets.core.validation.TicketValidator;
import com.supporttickets.core.validation.impl.TicketValidatorImpl;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/**
 * Shared AEM Mock wiring for support-ticket integration tests.
 */
@ExtendWith(AemContextExtension.class)
public abstract class SupportTicketsIntegrationTestBase {

    public static final String AGENT1 = "/home/users/support/agent1";
    public static final String AGENT2 = "/home/users/support/agent2";
    public static final String UNKNOWN_USER = "/home/users/support/nonexistent";

    protected final AemContext context = AppAemContext.newAemContext();

    protected ResourceResolver resolver;
    protected TicketRepository repository;
    protected TicketValidator validator;
    protected TicketSearchService searchService;
    protected UserLookupService userLookupService;

    @BeforeEach
    void setUpSupportTicketsContext() {
        context.create().resource(TicketConstants.TICKETS_PATH, "jcr:primaryType", "sling:Folder");
        context.create().resource("/home/users/support", "jcr:primaryType", "sling:Folder");
        context.create().resource(AGENT1, "jcr:primaryType", "rep:User");
        context.create().resource(AGENT2, "jcr:primaryType", "rep:User");

        userLookupService = Mockito.mock(UserLookupService.class);
        lenient().when(userLookupService.userExists(any(), eq(AGENT1))).thenReturn(true);
        lenient().when(userLookupService.userExists(any(), eq(AGENT2))).thenReturn(true);
        lenient().when(userLookupService.userExists(any(), eq(UNKNOWN_USER))).thenReturn(false);

        context.registerService(UserLookupService.class, userLookupService);
        context.registerService(TicketStateMachineService.class, new TicketStateMachineServiceImpl());
        context.registerService(QueryBuilder.class, Mockito.mock(QueryBuilder.class));
        repository = context.registerInjectActivateService(new TicketRepositoryImpl());
        validator = context.registerInjectActivateService(new TicketValidatorImpl());
        searchService = context.registerInjectActivateService(new TicketSearchServiceImpl());
        resolver = context.resourceResolver();
    }

    protected TicketDetail createValidatedTicket(String title, String description, Priority priority) {
        return createValidatedTicket(title, description, priority, AGENT1, null);
    }

    protected TicketDetail createValidatedTicket(
            String title,
            String description,
            Priority priority,
            String createdBy,
            String assignedTo) {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setPriority(priority);
        request.setCreatedBy(createdBy);
        request.setAssignedTo(assignedTo);
        validator.validateCreate(request, resolver);
        return repository.create(resolver, request);
    }

    protected TicketDetail requireTicketInStatus(TicketStatus targetStatus) {
        TicketDetail ticket = createValidatedTicket(
                "Status seed " + targetStatus.name(),
                "Seed ticket for status " + targetStatus.name(),
                Priority.MEDIUM);
        return advanceTicketToStatus(ticket.getId(), targetStatus);
    }

    protected TicketDetail advanceTicketToStatus(String ticketId, TicketStatus targetStatus) {
        TicketDetail current = repository.findById(resolver, ticketId);
        if (current.getStatus() == targetStatus) {
            return current;
        }

        switch (targetStatus) {
            case OPEN:
                return current;
            case IN_PROGRESS:
                return repository.updateStatus(resolver, ticketId, TicketStatus.IN_PROGRESS);
            case RESOLVED:
                current = repository.updateStatus(resolver, ticketId, TicketStatus.IN_PROGRESS);
                return repository.updateStatus(resolver, ticketId, TicketStatus.RESOLVED);
            case CLOSED:
                current = advanceTicketToStatus(ticketId, TicketStatus.RESOLVED);
                return repository.updateStatus(resolver, ticketId, TicketStatus.CLOSED);
            case CANCELLED:
                if (current.getStatus() == TicketStatus.OPEN) {
                    return repository.updateStatus(resolver, ticketId, TicketStatus.CANCELLED);
                }
                if (current.getStatus() == TicketStatus.IN_PROGRESS) {
                    return repository.updateStatus(resolver, ticketId, TicketStatus.CANCELLED);
                }
                throw new IllegalStateException("Cannot reach CANCELLED from " + current.getStatus());
            default:
                throw new IllegalArgumentException("Unsupported status: " + targetStatus);
        }
    }

    protected int countTicketNodes() {
        var ticketsRoot = resolver.getResource(TicketConstants.TICKETS_PATH);
        if (ticketsRoot == null) {
            return 0;
        }
        int count = 0;
        for (var child : ticketsRoot.getChildren()) {
            if (TicketConstants.TICKET_RESOURCE_TYPE.equals(child.getResourceType())) {
                count++;
            }
        }
        return count;
    }
}
