package com.allensandiego.adm.security;

import com.allensandiego.adm.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionResolver {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionResolver(UserRepository userRepository,
                              UserRoleRepository userRoleRepository,
                              RolePermissionRepository rolePermissionRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Resolves the effective permission set for a user.
     * Formula: permissions_of(user) = union of p where u_roles(role) and role_permissions(p, role) and p.active
     * Deactivated users resolve to an empty set.
     */
    @Transactional(readOnly = true)
    public Set<String> resolvePermissions(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        System.out.println("=== PermissionResolver.resolvePermissions ===");
        System.out.println("userId: " + userId);
        System.out.println("user: " + (user == null ? "null" : user.getUsername() + ", status: " + user.getStatus()));
        if (user == null || user.getStatus() != User.UserStatus.ACTIVE) {
            System.out.println("Returning empty set (user null or inactive)");
            return Collections.emptySet();
        }

        List<UserRole> userRoles = userRoleRepository.findByUserIdWithRoles(userId);
        System.out.println("userRoles: " + userRoles.size());
        Set<String> permissions = new HashSet<>();
        for (UserRole ur : userRoles) {
            if (ur.getRole() != null && ur.getRole().getId() != null) {
                List<String> codes = rolePermissionRepository
                        .findActivePermissionCodesByRoleId(ur.getRole().getId());
                System.out.println("role: " + ur.getRole().getName() + ", codes: " + codes);
                permissions.addAll(codes);
            }
        }
        System.out.println("Final permissions: " + permissions);
        return permissions;
    }

    @Transactional(readOnly = true)
    public Set<String> resolvePermissions(String username) {
        return userRepository.findByUsername(username)
                .map(user -> resolvePermissions(user.getId()))
                .orElse(Collections.emptySet());
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, String permissionCode) {
        return resolvePermissions(userId).contains(permissionCode);
    }

    @Transactional(readOnly = true)
    public Set<UUID> findUsersWithProtectedRole() {
        return userRoleRepository.findAll().stream()
                .filter(ur -> ur.getRole() != null && ur.getRole().isProtected())
                .map(ur -> ur.getUser().getId())
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public long countDistinctUsersWithProtectedRole() {
        return userRoleRepository.countDistinctUsersWithProtectedRole();
    }
}
