package com.reqres.automation.helpers;

import com.reqres.automation.clients.rest.UserRestClient;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.models.rest.User;
import io.restassured.response.Response;

import java.util.Map;

/**
 * Thin static delegation wrapper around {@link UserRestClient} - the one
 * legal seam between {@code RestUserService} and the client layer. Each
 * method forwards unchanged to the same-named client method; request
 * shaping stays owned by {@code RestClientBase}/{@code UserRequestSpecification}.
 */
public final class RestUserClientHelper {

    private RestUserClientHelper() {
    }

    public static Response getUserById(UserRestClient client, int id) {
        return client.getUserById(id);
    }

    public static Response getUserByIdWithExtraParams(UserRestClient client, int id,
            Map<String, String> extraHeaders, Map<String, String> extraQueryParams) {
        return client.getUserByIdWithExtraParams(id, extraHeaders, extraQueryParams);
    }

    public static Response createUser(UserRestClient client, User user) {
        return client.createUser(user);
    }

    public static Response getUserByRawId(UserRestClient client, String id) {
        return client.getUserByRawId(id);
    }

    public static Response createUserRaw(UserRestClient client, String rawJsonBody) {
        return client.createUserRaw(rawJsonBody);
    }

    public static Response listUsers(UserRestClient client, int page) {
        return client.listUsers(page);
    }

    public static Response updateUserRaw(UserRestClient client, int id, String httpMethod, String rawJsonBody) {
        return client.updateUserRaw(id, httpMethod, rawJsonBody);
    }

    public static Response deleteUser(UserRestClient client, int id) {
        return client.deleteUser(id);
    }

    public static Response getUnknownResources(UserRestClient client) {
        return client.getUnknownResources();
    }

    public static Response getUnknownResourceByRawId(UserRestClient client, String rawId) {
        return client.getUnknownResourceByRawId(rawId);
    }

    public static Response loginRaw(UserRestClient client, String rawJsonBody) {
        return client.loginRaw(rawJsonBody);
    }

    public static Response registerRaw(UserRestClient client, String rawJsonBody) {
        return client.registerRaw(rawJsonBody);
    }

    public static Response getFromOverrideBaseUri(UserRestClient client, String overrideBaseUri, String path) {
        return client.getFromOverrideBaseUri(overrideBaseUri, path);
    }

    public static Response postToOverrideBaseUri(UserRestClient client, String overrideBaseUri, String path,
            String rawJsonBody) {
        return client.postToOverrideBaseUri(overrideBaseUri, path, rawJsonBody);
    }

    public static Response postToOverrideBaseUri(UserRestClient client, String overrideBaseUri, String path,
            String endpointKey, EnvConfig config, String rawJsonBody) {
        return client.postToOverrideBaseUri(overrideBaseUri, path, endpointKey, config, rawJsonBody);
    }

    public static void close(UserRestClient client) {
        client.close();
    }
}
