package com.reqres.automation.config;

import com.reqres.automation.utils.Constants;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

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

    // blank in the checked-in *.properties files on purpose - supply via API_KEY env var
    // or a local, gitignored <env>.local.properties override
    public String getApiKey() {
        return properties.getProperty(Constants.API_KEY_PROPERTY, "");
    }

    public int getRequestTimeoutMs() {
        return Integer.parseInt(properties.getProperty(
                Constants.REQUEST_TIMEOUT_PROPERTY,
                String.valueOf(Constants.DEFAULT_REQUEST_TIMEOUT_MS)));
    }

    // parses sensitive.data.names (comma-separated, trimmed, lower-cased), falling back to
    // Constants.SENSITIVE_DATA when absent/blank so masking works with zero config (FR10)
    public Set<String> getSensitiveDataNames() {
        String raw = properties.getProperty(Constants.SENSITIVE_DATA_PROPERTY);
        if (raw == null || raw.isBlank()) {
            return Constants.SENSITIVE_DATA;
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
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
