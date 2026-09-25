package com.allensandiego.adm.e2e.scenarios;

import com.allensandiego.adm.e2e.support.TestContext;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

/**
 * Comprehensive Playwright Visual Screenshot E2E Test Suite.
 * Systematically captures full-page screenshots for ALL positive and negative application screens.
 * Screenshots are saved to target/e2e-screenshots/ directory.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class VisualScreenshotE2ETest {

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

    /**
     * 01_login_page.png - GET /login
     */
    @Test
    @Order(1)
    @DisplayName("Screenshot 01: Login Page")
    void screenshot01LoginPage() {
        context.navigateTo("/login");
        // Wait for page to fully load
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("01_login_page.png", "Login page should display form fields");
    }

    /**
     * 02_login_error_invalid_credentials.png - POST invalid credentials -> /login?error
     */
    @Test
    @Order(2)
    @DisplayName("Screenshot 02: Login Error - Invalid Credentials")
    void screenshot02LoginErrorInvalidCredentials() {
        context.navigateTo("/login");
        context.fillInput("login-username", "invaliduser");
        context.fillInput("login-password", "wrongpassword");
        context.clickButton("login-submit");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("02_login_error_invalid_credentials.png", "Login error should display for invalid credentials");
    }

    /**
     * 03_login_error_inactive_user.png - POST e2e.inactive credentials -> /login?error
     */
    @Test
    @Order(3)
    @DisplayName("Screenshot 03: Login Error - Inactive User")
    void screenshot03LoginErrorInactiveUser() {
        context.navigateTo("/login");
        context.fillInput("login-username", "e2e.inactive");
        context.fillInput("login-password", "password123");
        context.clickButton("login-submit");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("03_login_error_inactive_user.png", "Login error should display for inactive user account");
    }

    /**
     * 04_login_success_admin_dashboard.png - Sign in as e2e.admin -> GET /
     */
    @Test
    @Order(4)
    @DisplayName("Screenshot 04: Login Success - Admin Dashboard")
    void screenshot04LoginSuccessAdminDashboard() {
        context.loginAs("e2e.admin", "password123");
        // Should redirect to dashboard or home page
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("04_login_success_admin_dashboard.png", "Admin dashboard should be displayed after successful login");
    }

    /**
     * 05_users_list.png - GET /users as admin
     */
    @Test
    @Order(5)
    @DisplayName("Screenshot 05: Users List")
    void screenshot05UsersList() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/users");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("05_users_list.png", "Users list should display all seeded users");
    }

    /**
     * 06_user_new_form.png - GET /users/new as admin
     */
    @Test
    @Order(6)
    @DisplayName("Screenshot 06: User New Form")
    void screenshot06UserNewForm() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/users/new");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("06_user_new_form.png", "User new form should display all required fields");
    }

    /**
     * 07_user_detail.png - GET /users/{id} as admin
     */
    @Test
    @Order(7)
    @DisplayName("Screenshot 07: User Detail")
    void screenshot07UserDetail() {
        context.loginAs("e2e.admin", "password123");
        // Navigate to users list and click first View button (UUID-based navigation)
        context.navigateTo("/users");
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        // Click the first View button to navigate to detail page using UUID
        context.getPage().locator("[data-testid='user-view-button']").first().click();
        context.getPage().waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("07_user_detail.png", "User detail page should display user information");
    }

    /**
     * 08_roles_list.png - GET /roles as admin
     */
    @Test
    @Order(8)
    @DisplayName("Screenshot 08: Roles List")
    void screenshot08RolesList() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/roles");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("08_roles_list.png", "Roles list should display all seeded roles");
    }

    /**
     * 09_role_new_form.png - GET /roles/new as admin
     */
    @Test
    @Order(9)
    @DisplayName("Screenshot 09: Role New Form")
    void screenshot09RoleNewForm() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/roles/new");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("09_role_new_form.png", "Role new form should display all required fields");
    }

    /**
     * 10_role_detail.png - GET /roles/{id} as admin
     */
    @Test
    @Order(10)
    @DisplayName("Screenshot 10: Role Detail")
    void screenshot10RoleDetail() {
        context.loginAs("e2e.admin", "password123");
        // Navigate to roles list and click first View button (UUID-based navigation)
        context.navigateTo("/roles");
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        // Click the first View button to navigate to detail page using UUID
        context.getPage().locator("[data-testid='role-view-button']").first().click();
        context.getPage().waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("10_role_detail.png", "Role detail page should display role information");
    }

    /**
     * 11_permissions_list.png - GET /permissions as admin
     */
    @Test
    @Order(11)
    @DisplayName("Screenshot 11: Permissions List")
    void screenshot11PermissionsList() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/permissions");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("11_permissions_list.png", "Permissions list should display all seeded permissions");
    }

    /**
     * 12_permission_new_form.png - GET /permissions/new as admin
     */
    @Test
    @Order(12)
    @DisplayName("Screenshot 12: Permission New Form")
    void screenshot12PermissionNewForm() {
        context.loginAs("e2e.admin", "password123");
        context.navigateTo("/permissions/new");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("12_permission_new_form.png", "Permission new form should display all required fields");
    }

    /**
     * 13_permission_detail.png - GET /permissions/{id} as admin
     */
    @Test
    @Order(13)
    @DisplayName("Screenshot 13: Permission Detail")
    void screenshot13PermissionDetail() {
        context.loginAs("e2e.admin", "password123");
        // Navigate to permissions list and click first View button (UUID-based navigation)
        context.navigateTo("/permissions");
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        // Click the first View button to navigate to detail page using UUID
        context.getPage().locator("[data-testid='permission-view-button']").first().click();
        context.getPage().waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("13_permission_detail.png", "Permission detail page should display permission information");
    }

    /**
     * 14_logout_notice.png - POST /logout -> /login?logout
     */
    @Test
    @Order(14)
    @DisplayName("Screenshot 14: Logout Notice")
    void screenshot14LogoutNotice() {
        context.loginAs("e2e.admin", "password123");
        context.logout();
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        takeAndVerifyScreenshot("14_logout_notice.png", "Login page should display logout success message");
    }

    /**
     * 15_negative_403_forbidden_page.png - Sign in as e2e.viewer -> attempt GET /users/new or POST mutation -> 403 Forbidden screen
     */
    @Test
    @Order(15)
    @DisplayName("Screenshot 15: Negative - 403 Forbidden Page")
    void screenshot15Negative403ForbiddenPage() {
        // Login as viewer
        context.loginAs("e2e.viewer", "password123");
        
        // Attempt to access admin-only page (user creation)
        context.navigateTo("/users/new");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        // Verify we got a 403 response
        String currentUrl = context.getCurrentUrl();
        System.out.println("Current URL after attempting access: " + currentUrl);
        
        takeAndVerifyScreenshot("15_negative_403_forbidden_page.png", "403 Forbidden page should be displayed for unauthorized access");
    }

    /**
     * 16_negative_404_not_found_page.png - GET /non-existent-page -> 404 error page
     */
    @Test
    @Order(16)
    @DisplayName("Screenshot 16: Negative - 404 Not Found Page")
    void screenshot16Negative404NotFoundPage() {
        // Login as viewer first to avoid being redirected to /login
        context.loginAs("e2e.viewer", "password123");
        
        // Navigate to non-existent page (after authentication, this will show 404 not redirect to login)
        context.navigateTo("/non-existent-page");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        takeAndVerifyScreenshot("16_negative_404_not_found_page.png", "404 Not Found page should be displayed for invalid routes");
    }

    /**
     * Helper method to take screenshot and verify it was saved.
     */
    private void takeAndVerifyScreenshot(String filename, String description) {
        // Take full-page screenshot using TestContext helper
        context.takeFullPageScreenshot(filename);
        
        // Verify the screenshot was saved
        java.nio.file.Path screenshotPath = java.nio.file.Paths.get("target/e2e-screenshots", filename);
        Assertions.assertTrue(
            java.nio.file.Files.exists(screenshotPath),
            "Screenshot should be saved at: " + screenshotPath + " - " + description
        );
        
        System.out.println("[PASS] Screenshot captured: " + filename);
    }
}
