package com.reqres.automation.utils;

import io.qameta.allure.Allure;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Redacts secrets (API keys, auth headers, tokens, passwords) before they hit
 * logs or the Allure report. Sensitive names normally come from
 * {@code EnvConfig#getSensitiveDataNames()}; {@link Constants#SENSITIVE_DATA}
 * is the fallback used by the no-arg overloads below.
 */
public final class LogMasker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogMasker.class);

    private static final String MASKED_VALUE = "***MASKED***";

    private LogMasker() {
    }

    public static boolean isSensitive(String key) {
        return isSensitive(key, Constants.SENSITIVE_DATA);
    }

    public static String maskValue(String key, String value) {
        return maskValue(key, value, Constants.SENSITIVE_DATA);
    }

    public static boolean isSensitive(String key, Set<String> sensitiveNames) {
        return key != null && sensitiveNames.contains(key.toLowerCase(Locale.ROOT));
    }

    public static String maskValue(String key, String value, Set<String> sensitiveNames) {
        return isSensitive(key, sensitiveNames) ? MASKED_VALUE : value;
    }

    // kept for RestAssuredConfigFactory's LogConfig#blacklistHeaders wiring
    public static Set<String> sensitiveHeaderNames() {
        return Constants.SENSITIVE_DATA;
    }

    /**
     * Logs each request/response to SLF4J with sensitive headers redacted,
     * and attaches masked copies to the Allure report. Never mutates the
     * real request - the actual outbound call still carries real values.
     */
    public static final class MaskingLoggingFilter implements Filter {

        private static final ThreadLocal<String> LAST_CORRELATION_ID = new ThreadLocal<>();

        private final Set<String> sensitiveNames;

        public MaskingLoggingFilter(Set<String> sensitiveNames) {
            this.sensitiveNames = (sensitiveNames == null || sensitiveNames.isEmpty())
                    ? Constants.SENSITIVE_DATA
                    : sensitiveNames;
        }

        // safe under parallel ("classes", thread-count=4) runs since filters run
        // synchronously on the caller's thread - each test thread sees only its own id
        public static String lastCorrelationId() {
            String correlationId = LAST_CORRELATION_ID.get();
            LAST_CORRELATION_ID.remove();
            return correlationId;
        }

        @Override
        public Response filter(FilterableRequestSpecification requestSpec,
                                FilterableResponseSpecification responseSpec,
                                FilterContext ctx) {
            String correlationId = UUID.randomUUID().toString().substring(0, 8);
            LAST_CORRELATION_ID.set(correlationId);
            String maskedUrl = stripQueryString(requestSpec.getURI());

            String headerSummary = buildHeaderSummary(requestSpec.getHeaders());

            StringBuilder queryParamSummary = new StringBuilder();
            for (Map.Entry<String, String> queryParam : requestSpec.getQueryParams().entrySet()) {
                queryParamSummary.append(queryParam.getKey())
                        .append('=')
                        .append(maskValue(queryParam.getKey(), queryParam.getValue(), sensitiveNames))
                        .append(' ');
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("--> {} {} headers=[{}] queryParams=[{}] corrId={}",
                        requestSpec.getMethod(), maskedUrl,
                        headerSummary, queryParamSummary.toString().trim(), correlationId);
            }

            Allure.addAttachment(
                    "REQUEST " + requestSpec.getMethod() + " " + maskedUrl + " [" + correlationId + "]",
                    "text/plain",
                    "Method: " + requestSpec.getMethod()
                            + "\nURL: " + maskedUrl
                            + "\nCorrelation-Id: " + correlationId
                            + "\nHeaders: " + headerSummary
                            + "\nQuery Params: " + queryParamSummary.toString().trim()
                            + "\nBody: " + buildBodySummary(requestSpec));

            Response response = ctx.next(requestSpec, responseSpec);

            String responseHeaderSummary = buildHeaderSummary(response.getHeaders());

            LOGGER.info("<-- {} {} status={} time={}ms corrId={}",
                    requestSpec.getMethod(), maskedUrl, response.getStatusCode(), response.getTime(),
                    correlationId);

            Allure.addAttachment(
                    "RESPONSE " + requestSpec.getMethod() + " " + maskedUrl + " [" + correlationId + "]",
                    "text/plain",
                    "Method: " + requestSpec.getMethod()
                            + "\nURL: " + maskedUrl
                            + "\nCorrelation-Id: " + correlationId
                            + "\nStatus: " + response.getStatusCode()
                            + "\nHeaders: " + responseHeaderSummary
                            + "\nBody: " + response.getBody().asString());

            return response;
        }

        // form-urlencoded bodies (e.g. TokenEndpointClient's client_secret/refresh_token
        // fields) are masked per-field; other body types fall back to the raw body as before
        private String buildBodySummary(FilterableRequestSpecification requestSpec) {
            Map<String, ?> formParams = requestSpec.getFormParams();
            if (formParams == null || formParams.isEmpty()) {
                return String.valueOf((Object) requestSpec.getBody());
            }
            StringBuilder maskedBody = new StringBuilder();
            for (Map.Entry<String, ?> formParam : formParams.entrySet()) {
                if (maskedBody.length() > 0) {
                    maskedBody.append('&');
                }
                Object formParamValue = formParam.getValue();
                maskedBody.append(formParam.getKey())
                        .append('=')
                        .append(maskValue(formParam.getKey(), String.valueOf(formParamValue), sensitiveNames));
            }
            return maskedBody.toString();
        }

        private String buildHeaderSummary(Iterable<Header> headers) {
            StringBuilder summary = new StringBuilder();
            for (Header header : headers) {
                summary.append(header.getName())
                        .append('=')
                        .append(maskValue(header.getName(), header.getValue(), sensitiveNames))
                        .append(' ');
            }
            return summary.toString().trim();
        }

        // query params are masked and shown separately, so strip them from the URL here
        private static String stripQueryString(String uri) {
            int queryStart = uri.indexOf('?');
            return queryStart == -1 ? uri : uri.substring(0, queryStart);
        }
    }
}
