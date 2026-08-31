package com.supporttickets.core.api.dto;

import com.supporttickets.core.domain.Priority;

/**
 * Input for updating an existing ticket. Null fields are left unchanged.
 */
public class UpdateTicketRequest {

    private String title;
    private String description;
    private Priority priority;
    private String assignedTo;
    private boolean assignedToProvided;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
        this.assignedToProvided = true;
    }

    public boolean isAssignedToProvided() {
        return assignedToProvided;
    }
}
