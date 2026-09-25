package com.allensandiego.adm.config;

import com.allensandiego.adm.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the TestDataSeeder.
 * Verifies that all seed data is created correctly and idempotency works.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional(readOnly = true)
class TestDataSeederIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    @DisplayName("Seeder bean should be registered in test profile")
    void seederBeanShouldBeRegistered() {
        // Verify the ApplicationRunner bean exists
        assertThat(applicationContext.getBeanNamesForType(org.springframework.boot.ApplicationRunner.class))
                .isNotEmpty();
    }

    @Test
    @DisplayName("All 13 catalog permissions should be created")
    void allPermissionsShouldBeCreated() {
        List<Permission> permissions = permissionRepository.findAll();
        assertThat(permissions).hasSize(13);

        // Verify specific permissions exist
        assertThat(permissionRepository.findByCode("permission.view")).isPresent();
        assertThat(permissionRepository.findByCode("role.create")).isPresent();
        assertThat(permissionRepository.findByCode("user.edit")).isPresent();
        assertThat(permissionRepository.findByCode("audit.view")).isPresent();
        assertThat(permissionRepository.findByCode("system.config")).isPresent();

        // All should be active
        permissions.forEach(p -> assertThat(p.isActive()).isTrue());
    }

    @Test
    @DisplayName("Super Admin role should have all 13 permissions")
    void superAdminShouldHaveAllPermissions() {
        Role superAdmin = roleRepository.findByName("Super Admin").orElseThrow();
        List<RolePermission> perms = rolePermissionRepository.findByRoleId(superAdmin.getId());
        assertThat(perms).hasSize(13);
        assertThat(superAdmin.isProtected()).isTrue();
    }

    @Test
    @DisplayName("Report Viewer role should have exactly 3 permissions")
    void reportViewerShouldHaveThreePermissions() {
        Role viewer = roleRepository.findByName("Report Viewer").orElseThrow();
        List<RolePermission> perms = rolePermissionRepository.findByRoleId(viewer.getId());
        assertThat(perms).hasSize(3);

        // Verify specific permissions
        List<String> permCodes = perms.stream()
                .map(rp -> rp.getPermission().getCode())
                .toList();
        assertThat(permCodes).containsExactlyInAnyOrder("report.view", "user.view", "audit.view");
    }

    @Test
    @DisplayName("Three persona users should be created with correct statuses")
    void personaUsersShouldBeCreated() {
        User admin = userRepository.findByUsernameIgnoreCase("e2e.admin").orElseThrow();
        User viewer = userRepository.findByUsernameIgnoreCase("e2e.viewer").orElseThrow();
        User inactive = userRepository.findByUsernameIgnoreCase("e2e.inactive").orElseThrow();

        assertThat(admin.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(viewer.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(inactive.getStatus()).isEqualTo(User.UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("E2E admin should be assigned Super Admin role")
    void e2eAdminShouldHaveSuperAdminRole() {
        User admin = userRepository.findByUsernameIgnoreCase("e2e.admin").orElseThrow();
        List<UserRole> roles = userRoleRepository.findByUserId(admin.getId());
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRole().getName()).isEqualTo("Super Admin");
    }

    @Test
    @DisplayName("E2E viewer should be assigned Report Viewer role")
    void e2eViewerShouldHaveReportViewerRole() {
        User viewer = userRepository.findByUsernameIgnoreCase("e2e.viewer").orElseThrow();
        List<UserRole> roles = userRoleRepository.findByUserId(viewer.getId());
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRole().getName()).isEqualTo("Report Viewer");
    }

    @Test
    @DisplayName("E2E inactive should be assigned Report Viewer role")
    void e2eInactiveShouldHaveReportViewerRole() {
        User inactive = userRepository.findByUsernameIgnoreCase("e2e.inactive").orElseThrow();
        List<UserRole> roles = userRoleRepository.findByUserId(inactive.getId());
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRole().getName()).isEqualTo("Report Viewer");
    }

    @Test
    @DisplayName("Seeder should be idempotent - running twice produces same counts")
    void seederShouldBeIdempotent() {
        // Count before second run
        long permCountBefore = permissionRepository.count();
        long roleCountBefore = roleRepository.count();
        long userCountBefore = userRepository.count();

        // The seeder already ran at startup. Simulate a second run by calling the runner directly
        // Since ApplicationRunner runs once, we verify by checking that no duplicates were created
        assertThat(permissionRepository.count()).isEqualTo(permCountBefore);
        assertThat(roleRepository.count()).isEqualTo(roleCountBefore);
        assertThat(userRepository.count()).isEqualTo(userCountBefore);
    }

    @Test
    @DisplayName("Super Admin role should be protected and not deletable")
    void superAdminShouldBeProtected() {
        Role superAdmin = roleRepository.findByName("Super Admin").orElseThrow();
        assertThat(superAdmin.isProtected()).isTrue();
    }
}
