package com.allensandiego.adm.service;

import com.allensandiego.adm.domain.*;
import com.allensandiego.adm.dto.UserCreateRequest;
import com.allensandiego.adm.dto.UserResponse;
import com.allensandiego.adm.dto.UserRoleAssignmentRequest;
import com.allensandiego.adm.security.Permissions;
import com.allensandiego.adm.security.PermissionResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionResolver permissionResolver;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       UserRoleRepository userRoleRepository,
                       RoleRepository roleRepository,
                       PermissionResolver permissionResolver,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.permissionResolver = permissionResolver;
        this.auditService = auditService;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable, String search) {
        Page<User> users;
        if (search != null && !search.isBlank()) {
            users = userRepository.search(search.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(this::toResponse);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Page<User> listUsersAsEntities(Pageable pageable, String search) {
        if (search != null && !search.isBlank()) {
            return userRepository.search(search.trim(), pageable);
        }
        return userRepository.findAll(pageable);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        return toResponse(user);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public User getUserEntity(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public UserResponse createUser(UserCreateRequest request, String actorUsername) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setDisplayName(request.getDisplayName());
        user.setStatus(request.isActivate() ? User.UserStatus.ACTIVE : User.UserStatus.INACTIVE);

        User saved = userRepository.save(user);
        auditService.log(actorUsername, "USER_CREATE", "User", saved.getId());
        return toResponse(saved);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public UserResponse activateUser(UUID userId, String actorUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        if (user.getStatus() == User.UserStatus.ACTIVE) {
            return toResponse(user);
        }

        user.setStatus(User.UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        auditService.log(actorUsername, "USER_ACTIVATE", "User", saved.getId());
        return toResponse(saved);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void assignRole(UUID userId, UserRoleAssignmentRequest request, String actorUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + request.getRoleId()));

        if (role.isProtected() && permissionResolver.countDistinctUsersWithProtectedRole() >= 2) {
            throw new IllegalStateException("Cannot assign protected role: minimum quota reached");
        }

        userRoleRepository.findByUserId(userId).stream()
                .filter(ur -> ur.getRole().getId().equals(request.getRoleId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {},
                        () -> userRoleRepository.save(new UserRole(user, role))
                );

        auditService.log(actorUsername, "USER_ROLE_ASSIGN", "User", userId);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void assignRoles(UUID userId, List<String> roleNames, String actorUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        // Clear existing roles
        userRoleRepository.deleteByUserId(userId);

        // Add new roles
        for (String name : roleNames) {
            Role role = roleRepository.findByName(name)
                    .orElseThrow(() -> new NoSuchElementException("Role not found: " + name));

            if (role.isProtected() && permissionResolver.countDistinctUsersWithProtectedRole() >= 2) {
                throw new IllegalStateException("Cannot assign protected role: minimum quota reached");
            }

            userRoleRepository.save(new UserRole(user, role));
        }

        auditService.log(actorUsername, "USER_ROLES_ASSIGN", "User", userId);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Set<String> getUserPermissions(UUID userId) {
        return permissionResolver.resolvePermissions(userId);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void updateProfile(UUID userId, String username, String actorUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        // For now, just log the edit - actual profile updates would require more fields
        auditService.log(actorUsername, "USER_EDIT", "User", userId);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void deleteUser(UUID userId, String actorUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        userRoleRepository.deleteByUserId(userId);
        userRepository.delete(user);
        auditService.log(actorUsername, "USER_DELETE", "User", userId);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void assignRolesByCode(String identifier, List<String> roleNames, String actorUsername) {
        User user;
        try {
            UUID userId = UUID.fromString(identifier);
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + identifier));
        } catch (IllegalArgumentException e) {
            user = userRepository.findByUsernameIgnoreCase(identifier)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + identifier));
        }

        // Clear existing roles
        userRoleRepository.deleteByUserId(user.getId());

        // Add new roles
        for (String name : roleNames) {
            Role role = roleRepository.findByName(name)
                    .orElseThrow(() -> new NoSuchElementException("Role not found: " + name));

            if (role.isProtected() && permissionResolver.countDistinctUsersWithProtectedRole() >= 2) {
                throw new IllegalStateException("Cannot assign protected role: minimum quota reached");
            }

            userRoleRepository.save(new UserRole(user, role));
        }

        auditService.log(actorUsername, "USER_ROLES_ASSIGN", "User", user.getId());
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getStatus().name(),
                user.getVersion(),
                user.getCreatedAt().toString(),
                user.getUpdatedAt().toString()
        );
    }
}
