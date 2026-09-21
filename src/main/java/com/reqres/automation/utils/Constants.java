package com.reqres.automation.utils;

import java.util.Set;

/**
 * Centralized header names, property keys, timeouts and paths so they are
 * never duplicated/hardcoded across clients and tests.
 */
public final class Constants {

    private Constants() {
    }

    // Environment resolution
    public static final String ENV_SYSTEM_PROPERTY = "env";
    public static final String DEFAULT_ENV = "qa";

    // Base config layer shared by every environment (see ConfigLoader)
    public static final String CONFIG_DIR = "config/";
    public static final String COMMON_CONFIG_FILE = "common.properties";

    // Config property keys (also drive the env-var override name, see
    // ConfigLoader#toEnvVarName)
    public static final String REST_BASE_URL_PROPERTY = "rest.base.url";
    public static final String GRAPHQL_BASE_URL_PROPERTY = "graphql.base.url";
    public static final String WEBSOCKET_BASE_URL_PROPERTY = "websocket.base.url";
    public static final String WEBHOOK_PORT_PROPERTY = "webhook.port";
    public static final String API_KEY_PROPERTY = "api.key";
    public static final String REQUEST_TIMEOUT_PROPERTY = "request.timeout.ms";
    public static final String SENSITIVE_DATA_PROPERTY = "sensitive.data.names";

    // Headers
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_API_KEY = "x-api-key";

    // fallback when the sensitive.data.names config property is absent/blank;
    // EnvConfig#getSensitiveDataNames() is the authoritative source otherwise
    public static final Set<String> SENSITIVE_DATA = Set.of(
            "authorization", "api-key", HEADER_API_KEY, "apikey", "password", "token", "secret");

    // REST paths
    public static final String USERS_PATH = "/users";
    public static final String USER_BY_ID_PATH = "/users/{id}";

    // Defaults
    public static final int DEFAULT_REQUEST_TIMEOUT_MS = 10_000;
    public static final int DEFAULT_WEBHOOK_PORT = 0;
}
