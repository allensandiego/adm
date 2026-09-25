package com.allensandiego.adm.security;

import com.allensandiego.adm.domain.*;
import com.allensandiego.adm.service.RoleService;
import com.allensandiego.adm.service.UserService;
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
class UserAuthorizationTest {

    @Autowired private UserService userService;
    @Autowired private PermissionResolver permissionResolver;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;

    private UUID adminUserId;
    private UUID editorUserId;
    private UUID adminRoleId;
    private UUID editorRoleId;
    private UUID userViewPermId;
    private UUID userCreatePermId;
    private UUID userEditPermId;

    @BeforeEach
    void setUp() {
        // Delete in reverse order of foreign key dependencies to avoid referential integrity violations
        userRoleRepository.deleteAll();
        rolePermissionRepository.deleteAll();
        userRepository.deleteAll();
        permissionRepository.deleteAll();
        roleRepository.deleteAll();

        // Create permissions (idempotent - check if exists first)
        Permission userView = permissionRepository.findByCode(Permissions.USER_VIEW).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.USER_VIEW);
            p.setLabel("View Users");
            return permissionRepository.save(p);
        });
        userViewPermId = userView.getId();

        Permission userCreate = permissionRepository.findByCode(Permissions.USER_CREATE).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.USER_CREATE);
            p.setLabel("Create Users");
            return permissionRepository.save(p);
        });
        userCreatePermId = userCreate.getId();

        Permission userEdit = permissionRepository.findByCode(Permissions.USER_EDIT).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.USER_EDIT);
            p.setLabel("Edit Users");
            return permissionRepository.save(p);
        });
        userEditPermId = userEdit.getId();

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
        rolePermissionRepository.save(new RolePermission(adminRole, userView));
        rolePermissionRepository.save(new RolePermission(adminRole, userCreate));
        rolePermissionRepository.save(new RolePermission(adminRole, userEdit));
        rolePermissionRepository.save(new RolePermission(editorRole, userView));

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
    @DisplayName("Anonymous user has no permissions for users resource")
    void anonymousHasNoUserPermissions() {
        var perms = permissionResolver.resolvePermissions(UUID.randomUUID());
        assertTrue(perms.isEmpty());
    }

    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    @DisplayName("Editor user resolves with user.view permission")
    void editorResolvesWithUserView() {
        var perms = permissionResolver.resolvePermissions(editorUserId);
        assertTrue(perms.contains(Permissions.USER_VIEW));
        assertFalse(perms.contains(Permissions.USER_CREATE));
        assertFalse(perms.contains(Permissions.USER_EDIT));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin user resolves with all user permissions")
    void adminResolvesWithAllUserPermissions() {
        var perms = permissionResolver.resolvePermissions(adminUserId);
        assertTrue(perms.contains(Permissions.USER_VIEW));
        assertTrue(perms.contains(Permissions.USER_CREATE));
        assertTrue(perms.contains(Permissions.USER_EDIT));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Anonymous user cannot access users endpoint")
    void anonymousCannotAccessUsers() {
        assertThrows(AuthorizationDeniedException.class, () -> {
            userService.getUser(adminUserId);
        });
    }

    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    @DisplayName("Editor can view users but not create")
    void editorCanViewButNotCreate() {
        var perms = permissionResolver.resolvePermissions(editorUserId);
        assertTrue(perms.contains(Permissions.USER_VIEW));
        assertFalse(perms.contains(Permissions.USER_CREATE));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin can create users")
    void adminCanCreateUsers() {
        var perms = permissionResolver.resolvePermissions(adminUserId);
        assertTrue(perms.contains(Permissions.USER_CREATE));
    }

    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    @DisplayName("Editor cannot edit users")
    void editorCannotEditUsers() {
        var perms = permissionResolver.resolvePermissions(editorUserId);
        assertFalse(perms.contains(Permissions.USER_EDIT));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin can edit users")
    void adminCanEditUsers() {
        var perms = permissionResolver.resolvePermissions(adminUserId);
        assertTrue(perms.contains(Permissions.USER_EDIT));
    }

    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    @DisplayName("Editor cannot assign roles to users")
    void editorCannotAssignRoles() {
        var perms = permissionResolver.resolvePermissions(editorUserId);
        assertFalse(perms.contains(Permissions.USER_EDIT));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin can assign roles to users")
    void adminCanAssignRoles() {
        var perms = permissionResolver.resolvePermissions(adminUserId);
        assertTrue(perms.contains(Permissions.USER_EDIT));
    }
}
