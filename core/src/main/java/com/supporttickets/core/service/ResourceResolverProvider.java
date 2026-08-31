package com.supporttickets.core.service;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * Provides service-user resource resolvers for API operations.
 */
public interface ResourceResolverProvider {

    ResourceResolver getServiceResourceResolver();
}
