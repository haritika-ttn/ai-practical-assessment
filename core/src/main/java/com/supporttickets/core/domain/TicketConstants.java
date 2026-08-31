package com.supporttickets.core.domain;

/**
 * JCR paths, property names, and resource types for ticket persistence.
 */
public final class TicketConstants {

    public static final String CONTENT_ROOT = "/content/support-tickets";
    public static final String TICKETS_PATH = CONTENT_ROOT + "/tickets";
    public static final String COMMENTS_NODE_NAME = "comments";

    public static final String TICKET_RESOURCE_TYPE = "support-tickets/components/ticket";
    public static final String COMMENT_RESOURCE_TYPE = "support-tickets/components/comment";
    public static final String API_RESOURCE_TYPE = "support-tickets/api";

    public static final String USER_BASE_PATH = "/home/users/support";

    public static final String PROP_TITLE = "title";
    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_PRIORITY = "priority";
    public static final String PROP_STATUS = "status";
    public static final String PROP_ASSIGNED_TO = "assignedTo";
    public static final String PROP_CREATED_BY = "createdBy";
    public static final String PROP_CREATED_AT = "createdAt";
    public static final String PROP_UPDATED_AT = "updatedAt";
    public static final String PROP_TICKET_ID = "ticketId";
    public static final String PROP_MESSAGE = "message";

    public static final String SERVICE_USER_SUBSERVICE = "support-tickets-service";

    private TicketConstants() {
    }
}
