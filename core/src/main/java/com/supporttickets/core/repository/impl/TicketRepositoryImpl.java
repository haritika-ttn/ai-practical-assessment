package com.supporttickets.core.repository.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.supporttickets.core.api.dto.Comment;
import com.supporttickets.core.api.dto.CreateCommentRequest;
import com.supporttickets.core.api.dto.CreateTicketRequest;
import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.api.dto.UpdateTicketRequest;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.domain.TicketConstants;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.InternalServiceException;
import com.supporttickets.core.exception.TicketNotFoundException;
import com.supporttickets.core.repository.TicketRepository;
import com.supporttickets.core.service.TicketStateMachineService;
import com.supporttickets.core.util.DateTimeUtil;
import com.supporttickets.core.util.JcrPathUtil;

/**
 * Sole JCR writer for ticket and comment nodes.
 */
@Component(service = TicketRepository.class)
public class TicketRepositoryImpl implements TicketRepository {

    private static final Logger LOG = LoggerFactory.getLogger(TicketRepositoryImpl.class);

    @Reference
    private TicketStateMachineService stateMachineService;

    @Override
    public TicketDetail create(ResourceResolver resolver, CreateTicketRequest request) {
        ensureTicketsRootExists(resolver);

        String ticketId = JcrPathUtil.newUuid();
        String now = DateTimeUtil.nowUtc();
        Resource ticketsRoot = requireTicketsRoot(resolver);

        Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:primaryType", "nt:unstructured");
        properties.put("sling:resourceType", TicketConstants.TICKET_RESOURCE_TYPE);
        properties.put(TicketConstants.PROP_TITLE, trim(request.getTitle()));
        properties.put(TicketConstants.PROP_DESCRIPTION, nullToEmpty(request.getDescription()));
        properties.put(TicketConstants.PROP_PRIORITY, request.getPriority().name());
        properties.put(TicketConstants.PROP_STATUS, TicketStatus.OPEN.name());
        properties.put(TicketConstants.PROP_CREATED_BY, trim(request.getCreatedBy()));
        properties.put(TicketConstants.PROP_CREATED_AT, now);
        properties.put(TicketConstants.PROP_UPDATED_AT, now);

        String assignedTo = trimToNull(request.getAssignedTo());
        if (assignedTo != null) {
            properties.put(TicketConstants.PROP_ASSIGNED_TO, assignedTo);
        }

        try {
            Resource ticketResource = resolver.create(ticketsRoot, ticketId, properties);
            commit(resolver);
            return toTicketDetail(ticketResource, List.of());
        } catch (PersistenceException ex) {
            throw new InternalServiceException("Failed to create ticket", ex);
        }
    }

    @Override
    public TicketDetail findById(ResourceResolver resolver, String ticketId) {
        Resource ticketResource = getTicketResource(resolver, ticketId);
        List<Comment> comments = findComments(resolver, ticketId);
        return toTicketDetail(ticketResource, comments);
    }

    @Override
    public TicketDetail update(ResourceResolver resolver, String ticketId, UpdateTicketRequest request) {
        Resource ticketResource = getTicketResource(resolver, ticketId);
        ModifiableValueMap properties = ticketResource.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new InternalServiceException("Ticket resource is not modifiable: " + ticketId, null);
        }

        if (request.getTitle() != null) {
            properties.put(TicketConstants.PROP_TITLE, trim(request.getTitle()));
        }
        if (request.getDescription() != null) {
            properties.put(TicketConstants.PROP_DESCRIPTION, request.getDescription());
        }
        if (request.getPriority() != null) {
            properties.put(TicketConstants.PROP_PRIORITY, request.getPriority().name());
        }
        if (request.isAssignedToProvided()) {
            String assignedTo = trimToNull(request.getAssignedTo());
            if (assignedTo == null) {
                properties.remove(TicketConstants.PROP_ASSIGNED_TO);
            } else {
                properties.put(TicketConstants.PROP_ASSIGNED_TO, assignedTo);
            }
        }

        properties.put(TicketConstants.PROP_UPDATED_AT, DateTimeUtil.nowUtcAfter(
                properties.get(TicketConstants.PROP_UPDATED_AT, String.class)));
        commit(resolver);

