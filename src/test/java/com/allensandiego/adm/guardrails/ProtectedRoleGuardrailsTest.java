package com.allensandiego.adm.guardrails;

import com.allensandiego.adm.domain.*;
import com.allensandiego.adm.dto.UserRoleAssignmentRequest;
import com.allensandiego.adm.security.PermissionResolver;
import com.allensandiego.adm.security.Permissions;
import com.allensandiego.adm.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@WithMockUser(username = "admin1")
class ProtectedRoleGuardrailsTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private PermissionResolver permissionResolver;
    @Autowired private UserService userService;

    private Role adminRole;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // Delete in reverse order of foreign key dependencies to avoid referential integrity violations
        userRoleRepository.deleteAll();
        rolePermissionRepository.deleteAll();
        userRepository.deleteAll();
        permissionRepository.deleteAll();
        roleRepository.deleteAll();

        // Create permissions (idempotent - check if exists first)
        Permission viewPerm = permissionRepository.findByCode(Permissions.USER_VIEW).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.USER_VIEW);
            p.setLabel("View Users");
            return permissionRepository.save(p);
        });

        Permission createPerm = permissionRepository.findByCode(Permissions.USER_CREATE).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(Permissions.USER_CREATE);
            p.setLabel("Create Users");
            return permissionRepository.save(p);
        });

        // Create protected admin role (idempotent - check if exists first)
        Role existingAdminRole = roleRepository.findByName("ADMIN").orElse(null);
        if (existingAdminRole != null) {
            adminRole = existingAdminRole;
        } else {
            adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Administrator role");
            adminRole.setProtected(true);
            adminRole = roleRepository.save(adminRole);
        }

        // Assign permissions to admin role (idempotent - check if exists first)
        boolean hasView = rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getId(), viewPerm.getId());
        if (!hasView) {
            rolePermissionRepository.save(new RolePermission(adminRole, viewPerm));
        }
        
        boolean hasCreate = rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getId(), createPerm.getId());
        if (!hasCreate) {
            rolePermissionRepository.save(new RolePermission(adminRole, createPerm));
        }

        // Create two users with admin role (quota = 2)
        user1 = new User();
        user1.setUsername("admin1");
        user1.setDisplayName("Admin One");
        user1.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user1);

        user2 = new User();
        user2.setUsername("admin2");
        user2.setDisplayName("Admin Two");
        user2.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user2);

        userRoleRepository.save(new UserRole(user1, adminRole));
        userRoleRepository.save(new UserRole(user2, adminRole));
    }

    @Test
    @DisplayName("Cannot assign protected role when quota is reached")
    void cannotAssignProtectedRoleWhenQuotaReached() {
        // Create a third user
        User user3 = new User();
        user3.setUsername("admin3");
        user3.setDisplayName("Admin Three");
        user3.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user3);

        // Try to assign protected role - should fail
        assertThrows(IllegalStateException.class, () -> {
            userService.assignRole(
                    user3.getId(),
                    new UserRoleAssignmentRequest(adminRole.getId()),
                    "admin1"
            );
        });

        // Verify quota is still 2
        assertEquals(2, permissionResolver.countDistinctUsersWithProtectedRole());
    }

    @Test
    @DisplayName("Can assign protected role when quota allows")
    void canAssignProtectedRoleWhenQuotaAllows() {
        // Create a third user
        User user3 = new User();
        user3.setUsername("admin3");
        user3.setDisplayName("Admin Three");
        user3.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user3);

        // Deactivate one of the existing admin users to free up quota
        user1.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user1);

        // Now assign protected role to third user - should succeed
        userService.assignRole(
                user3.getId(),
                new UserRoleAssignmentRequest(adminRole.getId()),
                "admin2"
        );

        assertEquals(2, permissionResolver.countDistinctUsersWithProtectedRole());
    }

    @Test
    @DisplayName("Deactivated protected role users do not resolve permissions")
    void deactivatedProtectedRoleUsersDoNotResolvePermissions() {
        Set<String> perms = permissionResolver.resolvePermissions(user1.getId());
        assertFalse(perms.isEmpty()); // admin role has permissions assigned in setup

        user1.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user1);

        perms = permissionResolver.resolvePermissions(user1.getId());
        assertTrue(perms.isEmpty());
    }
}
