package com.allensandiego.adm.e2e.scenarios;

import com.allensandiego.adm.e2e.support.TestContext;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for login/logout scenarios.
 * Validates authentication flow with seeded persona users.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class LoginScenarioTest {

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
    @DisplayName("Should display login page with form fields")
    void shouldDisplayLoginPageWithFormFields() {
        context.navigateTo("/login");

        assertThat(context.isVisible("login-username")).isTrue();
        assertThat(context.isVisible("login-password")).isTrue();
        assertThat(context.isVisible("login-submit")).isTrue();
    }

    @Test
    @DisplayName("Should login successfully with e2e.admin credentials")
    void shouldLoginSuccessfullyWithAdminCredentials() {
        context.loginAs("e2e.admin", "password123");

        // Should redirect to dashboard or home page
        assertThat(context.getCurrentUrl()).doesNotContain("/login");
        
        // Navigate to /users to verify user table is accessible for admin
        context.navigateTo("/users");
        assertThat(context.isVisible("user-table")).isTrue();
    }

    @Test
    @DisplayName("Should login successfully with e2e.viewer credentials")
    void shouldLoginSuccessfullyWithViewerCredentials() {
        context.loginAs("e2e.viewer", "password123");

        // Should redirect to dashboard or home page
        assertThat(context.getCurrentUrl()).doesNotContain("/login");
    }

    @Test
    @DisplayName("Should fail login with invalid credentials")
    void shouldFailLoginWithInvalidCredentials() {
        context.navigateTo("/login");
        context.fillInput("login-username", "invaliduser");
        context.fillInput("login-password", "wrongpassword");
        context.clickButton("login-submit");

        // Should show error message
        assertThat(context.getCurrentUrl()).contains("/login");
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() {
        context.loginAs("e2e.admin", "password123");
        context.logout();

        // Should redirect to login page
        assertThat(context.getCurrentUrl()).contains("/login");
    }

    @Test
    @DisplayName("Should redirect to login when accessing protected page without authentication")
    void shouldRedirectToLoginWhenUnauthenticated() {
        context.navigateTo("/users");

        // Should be redirected to login page
        assertThat(context.getCurrentUrl()).contains("/login");
    }

    @Test
    @DisplayName("E2E admin should see users table after login")
    void e2eAdminShouldSeeUsersTableAfterLogin() {
        context.loginAs("e2e.admin", "password123");

        // Navigate to /users to verify user table is accessible for admin
        context.navigateTo("/users");
        
        // Admin should have access to user management
        assertThat(context.isVisible("user-table")).isTrue();
        assertThat(context.isVisible("user-new-button")).isTrue();
    }

    @Test
    @DisplayName("Should capture screenshot on failure")
    void shouldCaptureScreenshotOnFailure() {
        context.navigateTo("/login");
        context.takeScreenshot("login-scenario");
        
        // Screenshot should be saved to target/e2e-artifacts/login-scenario/
        java.nio.file.Path screenshotPath = java.nio.file.Paths.get("target/e2e-artifacts/login-scenario/screenshot.png");
        assertThat(screenshotPath.toFile()).exists();
    }
}
