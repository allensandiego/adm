package com.allensandiego.adm.service;

import com.allensandiego.adm.domain.Permission;
import com.allensandiego.adm.domain.PermissionRepository;
import com.allensandiego.adm.domain.Role;
import com.allensandiego.adm.domain.RolePermission;
import com.allensandiego.adm.domain.RolePermissionId;
import com.allensandiego.adm.domain.RolePermissionRepository;
import com.allensandiego.adm.domain.RoleRepository;
import com.allensandiego.adm.dto.RoleCreateRequest;
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
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private AuditService auditService;
    @InjectMocks private RoleService roleService;

    private UUID testId;
    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testRole = new Role();
        testRole.setId(testId);
        testRole.setName("TEST_ROLE");
        testRole.setDescription("Test Role");
        testRole.setProtected(false);

        testPermission = new Permission();
        testPermission.setId(UUID.randomUUID());
        testPermission.setCode("TEST.PERM");
    }

    @Test
    @DisplayName("createRole - saves role with audit log")
    void createRoleSuccess() {
        RoleCreateRequest request = new RoleCreateRequest("NEW_ROLE", "New Role");
        when(roleRepository.existsByName("NEW_ROLE")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        roleService.createRole(request, "admin");

        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("createRole - throws on duplicate name")
    void createRoleDuplicateName() {
        RoleCreateRequest request = new RoleCreateRequest("TEST_ROLE", "New Role");
        when(roleRepository.existsByName("TEST_ROLE")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> roleService.createRole(request, "admin"));
    }

    @Test
    @DisplayName("listRolesAsEntities - returns paginated results")
    void listRolesAsEntities() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Role> roles = List.of(testRole);
        when(roleRepository.findAll(pageable)).thenReturn(new PageImpl<>(roles));

        Page<Role> result = roleService.listRolesAsEntities(pageable, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("listRolesAsEntities - filters by search term")
    void listRolesAsEntitiesWithSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Role> roles = List.of(testRole);
        when(roleRepository.search("test", pageable))
                .thenReturn(new PageImpl<>(roles));

        roleService.listRolesAsEntities(pageable, "test");

        verify(roleRepository).search("test", pageable);
    }

    @Test
    @DisplayName("getRoleEntity - returns role by ID")
    void getRoleEntityFound() {
        when(roleRepository.findById(testId)).thenReturn(Optional.of(testRole));

        Role result = roleService.getRoleEntity(testId);

        assertNotNull(result);
        assertEquals(testId, result.getId());
    }

    @Test
    @DisplayName("getRoleEntity - throws NoSuchElementException when not found")
    void getRoleEntityNotFound() {
        when(roleRepository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> roleService.getRoleEntity(testId));
    }

    @Test
    @DisplayName("assignPermissions - assigns permissions to role")
    void assignPermissionsSuccess() {
        when(roleRepository.findById(testId)).thenReturn(Optional.of(testRole));
        when(permissionRepository.findByCode("TEST.PERM")).thenReturn(Optional.of(testPermission));
        roleService.assignPermissions(testId, List.of("TEST.PERM"), "admin");

        verify(rolePermissionRepository).save(any(RolePermission.class));
    }

    @Test
    @DisplayName("assignPermissions - throws NoSuchElementException when role not found")
    void assignPermissionsRoleNotFound() {
        when(roleRepository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> roleService.assignPermissions(testId, List.of("TEST.PERM"), "admin"));
    }

    @Test
    @DisplayName("assignPermissions - clears existing permissions and assigns new ones")
    void assignPermissionsDuplicateSkipped() {
        when(roleRepository.findById(testId)).thenReturn(Optional.of(testRole));
        when(permissionRepository.findByCode("TEST.PERM")).thenReturn(Optional.of(testPermission));

        roleService.assignPermissions(testId, List.of("TEST.PERM"), "admin");

        verify(rolePermissionRepository).deleteByRoleId(testId);
        verify(rolePermissionRepository).save(any(RolePermission.class));
    }
}
