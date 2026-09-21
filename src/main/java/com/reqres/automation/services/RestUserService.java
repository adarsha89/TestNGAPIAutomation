package com.reqres.automation.services;

import com.reqres.automation.clients.rest.UserRestClient;
import com.reqres.automation.config.ConfigLoader;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.helpers.FallbackHelper;
import com.reqres.automation.helpers.RateLimitHelper;
import com.reqres.automation.helpers.RestUserAssertionHelper;
import com.reqres.automation.helpers.RestUserClientHelper;
import com.reqres.automation.helpers.RetryHelper;
import com.reqres.automation.models.rest.User;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.utils.Constants;
import io.restassured.response.Response;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thin pass-through service wrapping {@link UserRestClient} 1:1 (same
 * method names/signatures/return types as the client), constructed with an
 * already-built client instance (built via
 * {@code ClientLifecycleHelper.createRestClient()} in the base class).
 * Also exposes opt-in rate-limit/fallback-aware methods and an
 * override-base-URI passthrough for local stub targets.
 *
 * <p>Every {@code <method>AndVerify(...)} overload below performs the same
 * outbound call as its plain counterpart, then validates the result against
 * a caller-supplied {@link ResponseExpectation} via {@link #verify}, the
 * only place this service invokes {@code assertions/*}. Test classes call
 * only these {@code AndVerify}/plain methods - never {@code UserRestClient}
 * or {@code assertions/*} directly.</p>
 */
public class RestUserService {

    private final UserRestClient client;
    private final EnvConfig config;

    public RestUserService(UserRestClient client) {
        this(client, ConfigLoader.load());
    }

    public RestUserService(UserRestClient client, EnvConfig config) {
        this.client = client;
        this.config = config;
    }

    public Response getUserById(int id) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.getUserById(client, id);
    }

    public Response getUserByIdWithExtraParams(int id, Map<String, String> extraHeaders,
            Map<String, String> extraQueryParams) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.getUserByIdWithExtraParams(client, id, extraHeaders, extraQueryParams);
    }

    public Response createUser(User user) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.createUser(client, user);
    }

    public Response getUserByRawId(String id) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.getUserByRawId(client, id);
    }

    public Response createUserRaw(String rawJsonBody) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.createUserRaw(client, rawJsonBody);
    }

    public Response listUsers(int page) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.listUsers(client, page);
    }

    public Response updateUserRaw(int id, String httpMethod, String rawJsonBody) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.updateUserRaw(client, id, httpMethod, rawJsonBody);
    }

    public Response deleteUser(int id) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.deleteUser(client, id);
    }

    public Response getUnknownResources() {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.getUnknownResources(client);
    }

    public Response getUnknownResourceByRawId(String rawId) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.getUnknownResourceByRawId(client, rawId);
    }

    public Response loginRaw(String rawJsonBody) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.loginRaw(client, rawJsonBody);
    }

    public Response registerRaw(String rawJsonBody) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.registerRaw(client, rawJsonBody);
    }

    public void close() {
        RestUserClientHelper.close(client);
    }

    /** The only place this service invokes assertion-checking logic -
     * every {@code AndVerify} method below funnels through here. Throws (via {@code Assert}) on
     * the first mismatched expectation; otherwise returns the same, now-validated {@code response}. */
    private Response verify(Response response, ResponseExpectation expectation) {
        return RestUserAssertionHelper.verify(response, expectation);
    }

    public Response getUserByIdAndVerify(int id, ResponseExpectation expectation) {
        return verify(getUserById(id), expectation);
    }

    public Response getUserByIdWithExtraParamsAndVerify(int id, Map<String, String> extraHeaders,
            Map<String, String> extraQueryParams, ResponseExpectation expectation) {
        return verify(getUserByIdWithExtraParams(id, extraHeaders, extraQueryParams), expectation);
    }

    public Response createUserAndVerify(User user, ResponseExpectation expectation) {
        return verify(createUser(user), expectation);
    }

    public Response getUserByRawIdAndVerify(String id, ResponseExpectation expectation) {
        return verify(getUserByRawId(id), expectation);
    }

    public Response createUserRawAndVerify(String rawJsonBody, ResponseExpectation expectation) {
        return verify(createUserRaw(rawJsonBody), expectation);
    }

    public Response listUsersAndVerify(int page, ResponseExpectation expectation) {
        return verify(listUsers(page), expectation);
    }

    public Response updateUserRawAndVerify(int id, String httpMethod, String rawJsonBody,
            ResponseExpectation expectation) {
        return verify(updateUserRaw(id, httpMethod, rawJsonBody), expectation);
    }

    public Response deleteUserAndVerify(int id, ResponseExpectation expectation) {
        return verify(deleteUser(id), expectation);
    }

    public Response getUnknownResourcesAndVerify(ResponseExpectation expectation) {
        return verify(getUnknownResources(), expectation);
    }

    public Response getUnknownResourceByRawIdAndVerify(String rawId, ResponseExpectation expectation) {
        return verify(getUnknownResourceByRawId(rawId), expectation);
    }

    public Response loginRawAndVerify(String rawJsonBody, ResponseExpectation expectation) {
        return verify(loginRaw(rawJsonBody), expectation);
    }

    public Response registerRawAndVerify(String rawJsonBody, ResponseExpectation expectation) {
        return verify(registerRaw(rawJsonBody), expectation);
    }

    /** Fallback-aware variant of {@link #getUserById(int)} - delegates the 503-fallback decision
     * (config-gated, per-endpoint-key) to {@link FallbackHelper}. */
    public Response getUserByIdWithFallback(int id, WebhookService fallbackReceiver) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return FallbackHelper.executeWithFallback(Constants.USERS_ENDPOINT_KEY, config, fallbackReceiver,
                () -> RestUserClientHelper.getUserById(client, id));
    }

    /** Rate-limit-aware GET against an explicit local stub/mock URL, keyed by {@code endpointKey}. */
    public Response getFromOverrideBaseUriRateLimited(String endpointKey, String overrideBaseUri, String path) {
        RateLimitHelper.acquire(endpointKey, config);
        return RestUserClientHelper.getFromOverrideBaseUri(client, overrideBaseUri, path);
    }

    public Response getFromOverrideBaseUriRateLimitedAndVerify(String endpointKey, String overrideBaseUri,
            String path, ResponseExpectation expectation) {
        return verify(getFromOverrideBaseUriRateLimited(endpointKey, overrideBaseUri, path), expectation);
    }

    /** Fallback-aware GET against an explicit local stub/mock URL, keyed by {@code endpointKey}. */
    public Response getFromOverrideBaseUriWithFallback(String endpointKey, String overrideBaseUri, String path,
            WebhookService fallbackReceiver) {
        RateLimitHelper.acquire(endpointKey, config);
        return FallbackHelper.executeWithFallback(endpointKey, config, fallbackReceiver,
                () -> RestUserClientHelper.getFromOverrideBaseUri(client, overrideBaseUri, path));
    }

    public Response getFromOverrideBaseUriWithFallbackAndVerify(String endpointKey, String overrideBaseUri,
            String path, WebhookService fallbackReceiver, ResponseExpectation expectation) {
        return verify(getFromOverrideBaseUriWithFallback(endpointKey, overrideBaseUri, path, fallbackReceiver),
                expectation);
    }

    /** Posts against an explicit local stub/mock URL - used by auth-strategy demonstration tests. */
    public Response postToOverrideBaseUri(String overrideBaseUri, String path, String rawJsonBody) {
        RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
        return RestUserClientHelper.postToOverrideBaseUri(client, overrideBaseUri, path, rawJsonBody);
    }

    public Response postToOverrideBaseUriAndVerify(String overrideBaseUri, String path, String rawJsonBody,
            ResponseExpectation expectation) {
        return verify(postToOverrideBaseUri(overrideBaseUri, path, rawJsonBody), expectation);
    }

    /** Posts against an explicit local stub/mock URL using a request spec built fresh for
     * {@code endpointKey} - lets one service demonstrate multiple auth strategies. */
    public Response postToOverrideBaseUri(String overrideBaseUri, String path, String endpointKey,
            String rawJsonBody) {
        return RestUserClientHelper.postToOverrideBaseUri(client, overrideBaseUri, path, endpointKey, config,
                rawJsonBody);
    }

    public Response postToOverrideBaseUriAndVerify(String overrideBaseUri, String path, String endpointKey,
            String rawJsonBody, ResponseExpectation expectation) {
        return verify(postToOverrideBaseUri(overrideBaseUri, path, endpointKey, rawJsonBody), expectation);
    }

    /** Exposes this service's resolved {@link EnvConfig} - needed by demonstration tests that must
     * read the same per-endpoint-key properties (e.g. the fallback webhook path) this service uses. */
    public EnvConfig config() {
        return config;
    }

    /** Retry-aware variant of {@link #getUserById(int)} - retries via {@link RetryHelper} on a
     * transient response status ({@code retry.transient.statuses}, default 502/503/504), bounded
     * by {@code retry.max.attempts} (default 3) with {@code retry.initial.delay.ms} (default 200)
     * exponential backoff. Additive - {@link #getUserById(int)}'s behavior is unchanged. */
    public Response getUserByIdWithRetry(int id) {
        return RetryHelper.retryOnTransientStatus(
                () -> {
                    RateLimitHelper.acquire(Constants.USERS_ENDPOINT_KEY, config);
                    return RestUserClientHelper.getUserById(client, id);
                }, transientStatuses(), maxAttempts(), initialDelayMs());
    }

    /** Retry-aware GET against an explicit local stub/mock URL, used by transient-failure
     * demonstration tests since reqres.in cannot be told to fail transiently on demand. Keyed by
     * {@code endpointKey} so each individual retry attempt (a real outbound HTTP call) is paced. */
    public Response getFromOverrideBaseUriWithRetry(String endpointKey, String overrideBaseUri, String path) {
        return RetryHelper.retryOnTransientStatus(
                () -> {
                    RateLimitHelper.acquire(endpointKey, config);
                    return RestUserClientHelper.getFromOverrideBaseUri(client, overrideBaseUri, path);
                }, transientStatuses(), maxAttempts(), initialDelayMs());
    }

    public Response getFromOverrideBaseUriWithRetryAndVerify(String endpointKey, String overrideBaseUri,
            String path, ResponseExpectation expectation) {
        return verify(getFromOverrideBaseUriWithRetry(endpointKey, overrideBaseUri, path), expectation);
    }

    private Set<Integer> transientStatuses() {
        String raw = config.getProperty(Constants.RETRY_TRANSIENT_STATUSES_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = Constants.DEFAULT_RETRY_TRANSIENT_STATUSES;
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toUnmodifiableSet());
    }

    private int maxAttempts() {
        String raw = config.getProperty(Constants.RETRY_MAX_ATTEMPTS_PROPERTY);
        return (raw == null || raw.isBlank()) ? Constants.DEFAULT_RETRY_MAX_ATTEMPTS : Integer.parseInt(raw.trim());
    }

    private long initialDelayMs() {
        String raw = config.getProperty(Constants.RETRY_INITIAL_DELAY_MS_PROPERTY);
        return (raw == null || raw.isBlank())
                ? Constants.DEFAULT_RETRY_INITIAL_DELAY_MS
                : Long.parseLong(raw.trim());
    }
}
