package com.supporttickets.core.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.supporttickets.core.api.dto.TicketSummary;
import com.supporttickets.core.domain.Priority;
import com.supporttickets.core.domain.TicketConstants;
import com.supporttickets.core.domain.TicketStatus;
import com.supporttickets.core.exception.InternalServiceException;
import com.supporttickets.core.exception.ValidationException;
import com.supporttickets.core.service.TicketSearchService;
import com.supporttickets.core.util.LikeEscapeUtil;

/**
 * Oak QueryBuilder-backed ticket search with repository traversal fallback.
 */
@Component(service = TicketSearchService.class)
public class TicketSearchServiceImpl implements TicketSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketSearchServiceImpl.class);
    private static final int KEYWORD_MAX_LENGTH = 200;

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public List<TicketSummary> search(ResourceResolver resolver, String keyword, TicketStatus status) {
        Session session = resolver.adaptTo(Session.class);
        if (session != null) {
            try {
                Query query = queryBuilder.createQuery(PredicateGroup.create(buildPredicates(keyword, status)), session);
                query.setHitsPerPage(0);
                SearchResult result = query.getResult();
                List<TicketSummary> tickets = new ArrayList<>();
                for (Hit hit : result.getHits()) {
                    Resource resource = hit.getResource();
                    if (resource != null) {
                        tickets.add(toSummary(resource));
                    }
                }
                if (!tickets.isEmpty() || result.getTotalMatches() > 0) {
                    return tickets;
                }
            } catch (Exception ex) {
                LOG.debug("QueryBuilder search unavailable, falling back to direct traversal: {}", ex.getMessage());
            }
        }

        return searchByTraversal(resolver, keyword, status);
    }

    @Override
    public void validateListQueryParameters(String keyword, String statusValue) {
        List<String> fields = new ArrayList<>();

        if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
            fields.add("q");
        }

        if (statusValue != null && !statusValue.isBlank()) {
            try {
                TicketStatus.fromString(statusValue);
            } catch (IllegalArgumentException ex) {
                fields.add("status");
            }
        }

        if (!fields.isEmpty()) {
            throw new ValidationException("Invalid query parameter.", fields);
        }
    }

    private Map<String, String> buildPredicates(String keyword, TicketStatus status) {
        Map<String, String> predicates = new HashMap<>();
        predicates.put("path", TicketConstants.TICKETS_PATH);
        predicates.put("type", "nt:unstructured");
        predicates.put("1_property", "sling:resourceType");
        predicates.put("1_property.value", TicketConstants.TICKET_RESOURCE_TYPE);
        predicates.put("orderby", "@updatedAt");
        predicates.put("orderby.sort", "desc");

        if (status != null) {
            predicates.put("2_property", "status");
            predicates.put("2_property.value", status.name());
        }

        if (keyword != null && !keyword.isBlank()) {
            String escaped = LikeEscapeUtil.escape(keyword.trim().toLowerCase(Locale.ROOT));
            predicates.put("group.p.or", "true");
            predicates.put("group.1_property", "title");
            predicates.put("group.1_property.operation", "like");
            predicates.put("group.1_property.value", "%" + escaped + "%");
            predicates.put("group.2_property", "description");
            predicates.put("group.2_property.operation", "like");
            predicates.put("group.2_property.value", "%" + escaped + "%");
        }

        return predicates;
    }

    private List<TicketSummary> searchByTraversal(
            ResourceResolver resolver, String keyword, TicketStatus statusFilter) {
        Resource ticketsRoot = resolver.getResource(TicketConstants.TICKETS_PATH);
        if (ticketsRoot == null) {
            return List.of();
        }

        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);
        List<TicketSummary> tickets = new ArrayList<>();

        for (Resource child : ticketsRoot.getChildren()) {
            if (!TicketConstants.TICKET_RESOURCE_TYPE.equals(child.getResourceType())) {
                continue;
            }
            TicketSummary summary = toSummary(child);
            if (statusFilter != null && summary.getStatus() != statusFilter) {
                continue;
            }
            if (normalizedKeyword != null && !normalizedKeyword.isEmpty()
                    && !matchesKeyword(summary, normalizedKeyword)) {
                continue;
            }
            tickets.add(summary);
        }

        tickets.sort(Comparator.comparing(TicketSummary::getUpdatedAt, Comparator.nullsLast(String::compareTo)).reversed());
        return tickets;
    }

    private boolean matchesKeyword(TicketSummary summary, String keyword) {
        String title = summary.getTitle() == null ? "" : summary.getTitle().toLowerCase(Locale.ROOT);
        String description = summary.getDescription() == null ? "" : summary.getDescription().toLowerCase(Locale.ROOT);
        return title.contains(keyword) || description.contains(keyword);
    }

    private TicketSummary toSummary(Resource resource) {
        ValueMap properties = resource.getValueMap();
        TicketSummary summary = new TicketSummary();
        summary.setId(resource.getName());
        summary.setTitle(properties.get(TicketConstants.PROP_TITLE, String.class));
        summary.setDescription(properties.get(TicketConstants.PROP_DESCRIPTION, String.class));
        summary.setPriority(readPriority(properties));
        summary.setStatus(readStatus(properties));
        summary.setAssignedTo(readAssignedTo(properties));
        summary.setCreatedBy(properties.get(TicketConstants.PROP_CREATED_BY, String.class));
        summary.setCreatedAt(properties.get(TicketConstants.PROP_CREATED_AT, String.class));
        summary.setUpdatedAt(properties.get(TicketConstants.PROP_UPDATED_AT, String.class));
        return summary;
    }

    private Priority readPriority(ValueMap properties) {
        String priority = properties.get(TicketConstants.PROP_PRIORITY, String.class);
        return priority == null ? null : Priority.fromString(priority);
    }

    private TicketStatus readStatus(ValueMap properties) {
        String status = properties.get(TicketConstants.PROP_STATUS, String.class);
        return status == null ? null : TicketStatus.fromString(status);
    }

    private String readAssignedTo(ValueMap properties) {
        String assignedTo = properties.get(TicketConstants.PROP_ASSIGNED_TO, String.class);
        if (assignedTo == null || assignedTo.isBlank()) {
            return null;
        }
        return assignedTo;
    }
}
