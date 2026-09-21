package com.reqres.automation.helpers;

import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.services.WebhookService;
import com.reqres.automation.utils.Constants;
import io.restassured.response.Response;

import java.util.function.Supplier;

/**
 * Config-gated, per-endpoint-key 503-fallback wrapper. Calls
 * {@code primaryCall}; if - and only if - the result's status is exactly
 * 503 and {@code <endpointKey>.fallback.on503.enabled=true}, makes exactly
 * one additional call to the configured local webhook path
 * ({@code <endpointKey>.fallback.webhook.path}) via the given
 * {@link WebhookService} and returns that response instead. Otherwise
 * returns the primary response unchanged. Never triggers for any status
 * other than 503.
 */
public final class FallbackHelper {

    private static final ThreadLocal<Boolean> FALLBACK_USED = new ThreadLocal<>();

    private FallbackHelper() {
    }

    public static Response executeWithFallback(String endpointKey, EnvConfig config,
            WebhookService fallbackReceiver, Supplier<Response> primaryCall) {
        Response primaryResponse = primaryCall.get();

        boolean fallbackEnabled = Boolean.parseBoolean(
                config.getProperty(endpointKey + Constants.FALLBACK_ON_503_ENABLED_SUFFIX));

        if (primaryResponse.getStatusCode() != 503 || !fallbackEnabled) {
            FALLBACK_USED.set(false);
            return primaryResponse;
        }

        String fallbackPath = config.getProperty(endpointKey + Constants.FALLBACK_WEBHOOK_PATH_SUFFIX);
        if (fallbackPath == null || fallbackPath.isBlank()) {
            FALLBACK_USED.set(false);
            return primaryResponse;
        }

        Response fallbackResponse = fallbackReceiver.sendCallerRequest(fallbackPath, "{}");
        FALLBACK_USED.set(true);
        return fallbackResponse;
    }

    // safe under parallel runs - callers own thread only, mirrors
    // LogMasker.MaskingLoggingFilter#lastCorrelationId()'s consume-and-clear shape
    public static boolean wasLastCallServedByFallback() {
        Boolean used = FALLBACK_USED.get();
        return used != null && used;
    }
}
