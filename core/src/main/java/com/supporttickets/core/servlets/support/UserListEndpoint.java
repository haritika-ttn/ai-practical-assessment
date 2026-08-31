package com.supporttickets.core.servlets.support;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.supporttickets.core.service.UserLookupService;

/**
 * Handles seeded user listing.
 */
@Component(service = UserListEndpoint.class)
public class UserListEndpoint {

    @Reference
    private UserLookupService userLookupService;

    public void doGet(SlingHttpServletResponse response, ResourceResolver resolver) throws IOException {
        ApiPathParser.writeJson(response, 200, userLookupService.listSeededUsers(resolver));
    }
}
