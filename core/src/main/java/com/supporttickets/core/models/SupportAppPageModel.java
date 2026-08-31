package com.supporttickets.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

/**
 * Exposes API configuration to support-app HTL components.
 */
@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SupportAppPageModel {

    private static final String API_BASE = "/bin/support-tickets";
    private static final String CSRF_TOKEN_URL = "/libs/granite/csrf/token.json";
    private static final String LIST_PAGE_URL = "/content/support-app.html";
    private static final String CREATE_PAGE_URL = "/content/support-app/create.html";
    private static final String DETAIL_PAGE_URL = "/content/support-app/ticket.html";

    public String getApiBase() {
        return API_BASE;
    }

    public String getCsrfTokenUrl() {
        return CSRF_TOKEN_URL;
    }

    public String getListPageUrl() {
        return LIST_PAGE_URL;
    }

    public String getCreatePageUrl() {
        return CREATE_PAGE_URL;
    }

    public String getDetailPageUrl() {
        return DETAIL_PAGE_URL;
    }
}
