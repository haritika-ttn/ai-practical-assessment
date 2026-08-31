package com.supporttickets.core.service;

import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;

import com.supporttickets.core.api.dto.TicketSummary;
import com.supporttickets.core.domain.TicketStatus;

/**
 * Searches and filters tickets for the list API.
 */
public interface TicketSearchService {

    List<TicketSummary> search(ResourceResolver resolver, String keyword, TicketStatus status);

    void validateListQueryParameters(String keyword, String statusValue);
}
