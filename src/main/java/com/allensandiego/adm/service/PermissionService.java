package com.allensandiego.adm.service;

import com.allensandiego.adm.domain.Permission;
import com.allensandiego.adm.domain.PermissionRepository;
import com.allensandiego.adm.dto.PermissionCreateRequest;
import com.allensandiego.adm.dto.PermissionResponse;
import com.allensandiego.adm.security.Permissions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public PermissionService(PermissionRepository permissionRepository,
                              AuditService auditService) {
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<PermissionResponse> listPermissions(Pageable pageable, String search) {
        Page<Permission> permissions;
        if (search != null && !search.isBlank()) {
            permissions = permissionRepository.search(search.trim(), pageable);
        } else {
            permissions = permissionRepository.findAll(pageable);
        }
        return permissions.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<Permission> listPermissionsAsEntities(Pageable pageable, String search) {
        if (search != null && !search.isBlank()) {
            return permissionRepository.search(search.trim(), pageable);
        }
        return permissionRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listActivePermissions() {
        return permissionRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissionResponse getPermission(UUID permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NoSuchElementException("Permission not found: " + permissionId));
        return toResponse(permission);
    }

    @Transactional(readOnly = true)
    public Permission getPermissionEntity(UUID permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NoSuchElementException("Permission not found: " + permissionId));
    }

    @Transactional
    public PermissionResponse createPermission(PermissionCreateRequest request, String actorUsername) {
        if (permissionRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Permission already exists: " + request.getCode());
        }

        Permission permission = new Permission();
        permission.setCode(request.getCode());
        permission.setLabel(request.getLabel());

        Permission saved = permissionRepository.save(permission);
        auditService.log(actorUsername, "PERMISSION_CREATE", "Permission", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void toggleActive(UUID permissionId, String actorUsername) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NoSuchElementException("Permission not found: " + permissionId));

        permission.setActive(!permission.isActive());
        Permission saved = permissionRepository.save(permission);
        auditService.log(actorUsername, "PERMISSION_TOGGLE", "Permission", saved.getId());
    }

    @Transactional
    public void updateLabel(UUID permissionId, String newLabel, String actorUsername) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NoSuchElementException("Permission not found: " + permissionId));
        permission.setLabel(newLabel.trim());
        Permission saved = permissionRepository.save(permission);
        auditService.log(actorUsername, "PERMISSION_EDIT", "Permission", saved.getId());
    }

    private PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getLabel(),
                permission.isActive(),
                permission.getCreatedAt().toString(),
                permission.getUpdatedAt().toString()
        );
    }
}
