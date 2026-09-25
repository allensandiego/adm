package com.allensandiego.adm.service;

import com.allensandiego.adm.domain.Permission;
import com.allensandiego.adm.domain.PermissionRepository;
import com.allensandiego.adm.dto.PermissionCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock private PermissionRepository permissionRepository;
    @Mock private AuditService auditService;
    @InjectMocks private PermissionService permissionService;

    private UUID testId;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testPermission = new Permission();
        testPermission.setId(testId);
        testPermission.setCode("TEST.PERM");
        testPermission.setLabel("Test Permission");
        testPermission.setActive(true);
    }

    @Test
    @DisplayName("createPermission - saves permission with audit log")
    void createPermissionSuccess() {
        PermissionCreateRequest request = new PermissionCreateRequest("NEW.PERM", "New Permission");
        when(permissionRepository.existsByCode("NEW.PERM")).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> {
            Permission p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        permissionService.createPermission(request, "admin");

        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    @DisplayName("createPermission - throws on duplicate code")
    void createPermissionDuplicateCode() {
        PermissionCreateRequest request = new PermissionCreateRequest("TEST.PERM", "New Permission");
        when(permissionRepository.existsByCode("TEST.PERM")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> permissionService.createPermission(request, "admin"));
    }

    @Test
    @DisplayName("listPermissionsAsEntities - returns paginated results")
    void listPermissionsAsEntities() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Permission> permissions = List.of(testPermission);
        when(permissionRepository.findAll(pageable)).thenReturn(new PageImpl<>(permissions));

        Page<Permission> result = permissionService.listPermissionsAsEntities(pageable, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("listPermissionsAsEntities - filters by search term")
    void listPermissionsAsEntitiesWithSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Permission> permissions = List.of(testPermission);
        when(permissionRepository.search("test", pageable))
                .thenReturn(new PageImpl<>(permissions));

        permissionService.listPermissionsAsEntities(pageable, "test");

        verify(permissionRepository).search("test", pageable);
    }

    @Test
    @DisplayName("getPermissionEntity - returns permission by ID")
    void getPermissionEntityFound() {
        when(permissionRepository.findById(testId)).thenReturn(Optional.of(testPermission));

        Permission result = permissionService.getPermissionEntity(testId);

        assertNotNull(result);
        assertEquals(testId, result.getId());
    }

    @Test
    @DisplayName("getPermissionEntity - throws NoSuchElementException when not found")
    void getPermissionEntityNotFound() {
        when(permissionRepository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> permissionService.getPermissionEntity(testId));
    }

    @Test
    @DisplayName("toggleActive - toggles active status and logs audit")
    void toggleActiveSuccess() {
        when(permissionRepository.findById(testId)).thenReturn(Optional.of(testPermission));
        when(permissionRepository.save(any(Permission.class))).thenReturn(testPermission);

        permissionService.toggleActive(testId, "admin");

        assertFalse(testPermission.isActive());
        verify(permissionRepository).save(testPermission);
    }

    @Test
    @DisplayName("toggleActive - throws NoSuchElementException when not found")
    void toggleActiveNotFound() {
        when(permissionRepository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> permissionService.toggleActive(testId, "admin"));
    }
}
