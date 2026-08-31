package com.supporttickets.core.api.dto;

import java.util.ArrayList;
import java.util.List;

import com.supporttickets.core.domain.TicketStatus;

/**
 * Full ticket representation including description, comments, and allowed transitions.
 */
public class TicketDetail extends TicketSummary {

    private List<Comment> comments = new ArrayList<>();
    private List<TicketStatus> allowedTransitions = new ArrayList<>();

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments == null ? new ArrayList<>() : new ArrayList<>(comments);
    }

    public List<TicketStatus> getAllowedTransitions() {
        return allowedTransitions;
    }

    public void setAllowedTransitions(List<TicketStatus> allowedTransitions) {
        this.allowedTransitions = allowedTransitions == null
                ? new ArrayList<>()
                : new ArrayList<>(allowedTransitions);
    }
}
