package com.supporttickets.core.resource;

import java.util.Collections;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.osgi.service.component.annotations.Component;

import com.supporttickets.core.domain.TicketConstants;
import com.supporttickets.core.servlets.support.ApiPathParser;

/**
 * Supplies synthetic resources for nested {@code /bin/support-tickets/...} API paths.
 *
 * <p>Sling path servlets only bind to exact paths such as {@code /bin/support-tickets.json}.
 * Nested URLs like {@code /bin/support-tickets/users.json} need a resolvable resource before
 * the API servlet can run.</p>
 */
@Component(
        service = ResourceProvider.class,
        property = {
                ResourceProvider.ROOTS + "=" + ApiPathParser.API_ROOT
        })
public class SupportTicketsApiResourceProvider implements ResourceProvider {

    @Override
    public Resource getResource(ResourceResolver resolver, String path) {
        if (!ApiPathParser.isApiResourcePath(path)) {
            return null;
        }
        return new SyntheticResource(resolver, path, TicketConstants.API_RESOURCE_TYPE);
    }

    @Override
    public Resource getResource(ResourceResolver resolver, HttpServletRequest request, String path) {
        return getResource(resolver, path);
    }

    @Override
    public Iterator<Resource> listChildren(Resource parent) {
        return Collections.emptyIterator();
    }
}
