package com.allensandiego.adm.config;

/**
 * E2E Test Configuration Resolver.
 * Reads environment variables and properties to configure Playwright tests.
 */
public class E2EConfig {

    private static final String DEFAULT_BASE_URL = "";
    private static final boolean DEFAULT_HEADLESS = true;
    private static final String DEFAULT_BROWSER = "chromium";
    private static final String DEFAULT_ARTIFACTS_DIR = "target/e2e-artifacts";

    /**
     * Creates a new E2EConfig with default values.
     */
    public static final class Config {
        private final String baseUrl;
        private final boolean headless;
        private final String browser;
        private final String artifactsDir;

        public Config(String baseUrl, boolean headless, String browser, String artifactsDir) {
            this.baseUrl = baseUrl;
            this.headless = headless;
            this.browser = browser;
            this.artifactsDir = artifactsDir;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public boolean isHeadless() {
            return headless;
        }

        public String getBrowser() {
            return browser;
        }

        public String getArtifactsDir() {
            return artifactsDir;
        }
    }

    /**
     * Creates E2EConfig from environment variables and properties.
     * Priority: System Environment > System Properties > Defaults
     */
    public static Config fromEnvironment() {
        String baseUrl = resolveProperty("e2e.base-url", DEFAULT_BASE_URL);
        boolean headless = resolveBoolean("e2e.headless", DEFAULT_HEADLESS);
        String browser = resolveProperty("e2e.browser", DEFAULT_BROWSER);
        String artifactsDir = resolveProperty("e2e.artifacts-dir", DEFAULT_ARTIFACTS_DIR);

        return new Config(baseUrl, headless, browser, artifactsDir);
    }

    private static String resolveProperty(String key, String defaultValue) {
        // Check system environment first
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // Check system properties
        String propValue = System.getProperty(key);
        if (propValue != null && !propValue.isEmpty()) {
            return propValue;
        }

        return defaultValue;
    }

    private static boolean resolveBoolean(String key, boolean defaultValue) {
        String value = resolveProperty(key, Boolean.toString(defaultValue));
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return defaultValue;
    }

    /**
     * Creates E2EConfig with runtime override capability.
     */
    public static Config fromEnvironment(String baseUrlOverride) {
        String baseUrl = resolveProperty("e2e.base-url", DEFAULT_BASE_URL);
        if (baseUrlOverride != null && !baseUrlOverride.isEmpty()) {
            baseUrl = baseUrlOverride;
        }

        boolean headless = resolveBoolean("e2e.headless", DEFAULT_HEADLESS);
        String browser = resolveProperty("e2e.browser", DEFAULT_BROWSER);
        String artifactsDir = resolveProperty("e2e.artifacts-dir", DEFAULT_ARTIFACTS_DIR);
        return new Config(baseUrl, headless, browser, artifactsDir);
    }
}
