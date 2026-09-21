package com.reqres.automation.clients.rest;

import com.reqres.automation.clients.RequestSpecFactory;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.helpers.RateLimitHelper;
import com.reqres.automation.models.rest.User;
import com.reqres.automation.utils.Constants;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

/**
 * Reqres {@code /api/users} wrapper - the sample REST client.
 */
public class UserRestClient extends RestClientBase {


    public Response getUserById(int id) {
        return given()
                .pathParam("id", id)
                .when()
                .get(Constants.USER_BY_ID_PATH);
    }

    // lets masking-verification tests inject extra headers/query params without a raw request inline
    public Response getUserByIdWithExtraParams(int id, Map<String, String> extraHeaders,
            Map<String, String> extraQueryParams) {
        return given()
                .headers(extraHeaders)
                .queryParams(extraQueryParams)
                .pathParam("id", id)
                .when()
                .get(Constants.USER_BY_ID_PATH);
    }

    public Response createUser(User user) {
        return given()
                .body(user)
                .when()
                .post(Constants.USERS_PATH);
    }

    public Response listUsers(int page) {
        return given()
                .queryParam(Constants.USERS_QUERY_PARAM_PAGE, page)
                .when()
                .get(Constants.USERS_PATH);
    }

    /**
     * Raw-body PUT/PATCH variant - one method serves both the full-update
     * and partial-update CSV rows since only {@code httpMethod} differs.
     */
    public Response updateUserRaw(int id, String httpMethod, String rawJsonBody) {
        RequestSpecification request = given()
                .pathParam("id", id)
                .body(rawJsonBody);
        if ("PATCH".equalsIgnoreCase(httpMethod)) {
            return request.when().patch(Constants.USER_BY_ID_PATH);
        }
        return request.when().put(Constants.USER_BY_ID_PATH);
    }

    public Response deleteUser(int id) {
        return given()
                .pathParam("id", id)
                .when()
                .delete(Constants.USER_BY_ID_PATH);
    }

    public Response getUnknownResources() {
        return given()
                .when()
                .get(Constants.UNKNOWN_PATH);
    }

    /**
     * Raw-id variant, mirrors {@link #getUserByRawId(String)} - lets a
     * nonexistent-id row and a well-formed row share one method.
     */
    public Response getUnknownResourceByRawId(String rawId) {
        return given()
                .pathParam("id", rawId)
                .when()
                .get(Constants.UNKNOWN_BY_ID_PATH);
    }

    public Response loginRaw(String rawJsonBody) {
        return given()
                .body(rawJsonBody)
                .when()
                .post(Constants.LOGIN_PATH);
    }

    public Response registerRaw(String rawJsonBody) {
        return given()
                .body(rawJsonBody)
                .when()
                .post(Constants.REGISTER_PATH);
    }

    /**
     * Raw-id variant for negative-path ids that don't fit an {@code int}
     * (non-numeric, negative, zero, etc.).
     */
    public Response getUserByRawId(String id) {
        return given()
                .pathParam("id", id)
                .when()
                .get(Constants.USER_BY_ID_PATH);
    }

    /**
     * Raw-body variant for negative payloads the typed {@link User}
     * model can't express - a field omitted entirely (vs. serialized as
     * {@code null}), or no body sent at all.
     */
    public Response createUserRaw(String rawJsonBody) {
        RequestSpecification request = given();
        if (rawJsonBody != null) {
            request = request.body(rawJsonBody);
        }
        return request
                .when()
                .post(Constants.USERS_PATH);
    }

    /**
     * Posts against an explicit URL - used to target a local stub/mock
     * (e.g. {@code RestStubServer}, {@code WebhookReceiver}) while still
     * exercising this client's configured auth-strategy request spec.
     */
    public Response postToOverrideBaseUri(String overrideBaseUri, String path, String rawJsonBody) {
        RequestSpecification request = given().baseUri(overrideBaseUri);
        if (rawJsonBody != null) {
            request = request.body(rawJsonBody);
        }
        return request
                .when()
                .post(path);
    }

    /**
     * Posts against an explicit URL using a request spec built fresh for
     * {@code endpointKey}, rather than this client's cached "users" spec -
     * lets one client instance exercise a different auth strategy
     * (e.g. BASIC, BEARER) per call.
     */
    public Response postToOverrideBaseUri(String overrideBaseUri, String path, String endpointKey,
            EnvConfig config, String rawJsonBody) {
        RateLimitHelper.acquire(endpointKey, config);
        RequestSpecification endpointSpec = RequestSpecFactory.baseSpecBuilder(overrideBaseUri, config, endpointKey)
                .build();
        RequestSpecification request = RestAssured.given().spec(endpointSpec);
        if (rawJsonBody != null) {
            request = request.body(rawJsonBody);
        }
        return request
                .when()
                .post(path);
    }

    /**
     * GETs against an explicit URL - used to target a local stub/mock
     * (e.g. {@code RestStubServer}) while still exercising this client's
     * configured auth-strategy request spec.
     */
    public Response getFromOverrideBaseUri(String overrideBaseUri, String path) {
        return RestAssured.given()
                .spec(userRequestSpecification.spec())
                .baseUri(overrideBaseUri)
                .when()
                .get(path);
    }
}
