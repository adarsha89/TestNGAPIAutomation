package com.reqres.automation.util;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;

/**
 * Redacts secrets (API keys, auth headers, tokens, passwords) before they
 * are written to logs or attached to the Allure report.
 */
public final class LogMasker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogMasker.class);

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "authorization", "api-key", "x-api-key", "apikey", "password", "token", "secret");

    private static final String MASKED_VALUE = "***MASKED***";

    private LogMasker() {
    }

    public static boolean isSensitive(String key) {
        return key != null && SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT));
    }

    public static String maskHeaderValue(String key, String value) {
        return isSensitive(key) ? MASKED_VALUE : value;
    }

    /**
     * The canonical set of sensitive header names this framework redacts.
     * Consumed both by {@link MaskingLoggingFilter} (SLF4J log lines) and by
     * every client's {@code RequestSpecification} config
     * ({@code LogConfig#blacklistHeaders}) so that RestAssured's own
     * {@code AllureRestAssured} filter - which reads that same blacklist -
     * redacts the same header names before attaching request/response data
     * to the Allure report. This is what keeps the log line and the Allure
     * attachment consistently masked instead of only the former.
     */
    public static Set<String> sensitiveHeaderNames() {
        return SENSITIVE_KEYS;
    }

    /**
     * RestAssured filter that logs each request/response exchange to SLF4J
     * with sensitive header values redacted. This filter only controls what
     * reaches the SLF4J log line - it does not (and must not, since it would
     * corrupt the outgoing HTTP call) mutate the real header values on
     * {@code requestSpec}. Keeping secrets out of the Allure attachment too
     * is handled separately via {@link #sensitiveHeaderNames()} wired into
     * each client's {@code RestAssuredConfig} (see {@code RestClientBase}
     * and {@code GraphQLClient}), which {@code AllureRestAssured} itself
     * honors when rendering headers.
     */
    public static final class MaskingLoggingFilter implements Filter {

        @Override
        public Response filter(FilterableRequestSpecification requestSpec,
                                FilterableResponseSpecification responseSpec,
                                FilterContext ctx) {
            StringBuilder headerSummary = new StringBuilder();
            requestSpec.getHeaders().forEach(header ->
                    headerSummary.append(header.getName())
                            .append('=')
                            .append(maskHeaderValue(header.getName(), header.getValue()))
                            .append(' '));

            LOGGER.info("--> {} {} headers=[{}]",
                    requestSpec.getMethod(), requestSpec.getURI(), headerSummary.toString().trim());

            Response response = ctx.next(requestSpec, responseSpec);

            LOGGER.info("<-- {} {} status={} time={}ms",
                    requestSpec.getMethod(), requestSpec.getURI(), response.getStatusCode(), response.getTime());

            return response;
        }
    }
}
