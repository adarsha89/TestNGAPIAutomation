package com.reqres.automation.clients.rest;

import com.reqres.automation.models.rest.UserRequest;
import com.reqres.automation.util.Constants;
import io.restassured.RestAssured;
import io.restassured.response.Response;

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

    public Response createUser(UserRequest request) {
        return RestAssured.given()
                .spec(spec)
                .body(request)
                .when()
                .post(Constants.USERS_PATH);
    }
}
