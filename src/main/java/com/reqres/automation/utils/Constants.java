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
            "authorization", "api-key", HEADER_API_KEY, "apikey", "password", "token", "secret",
            "client_secret", "refresh_token");

    // REST paths
    public static final String USERS_PATH = "/users";
    public static final String USER_BY_ID_PATH = "/users/{id}";
    public static final String USERS_QUERY_PARAM_PAGE = "page";
    public static final String UNKNOWN_PATH = "/unknown";
    public static final String UNKNOWN_BY_ID_PATH = "/unknown/{id}";
    public static final String LOGIN_PATH = "/login";
    public static final String REGISTER_PATH = "/register";

    // Defaults
    public static final int DEFAULT_REQUEST_TIMEOUT_MS = 10_000;
    public static final int DEFAULT_WEBHOOK_PORT = 0;

    // Token cache config (see clients/auth/cache/TokenCacheRegistry)
    public static final String AUTH_TOKEN_CACHE_MAX_SIZE_PROPERTY = "auth.token.cache.max.size";
    public static final String AUTH_TOKEN_CACHE_MAX_TTL_MS_PROPERTY = "auth.token.cache.max.ttl.ms";
    public static final String AUTH_TOKEN_CACHE_SAFETY_MARGIN_SECONDS_PROPERTY =
            "auth.token.cache.safety.margin.seconds";
    public static final int DEFAULT_AUTH_TOKEN_CACHE_MAX_SIZE = 50;
    public static final long DEFAULT_AUTH_TOKEN_CACHE_MAX_TTL_MS = 24 * 60 * 60 * 1000L;
    public static final long DEFAULT_AUTH_TOKEN_CACHE_SAFETY_MARGIN_SECONDS = 30;

    // Per-endpoint-key auth/rate-limit/fallback config property suffixes (prefixed with
    // "<endpointKey>." by callers - see AuthStrategyFactory/RateLimitHelper/FallbackHelper)
    public static final String AUTH_STRATEGY_SUFFIX = ".auth.strategy";
    public static final String AUTH_HEADER_NAME_SUFFIX = ".auth.header.name";
    public static final String AUTH_USERNAME_SUFFIX = ".auth.username";
    public static final String AUTH_PASSWORD_SUFFIX = ".auth.password";
    public static final String AUTH_BEARER_TOKEN_SUFFIX = ".auth.bearer.token";
    public static final String AUTH_TOKEN_ENDPOINT_SUFFIX = ".auth.token.endpoint";
    public static final String AUTH_CLIENT_ID_SUFFIX = ".auth.client.id";
    public static final String AUTH_CLIENT_SECRET_SUFFIX = ".auth.client.secret";
    public static final String AUTH_SCOPE_SUFFIX = ".auth.scope";
    public static final String AUTH_JWT_TOKEN_SUFFIX = ".auth.jwt.token";
    public static final String AUTH_REFRESH_TOKEN_SUFFIX = ".auth.refresh.token";
    public static final String RATE_LIMIT_PER_SECOND_SUFFIX = ".rate.limit.per.second";
    public static final String FALLBACK_ON_503_ENABLED_SUFFIX = ".fallback.on503.enabled";
    public static final String FALLBACK_WEBHOOK_PATH_SUFFIX = ".fallback.webhook.path";

    // Endpoint keys
    public static final String USERS_ENDPOINT_KEY = "users";
    public static final String WEBHOOK_FALLBACK_DEMO_ENDPOINT_KEY = "webhookFallbackDemo";
    // Deliberately unconfigured (no <key>.rate.limit.per.second) - proves the no-op pacing baseline
    // for RetryHelperTests' existing exact-call-count/timing assertions.
    public static final String RETRY_DEMO_ENDPOINT_KEY = "retryDemo";

    // CLI HTML report output (see ImpactAnalysisCommand/FlakyReportCommand)
    public static final String CLI_REPORTS_DIR = "target/cli-reports";
    public static final String IMPACT_ANALYSIS_REPORT_FILE = CLI_REPORTS_DIR + "/impact-analysis-report.html";
    public static final String FLAKY_REPORT_FILE = CLI_REPORTS_DIR + "/flaky-report.html";

    // RetryHelper config property keys (see RestUserService#getUserByIdWithRetry)
    public static final String RETRY_TRANSIENT_STATUSES_PROPERTY = "retry.transient.statuses";
    public static final String RETRY_MAX_ATTEMPTS_PROPERTY = "retry.max.attempts";
    public static final String RETRY_INITIAL_DELAY_MS_PROPERTY = "retry.initial.delay.ms";
    public static final String DEFAULT_RETRY_TRANSIENT_STATUSES = "502,503,504";
    public static final int DEFAULT_RETRY_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_RETRY_INITIAL_DELAY_MS = 200;
}
