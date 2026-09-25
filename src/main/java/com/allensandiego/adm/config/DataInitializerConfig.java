package com.allensandiego.adm.config;

import com.allensandiego.adm.domain.*;
import com.allensandiego.adm.security.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

@Configuration
@Profile("!test")
public class DataInitializerConfig {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerConfig.class);

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               RoleRepository roleRepository,
                               PermissionRepository permissionRepository,
                               UserRoleRepository userRoleRepository,
                               RolePermissionRepository rolePermissionRepository,
                               PasswordEncoder passwordEncoder,
                               Environment env) {
        return args -> {
            String adminPassword = env.getProperty("app.seed.admin-password", "admin123");

            // Create default permissions if not exist
            for (String code : Permissions.all()) {
                if (!permissionRepository.existsByCode(code)) {
                    Permission p = new Permission();
                    p.setCode(code);
                    p.setLabel(code.replace(".", " ").replace("create", "Create").replace("view", "View").replace("edit", "Edit").replace("activate", "Activate").replace("assign", "Assign"));
                    permissionRepository.save(p);
                    log.info("Created permission: {}", code);
                }
            }

            // Create default roles if not exist
            Role adminRole = null;
            Role editorRole = null;

            if (!roleRepository.existsByName("ADMIN")) {
                adminRole = new Role();
                adminRole.setName("ADMIN");
                adminRole.setDescription("System Administrator");
                adminRole.setProtected(true);
                adminRole = roleRepository.save(adminRole);
                log.info("Created role: ADMIN");
            } else {
                adminRole = roleRepository.findByName("ADMIN").orElse(null);
            }

            if (!roleRepository.existsByName("EDITOR")) {
                editorRole = new Role();
                editorRole.setName("EDITOR");
                editorRole.setDescription("Editor - can view users");
                editorRole = roleRepository.save(editorRole);
                log.info("Created role: EDITOR");
            } else {
                editorRole = roleRepository.findByName("EDITOR").orElse(null);
            }

            // Assign permissions to roles
            List<Permission> allPermissions = permissionRepository.findAll();
            if (adminRole != null) {
                for (Permission p : allPermissions) {
                    if (!rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getId(), p.getId())) {
                        rolePermissionRepository.save(new RolePermission(adminRole, p));
                    }
                }
            }

            // Assign view permission to editor role
            if (editorRole != null) {
                Permission userView = permissionRepository.findByCode(Permissions.USER_VIEW).orElse(null);
                if (userView != null && !rolePermissionRepository.existsByRoleIdAndPermissionId(editorRole.getId(), userView.getId())) {
                    rolePermissionRepository.save(new RolePermission(editorRole, userView));
                }
            }

            // Create admin user if not exist
            if (!userRepository.existsByUsername("admin")) {
                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setDisplayName("Administrator");
                adminUser.setStatus(User.UserStatus.ACTIVE);
                adminUser.setPasswordHash(passwordEncoder.encode(adminPassword));
                userRepository.save(adminUser);

                if (adminRole != null) {
                    userRoleRepository.save(new UserRole(adminUser, adminRole));
                }

                log.info("Created admin user with password: {}", adminPassword);
            }

            log.info("Data initialization complete.");
        };
    }
}
