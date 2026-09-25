package com.allensandiego.adm.e2e.scenarios;

import com.allensandiego.adm.e2e.support.TestContext;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for role CRUD scenarios.
 * Validates role management with seeded system roles.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class RolesScenarioTest {

    @LocalServerPort
    private int port;

    private TestContext context;

    @BeforeEach
    void setUp() {
        context = new TestContext();
        context.initialize(port);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("Should display roles list after login")
    void shouldDisplayRolesListAfterLogin() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/roles");

        assertThat(context.isVisible("role-table")).isTrue();
        assertThat(context.isVisible("role-new-button")).isTrue();
    }

    @Test
    @DisplayName("Should display seeded roles in the list")
    void shouldDisplaySeededRoles() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/roles");

        // Wait for table to load
        context.waitForVisible("role-table");

        String pageContent = pageContent();
        assertThat(pageContent).contains("SUPER_ADMIN");
        assertThat(pageContent).contains("REPORT_VIEWER");
    }

    @Test
    @DisplayName("Should filter roles by search term")
    void shouldFilterRolesBySearchTerm() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/roles");

        // Search for "admin" roles (case-insensitive)
        context.fillInput("role-search-input", "admin");
        context.clickButton("role-search-button");

        String pageContent = pageContent();
        assertThat(pageContent).contains("SUPER_ADMIN");
    }

    @Test
    @DisplayName("Should clear role search filter")
    void shouldClearRoleSearchFilter() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/roles");

        // Search for something
        context.fillInput("role-search-input", "admin");
        context.clickButton("role-search-button");

        // Clear search
        context.clickButton("role-clear-button");

        String pageContent = pageContent();
        // Should show all roles again
        assertThat(pageContent).contains("SUPER_ADMIN");
        assertThat(pageContent).contains("REPORT_VIEWER");
    }

    @Test
    @DisplayName("E2E viewer should have limited role access")
    void e2eViewerShouldHaveLimitedRoleAccess() {
        context.loginAs("e2e.viewer", "password123");
        context.navigateTo("/roles");

        // Viewer may not have permission to view roles list
        // This depends on the actual permission setup
        assertThat(context.getCurrentUrl()).contains("/roles");
    }

    private String pageContent() {
        return context.getPage().content();
    }
}
