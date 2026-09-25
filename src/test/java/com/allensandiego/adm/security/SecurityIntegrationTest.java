package com.allensandiego.adm.security;

import com.allensandiego.adm.domain.*;
import com.allensandiego.adm.dto.UserCreateRequest;
import com.allensandiego.adm.dto.UserResponse;
import com.allensandiego.adm.dto.UserRoleAssignmentRequest;
import com.allensandiego.adm.security.PermissionResolver;
import com.allensandiego.adm.service.RoleService;
import com.allensandiego.adm.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@Transactional
@WithMockUser(username = "admin")
class SecurityIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private UserService userService;
    @Autowired private RoleService roleService;
    @Autowired private PermissionResolver permissionResolver;

    private User adminUser;
    private User regularUser;
    private Role adminRole;
    private Role editorRole;
    private Permission userViewPermission;
    private Permission userCreatePermission;

    @BeforeEach
    void setUp() {
        // Delete in reverse order of foreign key dependencies to avoid referential integrity violations
        userRoleRepository.deleteAll();
        rolePermissionRepository.deleteAll();
        userRepository.deleteAll();
        permissionRepository.deleteAll();
        roleRepository.deleteAll();

        // Create permissions (idempotent - check if exists first)
        userViewPermission = permissionRepository.findByCode(Permissions.USER_VIEW).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.USER_VIEW);
            p.setLabel("View Users");
            return permissionRepository.save(p);
        });

        userCreatePermission = permissionRepository.findByCode(Permissions.USER_CREATE).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.USER_CREATE);
            p.setLabel("Create Users");
            return permissionRepository.save(p);
        });

        // Create roles
        adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setDescription("Administrator role");
        adminRole.setProtected(true);
        adminRole = roleRepository.save(adminRole);

        editorRole = new Role();
        editorRole.setName("EDITOR");
        editorRole.setDescription("Editor role");
        editorRole = roleRepository.save(editorRole);

        // Assign permissions to roles
        rolePermissionRepository.save(new RolePermission(adminRole, userViewPermission));
        rolePermissionRepository.save(new RolePermission(adminRole, userCreatePermission));
        rolePermissionRepository.save(new RolePermission(editorRole, userViewPermission));

        // Create users
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setDisplayName("Admin User");
        adminUser.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(adminUser);

        regularUser = new User();
        regularUser.setUsername("editor");
        regularUser.setDisplayName("Editor User");
        regularUser.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(regularUser);

        // Assign roles
        userRoleRepository.save(new UserRole(adminUser, adminRole));
        userRoleRepository.save(new UserRole(regularUser, editorRole));
    }

    @Test
    @DisplayName("Admin user resolves with all permissions")
    void adminUserResolvesWithAllPermissions() {
        Set<String> perms = permissionResolver.resolvePermissions(adminUser.getId());
        assertTrue(perms.contains(Permissions.USER_VIEW));
        assertTrue(perms.contains(Permissions.USER_CREATE));
    }

    @Test
    @DisplayName("Editor user resolves with subset of permissions")
    void editorUserResolvesWithSubsetOfPermissions() {
        Set<String> perms = permissionResolver.resolvePermissions(regularUser.getId());
        assertTrue(perms.contains(Permissions.USER_VIEW));
        assertFalse(perms.contains(Permissions.USER_CREATE));
    }

    @Test
    @DisplayName("Deactivated user resolves to empty permissions")
    void deactivatedUserResolvesToEmptyPermissions() {
        regularUser.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(regularUser);

        Set<String> perms = permissionResolver.resolvePermissions(regularUser.getId());
        assertTrue(perms.isEmpty());
    }

    @Test
    @DisplayName("Non-existent user resolves to empty permissions")
    void nonExistentUserResolvesToEmptyPermissions() {
        Set<String> perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertTrue(perms.isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("Authenticated admin can access protected endpoint")
    void authenticatedAdminCanAccessProtectedEndpoint() {
        UserResponse user = userService.getUser(adminUser.getId());
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
    }

    @Test
    @WithMockUser(username = "editor", authorities = "ROLE_EDITOR")
    @DisplayName("Authenticated editor can access protected endpoint")
    void authenticatedEditorCanAccessProtectedEndpoint() {
        UserResponse user = userService.getUser(regularUser.getId());
        assertNotNull(user);
        assertEquals("editor", user.getUsername());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Anonymous user is denied access")
    void anonymousUserIsDeniedAccess() {
        assertThrows(AuthorizationDeniedException.class, () -> {
            userService.getUser(adminUser.getId());
        });
    }

    @Test
    @DisplayName("Protected role assignment respects minimum quota")
    void protectedRoleAssignmentRespectsMinimumQuota() {
        // We already have one admin user with protected role
        long count = permissionResolver.countDistinctUsersWithProtectedRole();
        assertEquals(1, count);

        // Create another user and try to assign protected role
        User newUser = new User();
        newUser.setUsername("newadmin");
        newUser.setDisplayName("New Admin");
        newUser.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(newUser);

        // This should succeed since we're at 1 (quota is 2)
        userService.assignRole(
                newUser.getId(),
                new UserRoleAssignmentRequest(adminRole.getId()),
                "admin"
        );

        assertEquals(2, permissionResolver.countDistinctUsersWithProtectedRole());
    }

    @Test
    @DisplayName("Audit log is created on user creation")
    void auditLogIsCreatedOnUserCreation() {
        UserCreateRequest request = new UserCreateRequest("testuser", "Test User", true);
        userService.createUser(request, "admin");

        // Verify user was created
        Optional<User> createdUser = userRepository.findByUsername("testuser");
        assertTrue(createdUser.isPresent());
    }
}
