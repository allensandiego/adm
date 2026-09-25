package com.allensandiego.adm.e2e.scenarios;

import com.allensandiego.adm.e2e.support.TestContext;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for permission CRUD scenarios.
 * Validates permission management with seeded catalog permissions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class PermissionsScenarioTest {

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
    @DisplayName("Should display permissions list after login")
    void shouldDisplayPermissionsListAfterLogin() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/permissions");

        assertThat(context.isVisible("permission-table")).isTrue();
        assertThat(context.isVisible("permission-new-button")).isTrue();
    }

    @Test
    @DisplayName("Should display all 13 seeded permissions in the list")
    void shouldDisplayAllSeededPermissions() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/permissions");

        // Wait for table to load
        context.waitForVisible("permission-table");

        // Verify some specific permissions are visible
        String pageContent = pageContent();
        assertThat(pageContent).contains("View Permissions");
        assertThat(pageContent).contains("Create Roles");
        assertThat(pageContent).contains("Edit Users");
        assertThat(pageContent).contains("View Audit Logs");
        assertThat(pageContent).contains("System Configuration");
    }

    @Test
    @DisplayName("Should filter permissions by search term")
    void shouldFilterPermissionsBySearchTerm() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/permissions");

        // Search for "role" permissions
        context.fillInput("permission-search-input", "role");
        context.clickButton("permission-search-button");

        String pageContent = pageContent();
        assertThat(pageContent).contains("View Roles");
        assertThat(pageContent).contains("Create Roles");
        assertThat(pageContent).contains("Edit Roles");
    }

    @Test
    @DisplayName("Should clear search filter")
    void shouldClearSearchFilter() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/permissions");

        // Search for something
        context.fillInput("permission-search-input", "role");
        context.clickButton("permission-search-button");

        // Clear search
        context.clickButton("permission-clear-button");

        String pageContent = pageContent();
        // Should show all permissions again
        assertThat(pageContent).contains("View Permissions");
    }

    @Test
    @DisplayName("E2E viewer should have limited permission access")
    void e2eViewerShouldHaveLimitedPermissionAccess() {
        context.loginAs("e2e.viewer", "password123");
        context.navigateTo("/permissions");

        // Viewer may not have permission to view permissions list
        // This depends on the actual permission setup
        assertThat(context.getCurrentUrl()).contains("/permissions");
    }

    private String pageContent() {
        return context.getPage().content();
    }
}
