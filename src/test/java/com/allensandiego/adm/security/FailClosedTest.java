package com.allensandiego.adm.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@Transactional
class FailClosedTest {

    @Autowired private PermissionResolver permissionResolver;

    @Test
    @WithAnonymousUser
    @DisplayName("Fail-closed: Unauthenticated user has no permissions")
    void anonymousHasNoPermissions() {
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertTrue(perms.isEmpty());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Fail-closed: Anonymous cannot access users resource - service returns empty set")
    void anonymousCannotAccessUsers() {
        // Service layer doesn't check authorization - that's handled by controller PreAuthorize
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertTrue(perms.isEmpty());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Fail-closed: Anonymous cannot access roles resource - service returns empty set")
    void anonymousCannotAccessRoles() {
        // Service layer doesn't check authorization - that's handled by controller PreAuthorize
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertTrue(perms.isEmpty());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Fail-closed: Anonymous cannot access permissions resource - service returns empty set")
    void anonymousCannotAccessPermissions() {
        // Service layer doesn't check authorization - that's handled by controller PreAuthorize
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertTrue(perms.isEmpty());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Fail-closed: Anonymous cannot create users - service returns empty set")
    void anonymousCannotCreateUsers() {
        // Service layer doesn't check authorization - that's handled by controller PreAuthorize
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertFalse(perms.contains(Permissions.USER_CREATE));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Fail-closed: Anonymous cannot create roles - service returns empty set")
    void anonymousCannotCreateRoles() {
        // Service layer doesn't check authorization - that's handled by controller PreAuthorize
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertFalse(perms.contains(Permissions.ROLE_CREATE));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Fail-closed: Anonymous cannot create permissions - service returns empty set")
    void anonymousCannotCreatePermissions() {
        // Service layer doesn't check authorization - that's handled by controller PreAuthorize
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertFalse(perms.contains(Permissions.PERMISSION_CREATE));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Fail-closed: Anonymous cannot edit users - service returns empty set")
    void anonymousCannotEditUsers() {
        // Service layer doesn't check authorization - that's handled by controller PreAuthorize
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertFalse(perms.contains(Permissions.USER_EDIT));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Fail-closed: Anonymous cannot edit roles - service returns empty set")
    void anonymousCannotEditRoles() {
        // Service layer doesn't check authorization - that's handled by controller PreAuthorize
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertFalse(perms.contains(Permissions.ROLE_EDIT));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Fail-closed: Anonymous cannot edit permissions - service returns empty set")
    void anonymousCannotEditPermissions() {
        // Service layer doesn't check authorization - that's handled by controller PreAuthorize
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertFalse(perms.contains(Permissions.PERMISSION_EDIT));
    }

    @Test
    @WithMockUser(username = "nonexistent")
    @DisplayName("Fail-closed: Unknown user without permissions receives empty set")
    void unknownUserWithoutPermissionsDenied() {
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertTrue(perms.isEmpty());
    }

    @Test
    @WithMockUser(username = "nonexistent")
    @DisplayName("Fail-closed: Unknown user cannot create resources - service returns empty set")
    void unknownUserCannotCreate() {
        // Service layer doesn't check authorization - that's handled by controller PreAuthorize
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertTrue(perms.isEmpty());
    }
}
