package com.allensandiego.adm.e2e.support;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages Playwright browser context and provides shared state for E2E tests.
 * Ensures proper cleanup of resources after each test.
 */
public class TestContext {

    private final Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private String baseUrl;

    public TestContext() {
        this.playwright = Playwright.create();
    }

    /**
     * Initialize a new browser context for testing.
     */
    public void initialize(int port) {
        this.baseUrl = "http://localhost:" + port;
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setSlowMo(100); // Slow down for visibility in CI/logs
        
        this.browser = playwright.chromium().launch(launchOptions);
        this.context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1280, 720)
                .setBaseURL(this.baseUrl));
        this.page = context.newPage();
    }

    /**
     * Get the current page instance.
     */
    public Page getPage() {
        return page;
    }

    /**
     * Navigate to a URL path.
     */
    public void navigateTo(String path) {
        page.navigate(this.baseUrl + path);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * Login as a specific user.
     */
    public void loginAs(String username, String password) {
        navigateTo("/login");
        page.getByTestId("login-username").fill(username);
        page.getByTestId("login-password").fill(password);
        page.getByTestId("login-submit").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * Logout the current user.
     */
    public void logout() {
        // Navigate to logout endpoint - this will trigger a redirect to /login?logout
        page.navigate(this.baseUrl + "/logout");
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * Take a screenshot and save to target/e2e-artifacts/ directory.
     */
    public void takeScreenshot(String scenarioName) {
        Path artifactsDir = Paths.get("target/e2e-artifacts", scenarioName);
        try {
            java.nio.file.Files.createDirectories(artifactsDir);
            page.screenshot(new Page.ScreenshotOptions().setPath(artifactsDir.resolve("screenshot.png")));
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }

    /**
     * Take a full-page screenshot and save with custom filename.
     */
    public void takeFullPageScreenshot(String filename) {
        try {
            Path screenshotPath = Paths.get("target/e2e-screenshots", filename);
            java.nio.file.Files.createDirectories(screenshotPath.getParent());
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotPath)
                    .setFullPage(true));
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }

    /**
     * Get current URL.
     */
    public String getCurrentUrl() {
        return page.url();
    }

    /**
     * Check if an element is visible by data-testid.
     */
    public boolean isVisible(String testId) {
        return page.getByTestId(testId).isVisible();
    }

    /**
     * Get text content of an element by data-testid.
     */
    public String getText(String testId) {
        return page.getByTestId(testId).textContent();
    }

    /**
     * Fill a form input by data-testid.
     */
    public void fillInput(String testId, String value) {
        page.getByTestId(testId).fill(value);
    }

    /**
     * Click an element by data-testid.
     */
    public void clickButton(String testId) {
        page.getByTestId(testId).click();
    }

    /**
     * Wait for an element to be visible.
     */
    public void waitForVisible(String testId) {
        page.getByTestId(testId).waitFor();
    }

    /**
     * Close the browser context and release resources.
     */
    public void close() {
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        playwright.close();
    }

    /**
     * Check if the context is initialized.
     */
    public boolean isInitialized() {
        return page != null && context != null && browser != null;
    }
}
