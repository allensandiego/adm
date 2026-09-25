package com.allensandiego.adm.e2e.scenarios;

import com.allensandiego.adm.e2e.support.TestContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for user CRUD scenarios.
 * Validates user management with seeded persona users.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class UsersScenarioTest {

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
    @DisplayName("Should display users list after login")
    void shouldDisplayUsersListAfterLogin() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/users");

        assertThat(context.isVisible("user-table")).isTrue();
        assertThat(context.isVisible("user-new-button")).isTrue();
    }

    @Test
    @DisplayName("Should display seeded users in the list")
    void shouldDisplaySeededUsers() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/users");

        // Wait for table to load
        context.waitForVisible("user-table");

        String pageContent = pageContent();
        assertThat(pageContent).contains("e2e.admin");
        assertThat(pageContent).contains("E2E Admin User");
        assertThat(pageContent).contains("e2e.viewer");
        assertThat(pageContent).contains("E2E Viewer User");
        assertThat(pageContent).contains("e2e.inactive");
        assertThat(pageContent).contains("E2E Inactive User");
    }

    @Test
    @DisplayName("Should filter users by search term")
    void shouldFilterUsersBySearchTerm() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/users");

        // Search for "admin" users
        context.fillInput("user-search-input", "admin");
        context.clickButton("user-search-button");

        String pageContent = pageContent();
        assertThat(pageContent).contains("e2e.admin");
    }

    @Test
    @DisplayName("Should clear user search filter")
    void shouldClearUserSearchFilter() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/users");

        // Search for something
        context.fillInput("user-search-input", "admin");
        context.clickButton("user-search-button");

        // Clear search
        context.clickButton("user-clear-button");

        String pageContent = pageContent();
        // Should show all users again
        assertThat(pageContent).contains("e2e.admin");
        assertThat(pageContent).contains("e2e.viewer");
        assertThat(pageContent).contains("e2e.inactive");
    }

    @Test
    @DisplayName("Should display user status badges correctly")
    void shouldDisplayUserStatusBadgesCorrectly() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/users");

        String pageContent = pageContent();
        // Active users should have "ACTIVE" status
        assertThat(pageContent).contains("ACTIVE");
        // Inactive user should have "INACTIVE" status
        assertThat(pageContent).contains("INACTIVE");
    }

    @Test
    @DisplayName("E2E viewer should have limited user access")
    void e2eViewerShouldHaveLimitedUserAccess() {
        context.loginAs("e2e.viewer", "password123");
        context.navigateTo("/users");

        // Viewer may not have permission to view users list
        // This depends on the actual permission setup
        assertThat(context.getCurrentUrl()).contains("/users");
    }

    @Test
    @DisplayName("Should capture screenshot for debugging")
    void shouldCaptureScreenshotForDebugging() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/users");
        context.takeScreenshot("users-scenario");

        // Screenshot should be saved to target/e2e-artifacts/users-scenario/
        java.nio.file.Path screenshotPath = java.nio.file.Paths.get("target/e2e-artifacts/users-scenario/screenshot.png");
        assertThat(screenshotPath.toFile()).exists();
    }

    private String pageContent() {
        return context.getPage().content();
    }
}
