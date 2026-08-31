package com.supporttickets.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.supporttickets.core.api.dto.User;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceImplTest {

    private static final String AGENT1 = "/home/users/support/agent1";
    private static final String SUPERVISOR1 = "/home/users/support/supervisor1";

    @Mock
    private ResourceResolver resolver;

    @Mock
    private UserManager userManager;

    private UserLookupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserLookupServiceImpl();
        when(resolver.adaptTo(UserManager.class)).thenReturn(userManager);
    }

    @Test
    void listSeededUsersUsesUserManagerSearch() throws RepositoryException {
        Authorizable agent = mockUser(AGENT1, "Alex", "Agent", "agent1@example.com", "AGENT");
        Authorizable supervisor = mockUser(
                SUPERVISOR1, "Pat", "Supervisor", "supervisor1@example.com", "SUPERVISOR");
        when(userManager.findAuthorizables(eq("profile/role"), eq("AGENT"), eq(UserManager.SEARCH_TYPE_USER)))
                .thenReturn(List.of(agent).iterator());
        when(userManager.findAuthorizables(eq("profile/role"), eq("SUPERVISOR"), eq(UserManager.SEARCH_TYPE_USER)))
                .thenReturn(List.of(supervisor).iterator());

        List<User> users = service.listSeededUsers(resolver);

        assertEquals(2, users.size());
        assertEquals(AGENT1, users.get(0).getId());
        assertEquals("Alex Agent", users.get(0).getName());
        assertEquals("agent1@example.com", users.get(0).getEmail());
        assertEquals("AGENT", users.get(0).getRole());
        assertEquals(SUPERVISOR1, users.get(1).getId());
    }

    @Test
    void listSeededUsersIgnoresUsersOutsideSupportFolder() throws RepositoryException {
        Authorizable outsideUser = mock(Authorizable.class);
        when(outsideUser.isGroup()).thenReturn(false);
        when(outsideUser.getPath()).thenReturn("/home/users/other/agent1");
        when(userManager.findAuthorizables(eq("profile/role"), eq("AGENT"), eq(UserManager.SEARCH_TYPE_USER)))
                .thenReturn(List.of(outsideUser).iterator());
        when(userManager.findAuthorizables(eq("profile/role"), eq("SUPERVISOR"), eq(UserManager.SEARCH_TYPE_USER)))
                .thenReturn(List.<Authorizable>of().iterator());

        List<User> users = service.listSeededUsers(resolver);

        assertTrue(users.isEmpty());
    }

    private Authorizable mockUser(
            String path,
            String givenName,
            String familyName,
            String email,
            String role) throws RepositoryException {
        Value givenValue = mock(Value.class);
        Value familyValue = mock(Value.class);
        Value emailValue = mock(Value.class);
        Value roleValue = mock(Value.class);
        when(givenValue.getString()).thenReturn(givenName);
        when(familyValue.getString()).thenReturn(familyName);
        when(emailValue.getString()).thenReturn(email);
        when(roleValue.getString()).thenReturn(role);

        Authorizable authorizable = mock(Authorizable.class);
        when(authorizable.isGroup()).thenReturn(false);
        when(authorizable.getPath()).thenReturn(path);
        when(authorizable.getProperty("profile/givenName")).thenReturn(new Value[] { givenValue });
        when(authorizable.getProperty("profile/familyName")).thenReturn(new Value[] { familyValue });
        when(authorizable.getProperty("profile/email")).thenReturn(new Value[] { emailValue });
        when(authorizable.getProperty("profile/role")).thenReturn(new Value[] { roleValue });
        return authorizable;
    }
}
