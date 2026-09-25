package com.allensandiego.adm.security;

import com.allensandiego.adm.domain.*;
import com.allensandiego.adm.service.RoleService;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@Transactional
class RoleAuthorizationTest {

    @Autowired private RoleService roleService;
    @Autowired private PermissionResolver permissionResolver;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;

    private UUID adminUserId;
    private UUID editorUserId;
    private UUID adminRoleId;
    private UUID editorRoleId;
    private UUID roleViewPermId;
    private UUID roleCreatePermId;
    private UUID roleEditPermId;

    @BeforeEach
    void setUp() {
        // Delete in reverse order of foreign key dependencies to avoid referential integrity violations
        userRoleRepository.deleteAll();
        rolePermissionRepository.deleteAll();
        userRepository.deleteAll();
        permissionRepository.deleteAll();
        roleRepository.deleteAll();

        // Create permissions for roles (idempotent - check if exists first)
        Permission roleView = permissionRepository.findByCode(Permissions.ROLE_VIEW).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.ROLE_VIEW);
            p.setLabel("View Roles");
            return permissionRepository.save(p);
        });
        roleViewPermId = roleView.getId();

        Permission roleCreate = permissionRepository.findByCode(Permissions.ROLE_CREATE).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.ROLE_CREATE);
            p.setLabel("Create Roles");
            return permissionRepository.save(p);
        });
        roleCreatePermId = roleCreate.getId();

        Permission roleEdit = permissionRepository.findByCode(Permissions.ROLE_EDIT).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.ROLE_EDIT);
            p.setLabel("Edit Roles");
            return permissionRepository.save(p);
        });
        roleEditPermId = roleEdit.getId();

        // Create roles
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setDescription("Administrator role");
        adminRole = roleRepository.save(adminRole);
        adminRoleId = adminRole.getId();

        Role editorRole = new Role();
        editorRole.setName("EDITOR");
        editorRole.setDescription("Editor role");
        editorRole = roleRepository.save(editorRole);
        editorRoleId = editorRole.getId();

        // Assign permissions to roles
        rolePermissionRepository.save(new RolePermission(adminRole, roleView));
        rolePermissionRepository.save(new RolePermission(adminRole, roleCreate));
        rolePermissionRepository.save(new RolePermission(adminRole, roleEdit));
        rolePermissionRepository.save(new RolePermission(editorRole, roleView));

        // Create users
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setDisplayName("Admin User");
        adminUser.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(adminUser);
        adminUserId = adminUser.getId();

        User editorUser = new User();
        editorUser.setUsername("editor");
        editorUser.setDisplayName("Editor User");
        editorUser.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(editorUser);
        editorUserId = editorUser.getId();

        // Assign roles
        userRoleRepository.save(new UserRole(adminUser, adminRole));
        userRoleRepository.save(new UserRole(editorUser, editorRole));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Anonymous user has no permissions for roles resource")
    void anonymousHasNoRolePermissions() {
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertTrue(perms.isEmpty());
    }

    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    @DisplayName("Editor user resolves with role.view permission")
    void editorResolvesWithRoleView() {
        var perms = permissionResolver.resolvePermissions(editorUserId);
        assertTrue(perms.contains(Permissions.ROLE_VIEW));
        assertFalse(perms.contains(Permissions.ROLE_CREATE));
        assertFalse(perms.contains(Permissions.ROLE_EDIT));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin user resolves with all role permissions")
    void adminResolvesWithAllRolePermissions() {
        var perms = permissionResolver.resolvePermissions(adminUserId);
        assertTrue(perms.contains(Permissions.ROLE_VIEW));
        assertTrue(perms.contains(Permissions.ROLE_CREATE));
        assertTrue(perms.contains(Permissions.ROLE_EDIT));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Anonymous user cannot access roles endpoint")
    void anonymousCannotAccessRoles() {
        assertThrows(AuthorizationDeniedException.class, () -> {
            roleService.getRole(adminRoleId);
        });
    }

    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    @DisplayName("Editor can view roles but not create")
    void editorCanViewButNotCreateRoles() {
        var perms = permissionResolver.resolvePermissions(editorUserId);
        assertTrue(perms.contains(Permissions.ROLE_VIEW));
        assertFalse(perms.contains(Permissions.ROLE_CREATE));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin can create roles")
    void adminCanCreateRoles() {
        var perms = permissionResolver.resolvePermissions(adminUserId);
        assertTrue(perms.contains(Permissions.ROLE_CREATE));
    }

    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    @DisplayName("Editor cannot edit roles")
    void editorCannotEditRoles() {
        var perms = permissionResolver.resolvePermissions(editorUserId);
        assertFalse(perms.contains(Permissions.ROLE_EDIT));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin can edit roles")
    void adminCanEditRoles() {
        var perms = permissionResolver.resolvePermissions(adminUserId);
        assertTrue(perms.contains(Permissions.ROLE_EDIT));
    }

    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    @DisplayName("Editor cannot assign permissions to roles")
    void editorCannotAssignPermissions() {
        var perms = permissionResolver.resolvePermissions(editorUserId);
        assertFalse(perms.contains(Permissions.ROLE_EDIT));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin can assign permissions to roles")
    void adminCanAssignPermissions() {
        var perms = permissionResolver.resolvePermissions(adminUserId);
        assertTrue(perms.contains(Permissions.ROLE_EDIT));
    }
}
