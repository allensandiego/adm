package com.allensandiego.adm.service;

import com.allensandiego.adm.domain.AuditAction;
import com.allensandiego.adm.domain.Permission;
import com.allensandiego.adm.domain.PermissionRepository;
import com.allensandiego.adm.web.form.PermissionForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Service tests for permission operations.
 * Tests blank code/label rejection, duplicate detection, and audit recording.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionServiceTests {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        // Reset all mocks before each test
        reset(permissionRepository);
        reset(auditService);
        
        // Initialize the service with mocks
        this.permissionService = new PermissionServiceImpl(permissionRepository, auditService);
        
        // Set up default mock behavior - return empty Optional for findById (allows tests that don't stub to pass)
        doReturn(java.util.Optional.empty()).when(permissionRepository).findById(any(UUID.class));
    }

    /**
     * Test that blank code is rejected.
     */
    @DisplayName("Rejects blank code")
    @Test
    void testCreatePermissionWithBlankCode() {
        PermissionForm form = new PermissionForm("", "New Permission", true);

        when(permissionRepository.existsByCode("")).thenReturn(true);

        assertThatThrownBy(() -> permissionService.createPermission(form, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");
    }

    /**
     * Test that blank label is rejected.
     */
    @DisplayName("Rejects blank label")
    @Test
    void testCreatePermissionWithBlankLabel() {
        PermissionForm form = new PermissionForm("newperm", "", true);

        when(permissionRepository.existsByCode("newperm")).thenReturn(true);

        assertThatThrownBy(() -> permissionService.createPermission(form, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");
    }

    /**
     * Test that code pattern is validated.
     */
    @DisplayName("Rejects invalid code pattern")
    @Test
    void testCreatePermissionWithInvalidCodePattern() {
        PermissionForm form = new PermissionForm("INVALID", "Invalid Code", true);

        when(permissionRepository.existsByCode("INVALID")).thenReturn(true);

        assertThatThrownBy(() -> permissionService.createPermission(form, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pattern");
    }

    /**
     * Test that code pattern allows valid format.
     */
    @DisplayName("Accepts valid code pattern")
    @Test
    void testCreatePermissionWithValidCodePattern() {
        PermissionForm form = new PermissionForm("permission.view", "View Permissions", true);

        when(permissionRepository.existsByCode("permission.view")).thenReturn(false);

        assertThatCode(() -> permissionService.createPermission(form, UUID.randomUUID())).doesNotThrowAnyException();
    }

    /**
     * Test that duplicate code is rejected.
     */
    @DisplayName("Rejects duplicate code")
    @Test
    void testCreatePermissionWithDuplicateCode() {
        Permission existingPermission = new Permission();
        existingPermission.setCode("permission.view");
        existingPermission.setLabel("View Permissions");

        when(permissionRepository.existsByCode("permission.view")).thenReturn(true);

        PermissionForm form = new PermissionForm("permission.view", "New Label", true);

        assertThatThrownBy(() -> permissionService.createPermission(form, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    /**
     * Test that deactivating a permission sets active=false.
     */
    @DisplayName("Deactivating sets active=false")
    @Test
    void testDeactivatePermission() {
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode("permission.view");
        permission.setLabel("View Permissions");
        permission.setActive(true);

        when(permissionRepository.findById(permission.getId())).thenReturn(java.util.Optional.of(permission));

        permissionService.deactivatePermission(permission.getId(), UUID.randomUUID());

        verify(permissionRepository).findById(permission.getId());
        verify(auditService).record(eq(any(UUID.class)), eq(AuditAction.UPDATE),
                eq("permission"), eq(permission.getId().toString()), anyString(), anyString());
    }

    /**
     * Test that activating a permission sets active=true.
     */
    @DisplayName("Activating sets active=true")
    @Test
    void testActivatePermission() {
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode("permission.view");
        permission.setLabel("View Permissions");
        permission.setActive(false);

        when(permissionRepository.findById(permission.getId())).thenReturn(java.util.Optional.of(permission));

        permissionService.activatePermission(permission.getId(), UUID.randomUUID());

        verify(permissionRepository).findById(permission.getId());
        verify(auditService).record(eq(any(UUID.class)), eq(AuditAction.UPDATE),
                eq("permission"), eq(permission.getId().toString()), anyString(), anyString());
    }

    /**
     * Test that listing permissions returns paged results.
     */
    @DisplayName("List permissions returns paged results")
    @Test
    void testListPermissions() {
        Permission permission1 = new Permission();
        permission1.setCode("permission.view");
        permission1.setLabel("View Permissions");

        Permission permission2 = new Permission();
        permission2.setCode("permission.create");
        permission2.setLabel("Create Permissions");

        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        Page<Permission> page = new PageImpl<>(List.of(permission1, permission2), pageable, 2);

        when(permissionRepository.findByCodeContainingIgnoreCaseOrLabelContainingIgnoreCase(
                        anyString(), anyString(), any())).thenAnswer(invocation -> page);

        Page<Permission> result = permissionService.listPermissions("permission", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(permissionRepository).findByCodeContainingIgnoreCaseOrLabelContainingIgnoreCase(
                "permission", null, eq(pageable));
    }

    /**
     * Test that getting a permission returns the entity.
     */
    @DisplayName("Get permission returns entity")
    @Test
    void testGetPermission() {
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode("permission.view");
        permission.setLabel("View Permissions");

        when(permissionRepository.findById(permission.getId())).thenReturn(java.util.Optional.of(permission));

        Permission result = permissionService.getPermission(permission.getId());

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("permission.view");
        verify(permissionRepository).findById(permission.getId());
    }

    /**
     * Test that updating a permission updates label and active flag.
     */
    @DisplayName("Update permission updates label and active")
    @Test
    void testUpdatePermission() {
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode("permission.view");
        permission.setLabel("Old Label");
        permission.setActive(true);

        UUID testId = permission.getId();
        doReturn(java.util.Optional.of(permission)).when(permissionRepository).findById(testId);
        doAnswer(invocation -> {
            Permission saved = invocation.getArgument(0);
            saved.setUpdatedAt(java.time.Instant.now());
            return saved;
        }).when(permissionRepository).save(any(Permission.class));

        Permission result = permissionService.updatePermission(
                testId, "New Label", false, UUID.randomUUID());

        assertThat(result.getLabel()).isEqualTo("New Label");
        assertThat(result.isActive()).isFalse();
    }

    /**
     * Test that code is immutable after creation (cannot be changed in update).
     */
    @DisplayName("Code is immutable - update does not change code")
    @Test
    void testUpdatePermissionDoesNotChangeCode() {
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode("permission.view");
        permission.setLabel("Old Label");

        UUID testId = permission.getId();
        doReturn(java.util.Optional.of(permission)).when(permissionRepository).findById(testId);
        doAnswer(invocation -> {
            Permission saved = invocation.getArgument(0);
            saved.setUpdatedAt(java.time.Instant.now());
            return saved;
        }).when(permissionRepository).save(any(Permission.class));

        Permission result = permissionService.updatePermission(
                testId, "New Label", true, UUID.randomUUID());

        assertThat(result.getCode()).isEqualTo("permission.view");
    }
}
