package com.reqres.automation.config;

import com.reqres.automation.util.Constants;

import java.util.Properties;

/**
 * Typed accessor over the properties resolved by {@link ConfigLoader} for
 * the active environment. Never construct directly - obtain an instance via
 * {@link ConfigLoader#load()}.
 */
public final class EnvConfig {

    private final String environment;
    private final Properties properties;

    EnvConfig(String environment, Properties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getRestBaseUrl() {
        return requireProperty(Constants.REST_BASE_URL_PROPERTY);
    }

    public String getGraphQLBaseUrl() {
        return requireProperty(Constants.GRAPHQL_BASE_URL_PROPERTY);
    }

    public String getWebSocketBaseUrl() {
        return requireProperty(Constants.WEBSOCKET_BASE_URL_PROPERTY);
    }

    public int getWebhookPort() {
        return Integer.parseInt(properties.getProperty(
                Constants.WEBHOOK_PORT_PROPERTY,
                String.valueOf(Constants.DEFAULT_WEBHOOK_PORT)));
    }

    /**
     * Never hardcode/commit a real value for this - it is deliberately
     * blank in the checked-in *.properties files and must be supplied via
     * the {@code API_KEY} environment variable or a local, gitignored
     * {@code <env>.local.properties} override.
     */
    public String getApiKey() {
        return properties.getProperty(Constants.API_KEY_PROPERTY, "");
    }

    public int getRequestTimeoutMs() {
        return Integer.parseInt(properties.getProperty(
                Constants.REQUEST_TIMEOUT_PROPERTY,
                String.valueOf(Constants.DEFAULT_REQUEST_TIMEOUT_MS)));
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    private String requireProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required config property '" + key + "' for environment '" + environment + "'");
        }
        return value;
    }
}
