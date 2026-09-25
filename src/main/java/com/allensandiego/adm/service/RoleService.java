package com.allensandiego.adm.service;

import com.allensandiego.adm.domain.*;
import com.allensandiego.adm.dto.RoleCreateRequest;
import com.allensandiego.adm.dto.RoleResponse;
import com.allensandiego.adm.security.PermissionResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionResolver permissionResolver;
    private final AuditService auditService;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       RolePermissionRepository rolePermissionRepository,
                       UserRoleRepository userRoleRepository,
                       PermissionResolver permissionResolver,
                       AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.permissionResolver = permissionResolver;
        this.auditService = auditService;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Page<RoleResponse> listRoles(Pageable pageable, String search) {
        Page<Role> roles;
        if (search != null && !search.isBlank()) {
            roles = roleRepository.search(search.trim(), pageable);
        } else {
            roles = roleRepository.findAll(pageable);
        }
        return roles.map(this::toResponse);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Page<Role> listRolesAsEntities(Pageable pageable, String search) {
        if (search != null && !search.isBlank()) {
            return roleRepository.search(search.trim(), pageable);
        }
        return roleRepository.findAll(pageable);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public RoleResponse getRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
        return toResponse(role);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Role getRoleEntity(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public RoleResponse createRole(RoleCreateRequest request, String actorUsername) {
        if (roleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Role already exists: " + request.getName());
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        Role saved = roleRepository.save(role);
        auditService.log(actorUsername, "ROLE_CREATE", "Role", saved.getId());
        return toResponse(saved);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void assignPermission(UUID roleId, UUID permissionId, String actorUsername) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NoSuchElementException("Permission not found: " + permissionId));

        if (role.isProtected()) {
            throw new IllegalStateException("Cannot modify permissions of protected role");
        }

        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            rolePermissionRepository.save(new RolePermission(role, permission));
        }

        auditService.log(actorUsername, "ROLE_PERMISSION_ASSIGN", "Role", roleId);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void removePermission(UUID roleId, UUID permissionId, String actorUsername) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));

        if (role.isProtected()) {
            throw new IllegalStateException("Cannot modify permissions of protected role");
        }

        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        auditService.log(actorUsername, "ROLE_PERMISSION_REMOVE", "Role", roleId);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void assignPermissions(UUID roleId, List<String> permissionCodes, String actorUsername) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));

        if (role.isProtected()) {
            throw new IllegalStateException("Cannot modify permissions of protected role");
        }

        // Clear existing permissions
        rolePermissionRepository.deleteByRoleId(roleId);

        // Add new permissions
        for (String code : permissionCodes) {
            Permission permission = permissionRepository.findByCode(code)
                    .orElseThrow(() -> new NoSuchElementException("Permission not found: " + code));
            rolePermissionRepository.save(new RolePermission(role, permission));
        }

        auditService.log(actorUsername, "ROLE_PERMISSIONS_ASSIGN", "Role", roleId);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<String> getRolePermissions(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
        return rolePermissionRepository.findActivePermissionCodesByRoleId(roleId);
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isProtected(),
                role.getVersion(),
                role.getCreatedAt().toString(),
                role.getUpdatedAt().toString()
        );
    }
}
