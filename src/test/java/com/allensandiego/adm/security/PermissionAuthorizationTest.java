package com.allensandiego.adm.security;

import com.allensandiego.adm.domain.*;
import com.allensandiego.adm.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@Transactional
class PermissionAuthorizationTest {

    @Autowired private PermissionService permissionService;
    @Autowired private PermissionResolver permissionResolver;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private UserRepository userRepository;

    private UUID adminUserId;
    private UUID editorUserId;
    private UUID adminRoleId;
    private UUID editorRoleId;
    private UUID permissionViewPermId;
    private UUID permissionCreatePermId;
    private UUID permissionEditPermId;

    @BeforeEach
    void setUp() {
        // Delete in reverse order of foreign key dependencies to avoid referential integrity violations
        userRoleRepository.deleteAll();
        rolePermissionRepository.deleteAll();
        userRepository.deleteAll();
        permissionRepository.deleteAll();
        roleRepository.deleteAll();

        // Create permissions for the test (idempotent - check if exists first)
        Permission permView = permissionRepository.findByCode(Permissions.PERMISSION_VIEW).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.PERMISSION_VIEW);
            p.setLabel("View Permissions");
            return permissionRepository.save(p);
        });

        Permission permCreate = permissionRepository.findByCode(Permissions.PERMISSION_CREATE).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.PERMISSION_CREATE);
            p.setLabel("Create Permissions");
            return permissionRepository.save(p);
        });

        Permission permEdit = permissionRepository.findByCode(Permissions.PERMISSION_EDIT).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.PERMISSION_EDIT);
            p.setLabel("Edit Permissions");
            return permissionRepository.save(p);
        });

        // Create roles
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setDescription("Administrator role");
        adminRole = roleRepository.save(adminRole);

        Role editorRole = new Role();
        editorRole.setName("EDITOR");
        editorRole.setDescription("Editor role");
        editorRole = roleRepository.save(editorRole);

        // Assign permissions to roles (idempotent - check if exists first)
        boolean hasView = rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getId(), permView.getId());
        if (!hasView) {
            rolePermissionRepository.save(new RolePermission(adminRole, permView));
        }
        
        boolean hasCreate = rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getId(), permCreate.getId());
        if (!hasCreate) {
            rolePermissionRepository.save(new RolePermission(adminRole, permCreate));
        }
        
        boolean hasEdit = rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getId(), permEdit.getId());
        if (!hasEdit) {
            rolePermissionRepository.save(new RolePermission(adminRole, permEdit));
        }
        
        boolean editorHasView = rolePermissionRepository.existsByRoleIdAndPermissionId(editorRole.getId(), permView.getId());
        if (!editorHasView) {
            rolePermissionRepository.save(new RolePermission(editorRole, permView));
        }

        // Create users (needed for permission resolution)
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setDisplayName("Admin User");
        adminUser.setStatus(User.UserStatus.ACTIVE);
        adminUser = userRepository.save(adminUser);
        adminUserId = adminUser.getId();
        
        User editorUser = new User();
        editorUser.setUsername("editor");
        editorUser.setDisplayName("Editor User");
        editorUser.setStatus(User.UserStatus.ACTIVE);
        editorUser = userRepository.save(editorUser);
        editorUserId = editorUser.getId();

        // Assign roles to users
        userRoleRepository.save(new UserRole(adminUser, adminRole));
        userRoleRepository.save(new UserRole(editorUser, editorRole));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Anonymous user has no permissions for permissions resource")
    void anonymousHasNoPermissionPermissions() {
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertTrue(perms.isEmpty());
    }

    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    @DisplayName("Editor user resolves with permission.view permission")
    void editorResolvesWithPermissionView() {
        var perms = permissionResolver.resolvePermissions(editorUserId);
        assertTrue(perms.contains(Permissions.PERMISSION_VIEW));
        assertFalse(perms.contains(Permissions.PERMISSION_CREATE));
        assertFalse(perms.contains(Permissions.PERMISSION_EDIT));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin user resolves with all permission permissions")
    void adminResolvesWithAllPermissionPermissions() {
        var perms = permissionResolver.resolvePermissions(adminUserId);
        assertTrue(perms.contains(Permissions.PERMISSION_VIEW));
        assertTrue(perms.contains(Permissions.PERMISSION_CREATE));
        assertTrue(perms.contains(Permissions.PERMISSION_EDIT));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Anonymous user cannot access permissions endpoint")
    void anonymousCannotAccessPermissions() {
        // Service layer doesn't check authorization - that's handled by controller PreAuthorize
        assertThrows(NoSuchElementException.class, () -> {
            permissionService.getPermission(UUID.randomUUID());
        });
    }

    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    @DisplayName("Editor can view permissions but not create")
    void editorCanViewButNotCreatePermissions() {
        var perms = permissionResolver.resolvePermissions(editorUserId);
        assertTrue(perms.contains(Permissions.PERMISSION_VIEW));
        assertFalse(perms.contains(Permissions.PERMISSION_CREATE));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin can create permissions")
    void adminCanCreatePermissions() {
        var perms = permissionResolver.resolvePermissions(adminUserId);
        assertTrue(perms.contains(Permissions.PERMISSION_CREATE));
    }

    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    @DisplayName("Editor cannot edit permissions")
    void editorCannotEditPermissions() {
        var perms = permissionResolver.resolvePermissions(editorUserId);
        assertFalse(perms.contains(Permissions.PERMISSION_EDIT));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin can edit permissions")
    void adminCanEditPermissions() {
        var perms = permissionResolver.resolvePermissions(adminUserId);
        assertTrue(perms.contains(Permissions.PERMISSION_EDIT));
    }
}
