package com.supporttickets.core.service.impl;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.supporttickets.core.domain.TicketConstants;
import com.supporttickets.core.exception.InternalServiceException;
import com.supporttickets.core.service.ResourceResolverProvider;

/**
 * Acquires {@link ResourceResolver} instances for the support-tickets service user.
 */
@Component(service = ResourceResolverProvider.class)
public class ResourceResolverProviderImpl implements ResourceResolverProvider {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public ResourceResolver getServiceResourceResolver() {
        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE,
                TicketConstants.SERVICE_USER_SUBSERVICE);
        try {
            return resourceResolverFactory.getServiceResourceResolver(authInfo);
        } catch (LoginException ex) {
            throw new InternalServiceException("Unable to obtain service resource resolver", ex);
        }
    }
}
