package com.supporttickets.core.service;

import java.util.List;
import java.util.Optional;

import org.apache.sling.api.resource.ResourceResolver;

import com.supporttickets.core.api.dto.User;

/**
 * Resolves AEM user principals under {@code /home/users/support/}.
 */
public interface UserLookupService {

    boolean userExists(ResourceResolver resolver, String userPath);

    Optional<User> getUser(ResourceResolver resolver, String userPath);

    List<User> listSeededUsers(ResourceResolver resolver);
}
