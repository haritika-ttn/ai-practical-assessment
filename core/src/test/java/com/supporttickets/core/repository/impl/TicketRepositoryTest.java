package com.supporttickets.core.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.supporttickets.core.api.dto.CreateCommentRequest;
import com.supporttickets.core.api.dto.CreateTicketRequest;
import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.api.dto.UpdateTicketRequest;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.domain.TicketConstants;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.InvalidTransitionException;
import com.supporttickets.core.exception.TicketNotFoundException;
import com.supporttickets.core.service.TicketStateMachineService;
import com.supporttickets.core.service.impl.TicketStateMachineServiceImpl;
import com.supporttickets.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class TicketRepositoryTest {

    private final AemContext context = AppAemContext.newAemContext();

    private TicketRepositoryImpl repository;
    private ResourceResolver resolver;

    @BeforeEach
    void setUp() {
        context.create().resource(TicketConstants.TICKETS_PATH, "jcr:primaryType", "sling:Folder");
        context.registerService(TicketStateMachineService.class, new TicketStateMachineServiceImpl());
        repository = context.registerInjectActivateService(new TicketRepositoryImpl());
        resolver = context.resourceResolver();
    }

    @Test
    void createTicketPersistsOpenStatus() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Printer offline");
        request.setDescription("Cannot print invoices");
        request.setPriority(Priority.HIGH);
        request.setCreatedBy("/home/users/support/agent1");

        TicketDetail created = repository.create(resolver, request);

        assertNotNull(created.getId());
        assertEquals(TicketStatus.OPEN, created.getStatus());
        assertEquals(Priority.HIGH, created.getPriority());
        assertEquals(2, created.getAllowedTransitions().size());

        TicketDetail loaded = repository.findById(resolver, created.getId());
        assertEquals("Printer offline", loaded.getTitle());
        assertEquals(TicketStatus.OPEN, loaded.getStatus());
    }

    @Test
    void updateDoesNotChangeStatus() {
        TicketDetail created = repository.create(resolver, buildCreateRequest());

        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setTitle("Updated title");
        update.setPriority(Priority.CRITICAL);

        TicketDetail updated = repository.update(resolver, created.getId(), update);

        assertEquals("Updated title", updated.getTitle());
        assertEquals(Priority.CRITICAL, updated.getPriority());
        assertEquals(TicketStatus.OPEN, updated.getStatus());
    }

    @Test
    void updateStatusDelegatesToStateMachine() {
        TicketDetail created = repository.create(resolver, buildCreateRequest());

        TicketDetail inProgress = repository.updateStatus(
                resolver, created.getId(), TicketStatus.IN_PROGRESS);
        assertEquals(TicketStatus.IN_PROGRESS, inProgress.getStatus());

        assertThrows(
                InvalidTransitionException.class,
                () -> repository.updateStatus(resolver, created.getId(), TicketStatus.CLOSED));
    }

    @Test
    void addCommentUpdatesTicketTimestampAndSortsComments() {
        TicketDetail created = repository.create(resolver, buildCreateRequest());

        CreateCommentRequest first = new CreateCommentRequest();
        first.setMessage("First update");
        first.setCreatedBy("/home/users/support/agent1");
        repository.addComment(resolver, created.getId(), first);

        CreateCommentRequest second = new CreateCommentRequest();
        second.setMessage("Second update");
        second.setCreatedBy("/home/users/support/agent2");
        repository.addComment(resolver, created.getId(), second);

        TicketDetail loaded = repository.findById(resolver, created.getId());
        assertEquals(2, loaded.getComments().size());
        assertTrue(loaded.getComments().get(0).getCreatedAt()
                .compareTo(loaded.getComments().get(1).getCreatedAt()) <= 0);
        assertNotNull(loaded.getUpdatedAt());
    }

    @Test
    void updateStatusInvalidTransitionDoesNotModifyTicket() {
        TicketDetail created = repository.create(resolver, buildCreateRequest());
        String originalUpdatedAt = created.getUpdatedAt();

        assertThrows(
                InvalidTransitionException.class,
                () -> repository.updateStatus(resolver, created.getId(), TicketStatus.CLOSED));

        TicketDetail reloaded = repository.findById(resolver, created.getId());
        assertEquals(TicketStatus.OPEN, reloaded.getStatus());
        assertEquals(originalUpdatedAt, reloaded.getUpdatedAt());
    }

    @Test
    void findByIdThrowsForMissingTicket() {
        assertThrows(
                TicketNotFoundException.class,
                () -> repository.findById(resolver, "550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    void findByIdThrowsForInvalidId() {
        assertThrows(
                TicketNotFoundException.class,
                () -> repository.findById(resolver, "../etc/passwd"));
    }

    private CreateTicketRequest buildCreateRequest() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("VPN issue");
        request.setDescription("Cannot connect");
        request.setPriority(Priority.MEDIUM);
        request.setCreatedBy("/home/users/support/agent1");
        return request;
    }
}
