package com.reqres.automation.mutation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reqres.automation.clients.rest.RestStubServer;
import com.reqres.automation.config.ConfigLoader;
import com.reqres.automation.services.RestUserService;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.utils.Constants;

import java.util.Map;

/** Test-only support for running response mutations inside existing API test methods. */
public final class ApiMutationSupport {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ENDPOINT_KEY = "mutationTesting";

    private ApiMutationSupport() {
    }

    /** A non-blank -DMUTATION_ENABLED wins; otherwise MUTATION_ENABLED from the env's config files. */
    public static boolean enabled() {
        String override = System.getProperty(Constants.MUTATION_ENABLED_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Boolean.parseBoolean(override.trim());
        }
        return ConfigLoader.load().isMutationEnabled();
    }

    public static String json(Map<String, ?> value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize local mutation fixture", e);
        }
    }

    public static String createUserResponse(String name, String job) {
        return json(Map.of("name", name, "job", job, "id", "mutation-user",
                "createdAt", "2026-10-28T00:00:00Z"));
    }

    public static String createUserResponseWithWrongJob(String name, String job) {
        return createUserResponse(name, "mutated-" + job);
    }

    public static void verifyGetMutation(RestUserService service, String mutationId, String path, int status,
            String baselineBody, String mutatedBody, ResponseExpectation expectation, String failureFragment) {
        withStubServer(server -> {
            server.stubGetResponse(path, status, baselineBody);
            service.getFromOverrideBaseUriRateLimitedAndVerify(ENDPOINT_KEY, server.stubUrl(""), path, expectation);

            server.reset();
            server.stubGetResponse(path, status, mutatedBody);
            service.getFromOverrideBaseUriRateLimitedAndVerify(
                    ENDPOINT_KEY, server.stubUrl(""), path, expectation);
        });
    }

    public static void verifyGetBaseline(RestUserService service, String path, int status, String responseBody,
            ResponseExpectation expectation) {
        withStubServer(server -> {
            server.stubGetResponse(path, status, responseBody);
            service.getFromOverrideBaseUriRateLimitedAndVerify(ENDPOINT_KEY, server.stubUrl(""), path, expectation);
        });
    }

    public static void verifyPostMutation(RestUserService service, String mutationId, String path, String requestBody,
            int status, String baselineBody, String mutatedBody, ResponseExpectation expectation,
            String failureFragment) {
        withStubServer(server -> {
            server.stubPostResponse(path, status, baselineBody);
            service.postToOverrideBaseUriAndVerify(server.stubUrl(""), path, ENDPOINT_KEY, requestBody, expectation);

            server.reset();
            server.stubPostResponse(path, status, mutatedBody);
            service.postToOverrideBaseUriAndVerify(server.stubUrl(""), path, ENDPOINT_KEY, requestBody, expectation);
        });
    }

    public static void verifyPostBaseline(RestUserService service, String path, String requestBody, int status,
            String responseBody, ResponseExpectation expectation) {
        withStubServer(server -> {
            server.stubPostResponse(path, status, responseBody);
            service.postToOverrideBaseUriAndVerify(server.stubUrl(""), path, ENDPOINT_KEY, requestBody, expectation);
        });
    }

    private static void withStubServer(StubServerAction action) {
        RestStubServer server = new RestStubServer();
        server.start();
        try {
            action.run(server);
        } finally {
            server.stop();
        }
    }

    @FunctionalInterface
    private interface StubServerAction {
        void run(RestStubServer server);
    }
}
