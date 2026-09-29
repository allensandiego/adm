package com.allensandiego.adm.web;

import com.allensandiego.adm.domain.Permission;
import com.allensandiego.adm.service.EffectivePermissionService;
import com.allensandiego.adm.service.PermissionService;
import com.allensandiego.adm.web.form.PermissionForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for permission controller endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class PermissionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PermissionService permissionService;

    @MockBean
    private EffectivePermissionService effectivePermissionService;

    private UUID testPermissionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private Permission testPermission;
    private PermissionForm validForm;

    @BeforeEach
    void setUp() {
        testPermission = new Permission();
        testPermission.setId(testPermissionId);
        testPermission.setCode("permission.view");
        testPermission.setLabel("View Permissions");
        testPermission.setActive(true);

        validForm = new PermissionForm("newperm", "New Permission", true);

        // Setup mock responses
        Page<Permission> emptyPage = new PageImpl<>(List.of());
        when(permissionService.listPermissions(anyString(), any())).thenReturn(emptyPage);
        when(permissionService.getPermission(testPermissionId)).thenReturn(testPermission);
        when(permissionService.createPermission(any(), any())).thenReturn(testPermission);
        when(permissionService.updatePermission(eq(testPermissionId), anyString(), anyBoolean(), any()))
                .thenReturn(testPermission);
    }

    /**
     * Test GET /permissions renders list view.
     */
    @DisplayName("GET /permissions renders list view")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Test
    void testGetPermissionsList() throws Exception {
        mockMvc.perform(get("/permissions"))
                .andExpect(status().isOk())
                .andExpect(view().name("permissions/list"));
    }

    /**
     * Test GET /permissions/new renders form view.
     */
    @DisplayName("GET /permissions/new renders form view")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Test
    void testGetNewForm() throws Exception {
        mockMvc.perform(get("/permissions/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("permissions/form"));
    }

    /**
     * Test POST /permissions with valid data creates and redirects.
     */
    @DisplayName("POST /permissions creates permission and redirects")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Test
    void testCreatePermissionValid() throws Exception {
        mockMvc.perform(post("/permissions")
                        .with(csrf())
                        .contentType("application/x-www-form-urlencoded")
                        .param("code", validForm.code())
                        .param("label", validForm.label())
                        .param("active", String.valueOf(validForm.active())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/permissions/" + testPermissionId));

        verify(permissionService).createPermission(any(), any());
    }

    /**
     * Test POST /permissions with invalid code re-renders form.
     */
    @DisplayName("POST /permissions with invalid code re-renders form")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Test
    void testCreatePermissionInvalidCode() throws Exception {
        mockMvc.perform(post("/permissions")
                        .with(csrf())
                        .contentType("application/x-www-form-urlencoded")
                        .param("code", "")
                        .param("label", validForm.label())
                        .param("active", String.valueOf(validForm.active())))
                .andExpect(status().is4xxClientError())
                .andExpect(view().name("permissions/form"));
    }

    /**
     * Test GET /permissions/{id} renders detail view.
     */
    @DisplayName("GET /permissions/{id} renders detail view")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Test
    void testGetPermissionDetail() throws Exception {
        mockMvc.perform(get("/permissions/" + testPermissionId))
                .andExpect(status().isOk())
                .andExpect(view().name("permissions/detail"));
    }

    /**
     * Test GET /permissions/{id} returns 404 for unknown ID.
     */
    @DisplayName("GET /permissions/{id} returns 404 for unknown id")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Test
    void testGetPermissionNotFound() throws Exception {
        mockMvc.perform(get("/permissions/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    /**
     * Test POST /permissions/{id}/edit with valid data updates and redirects.
     */
    @DisplayName("POST /permissions/{id}/edit updates permission and redirects")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Test
    void testEditPermissionValid() throws Exception {
        mockMvc.perform(post("/permissions/" + testPermissionId + "/edit")
                        .with(csrf())
                        .contentType("application/x-www-form-urlencoded")
                        .param("label", "Updated Label")
                        .param("active", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/permissions/" + testPermissionId));

        verify(permissionService).updatePermission(eq(testPermissionId), eq("Updated Label"), eq(false), any());
    }

    /**
     * Test POST /permissions/{id}/edit with invalid data re-renders form.
     */
    @DisplayName("POST /permissions/{id}/edit with invalid data re-renders form")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Test
    void testEditPermissionInvalid() throws Exception {
        mockMvc.perform(post("/permissions/" + testPermissionId + "/edit")
                        .with(csrf())
                        .contentType("application/x-www-form-urlencoded")
                        .param("label", "")
                        .param("active", "true"))
                .andExpect(status().is4xxClientError())
                .andExpect(view().name("permissions/form"));
    }

    /**
     * Test that code is immutable after creation.
     */
    @DisplayName("Code is immutable - update does not change code")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Test
    void testUpdatePermissionDoesNotChangeCode() throws Exception {
        mockMvc.perform(post("/permissions/" + testPermissionId + "/edit")
                        .with(csrf())
                        .contentType("application/x-www-form-urlencoded")
                        .param("label", "Updated Label")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection());

        verify(permissionService).updatePermission(eq(testPermissionId), eq("Updated Label"), anyBoolean(), any());
    }
}
