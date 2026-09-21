package com.reqres.automation.clients.rest;

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
        return RestAssured.given()
                .spec(spec)
                .pathParam("id", id)
                .when()
                .get(Constants.USER_BY_ID_PATH);
    }

    // lets masking-verification tests inject extra headers/query params without a raw request inline
    public Response getUserByIdWithExtraParams(int id, Map<String, String> extraHeaders,
            Map<String, String> extraQueryParams) {
        return RestAssured.given()
                .spec(spec)
                .headers(extraHeaders)
                .queryParams(extraQueryParams)
                .pathParam("id", id)
                .when()
                .get(Constants.USER_BY_ID_PATH);
    }

    public Response createUser(User user) {
        return RestAssured.given()
                .spec(spec)
                .body(user)
                .when()
                .post(Constants.USERS_PATH);
    }

    /**
     * Raw-id variant for negative-path ids that don't fit an {@code int}
     * (non-numeric, negative, zero, etc.).
     */
    public Response getUserByRawId(String id) {
        return RestAssured.given()
                .spec(spec)
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
        RequestSpecification request = RestAssured.given().spec(spec);
        if (rawJsonBody != null) {
            request = request.body(rawJsonBody);
        }
        return request
                .when()
                .post(Constants.USERS_PATH);
    }
}
