package com.allensandiego.adm.security;

import com.allensandiego.adm.config.DataSeeder;
import com.allensandiego.adm.domain.Permission;
import com.allensandiego.adm.domain.Role;
import com.allensandiego.adm.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dual-sided authorization tests for permission endpoints.
 * Tests that authorized users get 200 (or redirect) and unauthorized users get 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(DataSeeder.class)
@EnableMethodSecurity
public class PermissionAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test authorization for /permissions GET endpoint.
     */
    @ParameterizedTest
    @CsvSource({
        "permission.view,true,200",
        "permission.create,true,200",
        "user.view,false,403",
        "role.view,false,403"
    })
    @DisplayName("GET /permissions: dual-sided authorization test")
    void testGetPermissionsAuthorization(String permissionCode, boolean hasPermission, int expectedStatus) throws Exception {
        mockMvc.perform(get("/permissions"))
                .andExpect(status().is(expectedStatus));
    }

    /**
     * Test authorization for /permissions/new GET endpoint.
     */
    @ParameterizedTest
    @CsvSource({
        "permission.view,true,200",
        "permission.create,true,200",
        "user.view,false,403",
        "role.view,false,403"
    })
    @DisplayName("GET /permissions/new: dual-sided authorization test")
    void testGetNewFormAuthorization(String permissionCode, boolean hasPermission, int expectedStatus) throws Exception {
        mockMvc.perform(get("/permissions/new"))
                .andExpect(status().is(expectedStatus));
    }

    /**
     * Test authorization for POST /permissions endpoint.
     */
    @ParameterizedTest
    @CsvSource({
        "permission.view,true,302",
        "permission.create,true,302",
        "user.view,false,403",
        "role.view,false,403"
    })
    @DisplayName("POST /permissions: dual-sided authorization test")
    void testPostPermissionsAuthorization(String permissionCode, boolean hasPermission, int expectedStatus) throws Exception {
        mockMvc.perform(post("/permissions"))
                .andExpect(status().is(expectedStatus));
    }

    /**
     * Test authorization for GET /permissions/{id} endpoint.
     */
    @ParameterizedTest
    @CsvSource({
        "permission.view,true,200",
        "permission.create,false,403",
        "user.view,false,403",
        "role.view,false,403"
    })
    @DisplayName("GET /permissions/{id}: dual-sided authorization test")
    void testGetPermissionDetailAuthorization(String permissionCode, boolean hasPermission, int expectedStatus) throws Exception {
        mockMvc.perform(get("/permissions/550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().is(expectedStatus));
    }

    /**
     * Test authorization for POST /permissions/{id}/edit endpoint.
     */
    @ParameterizedTest
    @CsvSource({
        "permission.view,false,403",
        "permission.edit,true,302",
        "user.view,false,403",
        "role.view,false,403"
    })
    @DisplayName("POST /permissions/{id}/edit: dual-sided authorization test")
    void testEditPermissionAuthorization(String permissionCode, boolean hasPermission, int expectedStatus) throws Exception {
        mockMvc.perform(post("/permissions/550e8400-e29b-41d4-a716-446655440000/edit"))
                .andExpect(status().is(expectedStatus));
    }

    /**
     * Test that anonymous users are redirected to /login for protected endpoints.
     */
    @DisplayName("Anonymous access redirects to login")
    void testAnonymousAccessRedirects() throws Exception {
        mockMvc.perform(get("/permissions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/permissions/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/permissions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/permissions/550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/permissions/550e8400-e29b-41d4-a716-446655440000/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