        return findById(resolver, ticketId);
    }

    @Override
    public TicketDetail updateStatus(ResourceResolver resolver, String ticketId, TicketStatus newStatus) {
        Resource ticketResource = getTicketResource(resolver, ticketId);
        ModifiableValueMap properties = ticketResource.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new InternalServiceException("Ticket resource is not modifiable: " + ticketId, null);
        }

        TicketStatus currentStatus = readStatus(properties);
        TicketStatus nextStatus = stateMachineService.applyTransition(currentStatus, newStatus);

        properties.put(TicketConstants.PROP_STATUS, nextStatus.name());
        properties.put(TicketConstants.PROP_UPDATED_AT, DateTimeUtil.nowUtcAfter(
                properties.get(TicketConstants.PROP_UPDATED_AT, String.class)));
        commit(resolver);

        LOG.debug("Ticket {} transitioned from {} to {}", ticketId, currentStatus, nextStatus);
        return findById(resolver, ticketId);
    }

    @Override
    public Comment addComment(ResourceResolver resolver, String ticketId, CreateCommentRequest request) {
        Resource ticketResource = getTicketResource(resolver, ticketId);
        Resource commentsFolder = ensureCommentsFolder(resolver, ticketResource);

        String commentId = JcrPathUtil.newUuid();
        String now = DateTimeUtil.nowUtc();

        Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:primaryType", "nt:unstructured");
        properties.put("sling:resourceType", TicketConstants.COMMENT_RESOURCE_TYPE);
        properties.put(TicketConstants.PROP_TICKET_ID, ticketId);
        properties.put(TicketConstants.PROP_MESSAGE, trim(request.getMessage()));
        properties.put(TicketConstants.PROP_CREATED_BY, trim(request.getCreatedBy()));
        properties.put(TicketConstants.PROP_CREATED_AT, now);

        try {
            Resource commentResource = resolver.create(commentsFolder, commentId, properties);

            ModifiableValueMap ticketProperties = ticketResource.adaptTo(ModifiableValueMap.class);
            if (ticketProperties != null) {
                ticketProperties.put(
                        TicketConstants.PROP_UPDATED_AT,
                        DateTimeUtil.maxUtcAfter(
                                readTimestamp(ticketProperties.get(TicketConstants.PROP_UPDATED_AT)),
                                now));
            }

            commit(resolver);
            return toComment(commentResource);
        } catch (PersistenceException ex) {
            throw new InternalServiceException("Failed to add comment to ticket " + ticketId, ex);
        }
    }

    @Override
    public List<Comment> findComments(ResourceResolver resolver, String ticketId) {
        if (!JcrPathUtil.isValidUuid(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }

        Resource ticketResource = resolver.getResource(buildTicketPath(ticketId));
        if (ticketResource == null) {
            throw new TicketNotFoundException(ticketId);
        }

        Resource commentsFolder = ticketResource.getChild(TicketConstants.COMMENTS_NODE_NAME);
        if (commentsFolder == null) {
            return List.of();
        }

        List<Comment> comments = new ArrayList<>();
        for (Resource child : commentsFolder.getChildren()) {
            if (TicketConstants.COMMENT_RESOURCE_TYPE.equals(child.getResourceType())) {
                comments.add(toComment(child));
            }
        }

        comments.sort(Comparator.comparing(Comment::getCreatedAt, Comparator.nullsLast(String::compareTo)));
        return comments;
    }

    private Resource getTicketResource(ResourceResolver resolver, String ticketId) {
        if (!JcrPathUtil.isValidUuid(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }

        Resource ticketResource = resolver.getResource(buildTicketPath(ticketId));
        if (ticketResource == null
                || !TicketConstants.TICKET_RESOURCE_TYPE.equals(ticketResource.getResourceType())) {
            throw new TicketNotFoundException(ticketId);
        }
        return ticketResource;
    }

    private TicketDetail toTicketDetail(Resource ticketResource, List<Comment> comments) {
        ValueMap properties = ticketResource.getValueMap();
        TicketDetail detail = new TicketDetail();
        detail.setId(ticketResource.getName());
        detail.setTitle(properties.get(TicketConstants.PROP_TITLE, String.class));
        detail.setDescription(properties.get(TicketConstants.PROP_DESCRIPTION, String.class));
        detail.setPriority(readPriority(properties));
        detail.setStatus(readStatus(properties));
        detail.setAssignedTo(readAssignedTo(properties));
        detail.setCreatedBy(properties.get(TicketConstants.PROP_CREATED_BY, String.class));
        detail.setCreatedAt(properties.get(TicketConstants.PROP_CREATED_AT, String.class));
        detail.setUpdatedAt(properties.get(TicketConstants.PROP_UPDATED_AT, String.class));
        detail.setComments(comments);
        detail.setAllowedTransitions(
                stateMachineService.getAllowedTransitions(detail.getStatus()).stream()
                        .sorted()
                        .collect(Collectors.toList()));
        return detail;
    }

    private Comment toComment(Resource commentResource) {
        ValueMap properties = commentResource.getValueMap();
        Comment comment = new Comment();
        comment.setId(commentResource.getName());
        comment.setTicketId(properties.get(TicketConstants.PROP_TICKET_ID, String.class));
        comment.setMessage(properties.get(TicketConstants.PROP_MESSAGE, String.class));
        comment.setCreatedBy(properties.get(TicketConstants.PROP_CREATED_BY, String.class));
        comment.setCreatedAt(properties.get(TicketConstants.PROP_CREATED_AT, String.class));
        return comment;
    }

    private TicketStatus readStatus(ValueMap properties) {
        String status = properties.get(TicketConstants.PROP_STATUS, String.class);
        if (status == null) {
            throw new InternalServiceException("Ticket is missing status property", null);
        }
        return TicketStatus.fromString(status);
    }

    private Priority readPriority(ValueMap properties) {
        String priority = properties.get(TicketConstants.PROP_PRIORITY, String.class);
        if (priority == null) {
            throw new InternalServiceException("Ticket is missing priority property", null);
        }
        return Priority.fromString(priority);
    }

    private String readAssignedTo(ValueMap properties) {
        String assignedTo = properties.get(TicketConstants.PROP_ASSIGNED_TO, String.class);
        return trimToNull(assignedTo);
    }

    private Resource ensureCommentsFolder(ResourceResolver resolver, Resource ticketResource) {
        Resource commentsFolder = ticketResource.getChild(TicketConstants.COMMENTS_NODE_NAME);
        if (commentsFolder != null) {
            return commentsFolder;
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:primaryType", "nt:unstructured");

        try {
            return resolver.create(ticketResource, TicketConstants.COMMENTS_NODE_NAME, properties);
        } catch (PersistenceException ex) {
            throw new InternalServiceException("Failed to create comments folder", ex);
        }
    }

    private void ensureTicketsRootExists(ResourceResolver resolver) {
        if (resolver.getResource(TicketConstants.TICKETS_PATH) != null) {
            return;
        }

        Session session = resolver.adaptTo(Session.class);
        if (session == null) {
            throw new InternalServiceException("JCR session is not available", null);
        }

        try {
            createPathIfMissing(session, TicketConstants.CONTENT_ROOT, "sling:Folder");
            createPathIfMissing(session, TicketConstants.TICKETS_PATH, "sling:Folder");
            session.save();
        } catch (RepositoryException ex) {
            throw new InternalServiceException("Failed to initialize ticket content path", ex);
        }
    }

    private void createPathIfMissing(Session session, String path, String nodeType) throws RepositoryException {
        if (session.nodeExists(path)) {
            return;
        }

        String relative = path.startsWith("/") ? path.substring(1) : path;
        String[] segments = relative.split("/");
        Node current = session.getRootNode();
        StringBuilder currentPath = new StringBuilder();

        for (String segment : segments) {
            if (currentPath.length() > 0) {
                currentPath.append('/');
            }
            currentPath.append(segment);
            String absolutePath = "/" + currentPath;
            if (!session.nodeExists(absolutePath)) {
                current = current.addNode(segment, nodeType);
            } else {
                current = session.getNode(absolutePath);
            }
        }
    }

    private Resource requireTicketsRoot(ResourceResolver resolver) {
        Resource ticketsRoot = resolver.getResource(TicketConstants.TICKETS_PATH);
        if (ticketsRoot == null) {
            throw new InternalServiceException("Tickets root is not available", null);
        }
        return ticketsRoot;
    }

    private void commit(ResourceResolver resolver) {
        try {
            resolver.commit();
        } catch (PersistenceException ex) {
            throw new InternalServiceException("Failed to commit repository changes", ex);
        }
    }

    private String buildTicketPath(String ticketId) {
        return TicketConstants.TICKETS_PATH + "/" + ticketId;
    }

    private String readTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
