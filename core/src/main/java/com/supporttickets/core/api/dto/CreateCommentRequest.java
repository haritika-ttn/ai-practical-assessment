package com.supporttickets.core.api.dto;

/**
 * Input for adding a comment to a ticket.
 */
public class CreateCommentRequest {

    private String message;
    private String createdBy;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
