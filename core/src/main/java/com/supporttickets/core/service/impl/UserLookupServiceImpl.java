package com.supporttickets.core.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.supporttickets.core.api.dto.User;
import com.supporttickets.core.domain.TicketConstants;
import com.supporttickets.core.exception.InternalServiceException;
import com.supporttickets.core.service.UserLookupService;
import com.supporttickets.core.util.JcrPathUtil;

/**
 * User lookup backed by Jackrabbit UserManager.
 */
@Component(service = UserLookupService.class)
public class UserLookupServiceImpl implements UserLookupService {

    private static final Logger LOG = LoggerFactory.getLogger(UserLookupServiceImpl.class);

    @Override
    public boolean userExists(ResourceResolver resolver, String userPath) {
        if (!JcrPathUtil.isUnderSupportUserBase(userPath)) {
            return false;
        }
        return getAuthorizable(resolver, userPath).isPresent();
    }

    @Override
    public Optional<User> getUser(ResourceResolver resolver, String userPath) {
        if (!JcrPathUtil.isUnderSupportUserBase(userPath)) {
            return Optional.empty();
        }
        return getAuthorizable(resolver, userPath).map(this::toUser);
    }

    @Override
    public List<User> listSeededUsers(ResourceResolver resolver) {
        UserManager userManager = resolver.adaptTo(UserManager.class);
        if (userManager == null) {
            throw new InternalServiceException("UserManager is not available", null);
        }

        List<User> users = new ArrayList<>();
        try {
            addUsersByRole(userManager, users, "AGENT");
            addUsersByRole(userManager, users, "SUPERVISOR");

            if (users.isEmpty()) {
                addUsersByQuery(resolver, userManager, users);
            }
        } catch (RepositoryException ex) {
            throw new InternalServiceException("Failed to list seeded users", ex);
        }

        users.sort((left, right) -> left.getId().compareToIgnoreCase(right.getId()));
        return users;
    }

    private void addUsersByRole(UserManager userManager, List<User> users, String role)
            throws RepositoryException {
        Iterator<Authorizable> iterator = userManager.findAuthorizables(
                "profile/role", role, UserManager.SEARCH_TYPE_USER);
        while (iterator.hasNext()) {
            Authorizable authorizable = iterator.next();
            if (authorizable.isGroup()) {
                continue;
            }
            String path = authorizable.getPath();
            if (!JcrPathUtil.isUnderSupportUserBase(path) || containsUser(users, path)) {
                continue;
            }
            users.add(toUser(authorizable));
        }
    }

    private void addUsersByQuery(ResourceResolver resolver, UserManager userManager, List<User> users)
            throws RepositoryException {
        Session session = resolver.adaptTo(Session.class);
        if (session == null) {
            LOG.warn("Could not query seeded users: JCR session unavailable");
            return;
        }

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        String statement = "SELECT u.* FROM [rep:User] AS u "
                + "WHERE ISDESCENDANTNODE(u, '" + TicketConstants.USER_BASE_PATH + "')";
        Query query = queryManager.createQuery(statement, Query.JCR_SQL2);
        NodeIterator nodes = query.execute().getNodes();
        while (nodes.hasNext()) {
            Authorizable authorizable = userManager.getAuthorizableByPath(nodes.nextNode().getPath());
            if (authorizable == null || authorizable.isGroup()) {
                continue;
            }
            String path = authorizable.getPath();
            if (!JcrPathUtil.isUnderSupportUserBase(path) || containsUser(users, path)) {
                continue;
            }
            users.add(toUser(authorizable));
        }
    }

    private boolean containsUser(List<User> users, String path) {
        return users.stream().anyMatch(user -> path.equals(user.getId()));
    }

    private Optional<Authorizable> getAuthorizable(ResourceResolver resolver, String userPath) {
        UserManager userManager = resolver.adaptTo(UserManager.class);
        if (userManager == null) {
            throw new InternalServiceException("UserManager is not available", null);
        }
        try {
            Authorizable authorizable = userManager.getAuthorizableByPath(userPath);
            if (authorizable == null || authorizable.isGroup()) {
                return Optional.empty();
            }
            return Optional.of(authorizable);
        } catch (RepositoryException ex) {
            throw new InternalServiceException("Failed to resolve user: " + userPath, ex);
        }
    }

    private User toUser(Authorizable authorizable) {
        try {
            String path = authorizable.getPath();
            User user = new User();
            user.setId(path);
            user.setName(resolveDisplayName(authorizable, path));
            user.setEmail(readProperty(authorizable, "profile/email"));
            user.setRole(readProperty(authorizable, "profile/role"));
            return user;
        } catch (RepositoryException ex) {
            throw new InternalServiceException("Failed to read user properties", ex);
        }
    }

    private String resolveDisplayName(Authorizable authorizable, String path) throws RepositoryException {
        if (authorizable.getProperty("profile/givenName") != null
                && authorizable.getProperty("profile/familyName") != null) {
            String given = authorizable.getProperty("profile/givenName")[0].getString();
            String family = authorizable.getProperty("profile/familyName")[0].getString();
            return (given + " " + family).trim();
        }
        if (authorizable.getProperty("profile/title") != null) {
            return authorizable.getProperty("profile/title")[0].getString();
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private String readProperty(Authorizable authorizable, String propertyPath) throws RepositoryException {
        if (authorizable.getProperty(propertyPath) == null) {
            return null;
        }
        return authorizable.getProperty(propertyPath)[0].getString();
    }
}
