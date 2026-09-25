package com.allensandiego.adm.e2e.support;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base class for all Page Object Models.
 * Provides common browser interactions and element locators.
 */
public abstract class BasePageObjectModel {

    protected final Page page;
    protected final Playwright playwright;
    protected final String baseUrl;

    protected BasePageObjectModel(Page page, Playwright playwright, int port) {
        this.page = page;
        this.playwright = playwright;
        this.baseUrl = "http://localhost:" + port;
    }

    /**
     * Navigate to the given URL path.
     */
    protected void navigateTo(String path) {
        page.navigate(this.baseUrl + path);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * Fill a text input by data-testid.
     */
    protected void fillInput(String testId, String value) {
        page.getByTestId(testId).fill(value);
    }

    /**
     * Click a button/link by data-testid.
     */
    protected void clickButton(String testId) {
        page.getByTestId(testId).click();
    }

    /**
     * Get text content of an element by data-testid.
     */
    protected String getText(String testId) {
        return page.getByTestId(testId).textContent();
    }

    /**
     * Check if an element exists by data-testid.
     */
    protected boolean isVisible(String testId) {
        return page.getByTestId(testId).isVisible();
    }

    /**
     * Wait for an element to be visible.
     */
    protected void waitForVisible(String testId) {
        page.getByTestId(testId).waitFor();
    }

    /**
     * Take a screenshot and save to target/e2e-artifacts/ directory.
     */
    protected void takeScreenshot(String scenarioName) {
        Path artifactsDir = Paths.get("target/e2e-artifacts", scenarioName);
        try {
            java.nio.file.Files.createDirectories(artifactsDir);
            page.screenshot(new Page.ScreenshotOptions().setPath(artifactsDir.resolve("screenshot.png")));
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }

    /**
     * Get the current URL.
     */
    protected String getCurrentUrl() {
        return page.url();
    }

    /**
     * Wait for a navigation/response.
     */
    protected void waitForNavigation() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
}
