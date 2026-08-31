package com.supporttickets.core.repository;

import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;

import com.supporttickets.core.api.dto.Comment;
import com.supporttickets.core.api.dto.CreateCommentRequest;
import com.supporttickets.core.api.dto.CreateTicketRequest;
import com.supporttickets.core.api.dto.TicketDetail;
import com.supporttickets.core.api.dto.UpdateTicketRequest;
import com.supporttickets.core.domain.TicketStatus;

/**
 * JCR persistence for tickets and comments.
 */
public interface TicketRepository {

    TicketDetail create(ResourceResolver resolver, CreateTicketRequest request);

    TicketDetail findById(ResourceResolver resolver, String ticketId);

    TicketDetail update(ResourceResolver resolver, String ticketId, UpdateTicketRequest request);

    TicketDetail updateStatus(ResourceResolver resolver, String ticketId, TicketStatus newStatus);

    Comment addComment(ResourceResolver resolver, String ticketId, CreateCommentRequest request);

    List<Comment> findComments(ResourceResolver resolver, String ticketId);
}
