package com.allensandiego.adm.config;

import com.allensandiego.adm.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Idempotent test data seeder.
 * Creates catalog permissions, system roles, and persona users for 'test' and 'dev' profiles.
 * Safe to run multiple times - uses upsert logic based on unique codes/names.
 */
@Component
@Profile({"test", "dev"})
public class TestDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(TestDataSeeder.class);

    // 13 Catalog Permissions
    private static final List<PermissionDef> PERMISSIONS = List.of(
            new PermissionDef("permission.view", "View Permissions"),
            new PermissionDef("permission.create", "Create Permissions"),
            new PermissionDef("permission.edit", "Edit Permissions"),
            new PermissionDef("role.view", "View Roles"),
            new PermissionDef("role.create", "Create Roles"),
            new PermissionDef("role.edit", "Edit Roles"),
            new PermissionDef("user.view", "View Users"),
            new PermissionDef("user.create", "Create Users"),
            new PermissionDef("user.edit", "Edit Users"),
            new PermissionDef("audit.view", "View Audit Logs"),
            new PermissionDef("admin.access", "Admin Panel Access"),
            new PermissionDef("report.view", "View Reports"),
            new PermissionDef("system.config", "System Configuration")
    );

    // Roles with their permission codes
    private static final Map<String, List<String>> ROLES = Map.of(
            "SUPER_ADMIN", PERMISSIONS.stream().map(PermissionDef::code).toList(),
            "REPORT_VIEWER", List.of("report.view", "user.view", "audit.view")
    );

    // Persona users
    private static final List<UserDef> USERS = List.of(
            new UserDef("e2e.admin", "E2E Admin User", true, List.of("SUPER_ADMIN")),
            new UserDef("e2e.viewer", "E2E Viewer User", true, List.of("REPORT_VIEWER")),
            new UserDef("e2e.inactive", "E2E Inactive User", false, List.of("REPORT_VIEWER"))
    );

    // Default password for all seeded users (test/dev only)
    private static final String DEFAULT_PASSWORD = "password123";

    @Bean
    ApplicationRunner testDataSeederRunner(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRoleRepository userRoleRepository
    ) {
        return args -> seed(permissionRepository, roleRepository, userRepository, rolePermissionRepository, userRoleRepository);
    }

    @Transactional
    protected void seed(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRoleRepository userRoleRepository) {

        log.info("Starting test data seeding...");

        // 1. Seed Permissions (upsert)
        Map<String, Permission> permissionMap = seedPermissions(permissionRepository);

        // 2. Seed Roles with permissions
        seedRoles(roleRepository, rolePermissionRepository, permissionMap);

        // 3. Seed Users with roles
        seedUsers(userRepository, userRoleRepository, roleRepository);

        log.info("Test data seeding completed successfully.");
    }

    private Map<String, Permission> seedPermissions(PermissionRepository repo) {
        Map<String, Permission> map = new java.util.HashMap<>();
        for (PermissionDef def : PERMISSIONS) {
            repo.findByCode(def.code()).ifPresentOrElse(
                    existing -> map.put(def.code(), existing),
                    () -> {
                        Permission p = new Permission();
                        p.setCode(def.code());
                        p.setLabel(def.label());
                        p.setActive(true);
                        p.setCreatedAt(LocalDateTime.now());
                        p.setUpdatedAt(LocalDateTime.now());
                        Permission saved = repo.save(p);
                        map.put(def.code(), saved);
                        log.debug("Created permission: {}", def.code());
                    }
            );
        }
        return map;
    }

    private void seedRoles(RoleRepository roleRepo, RolePermissionRepository rpRepo, Map<String, Permission> permissions) {
        for (Map.Entry<String, List<String>> entry : ROLES.entrySet()) {
            String roleName = entry.getKey();
            List<String> permCodes = entry.getValue();

            roleRepo.findByName(roleName).ifPresentOrElse(
                    existing -> log.debug("Role already exists: {}", roleName),
                    () -> {
                        Role role = new Role();
                        role.setName(roleName);
                        role.setDescription(roleName + " system role");
                        role.setProtected("SUPER_ADMIN".equals(roleName));
                        role.setCreatedAt(LocalDateTime.now());
                        role.setUpdatedAt(LocalDateTime.now());
                        Role saved = roleRepo.save(role);

                        // Assign permissions (idempotent - check if exists first)
                        for (String code : permCodes) {
                            Permission perm = permissions.get(code);
                            if (perm != null) {
                                boolean exists = rpRepo.existsByRoleIdAndPermissionId(saved.getId(), perm.getId());
                                if (!exists) {
                                    rpRepo.save(new RolePermission(saved, perm));
                                }
                            }
                        }
                        log.debug("Created role '{}' with {} permissions", roleName, permCodes.size());
                    }
            );
        }
    }

    private void seedUsers(UserRepository userRepo, UserRoleRepository urRepo, RoleRepository roleRepo) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        
        for (UserDef def : USERS) {
            userRepo.findByUsernameIgnoreCase(def.username()).ifPresentOrElse(
                    existing -> {
                        log.debug("Updating existing user: {}", def.username());
                        
                        // Update password hash to ensure it's using the correct encoder
                        existing.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
                        existing.setUpdatedAt(LocalDateTime.now());
                        userRepo.save(existing);
                        
                        // Remove all existing role assignments for this user
                        urRepo.deleteAll(urRepo.findByUserId(existing.getId()));
                        
                        // Assign new roles (idempotent)
                        for (String roleName : def.roleCodes()) {
                            roleRepo.findByName(roleName).ifPresent(role -> {
                                boolean exists = urRepo.existsByUserIdAndRoleId(existing.getId(), role.getId());
                                if (!exists) {
                                    urRepo.save(new UserRole(existing, role));
                                }
                            });
                        }
                        log.debug("Updated user '{}' with roles: {}", def.username(), def.roleCodes());
                    },
                    () -> {
                        User user = new User();
                        user.setUsername(def.username());
                        user.setDisplayName(def.displayName());
                        // Use BCrypt encoder to generate password hash
                        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
                        user.setStatus(def.active() ? User.UserStatus.ACTIVE : User.UserStatus.INACTIVE);
                        user.setCreatedAt(LocalDateTime.now());
                        user.setUpdatedAt(LocalDateTime.now());
                        User saved = userRepo.save(user);

                        // Assign roles (idempotent - check if exists first)
                        for (String roleName : def.roleCodes()) {
                            roleRepo.findByName(roleName).ifPresent(role -> {
                                boolean exists = urRepo.existsByUserIdAndRoleId(saved.getId(), role.getId());
                                if (!exists) {
                                    urRepo.save(new UserRole(saved, role));
                                }
                            });
                        }
                        log.debug("Created user '{}' with roles: {}", def.username(), def.roleCodes());
                    }
            );
        }
    }

    record PermissionDef(String code, String label) {}

    record UserDef(String username, String displayName, boolean active, List<String> roleCodes) {
    }
}
