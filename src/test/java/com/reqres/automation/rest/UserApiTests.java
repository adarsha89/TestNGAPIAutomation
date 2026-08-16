package com.reqres.automation.rest;

import com.reqres.automation.assertions.ResponseAssertions;
import com.reqres.automation.assertions.SchemaAssertions;
import com.reqres.automation.base.BaseRestTest;
import com.reqres.automation.models.rest.UserRequest;
import com.reqres.automation.testdata.UserDataBuilder;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

/**
 * Sample REST coverage against the real Reqres API
 * (https://reqres.in/api/users).
 */
@Story("Reqres user API")
public class UserApiTests extends BaseRestTest {

    @Test(groups = {"rest", "smoke", "regression"})
    @Description("GET a known user by id, validate status, JSON schema, and a field value")
    public void shouldFetchExistingUserById() {
        Response response = restClient().getUserById(2);
        ResponseAssertions.assertStatusCode(response, 200);
        SchemaAssertions.assertMatchesSchema(response, "schemas/user-schema.json");
        ResponseAssertions.assertBodyValueEquals(response, "data.id", 2);
        ResponseAssertions.assertBodyValuePresent(response, "data.email");
    }

    @Test(groups = {"rest", "smoke", "regression"})
    @Description("Create a user with unique synthetic data and validate the echoed fields")
    public void shouldCreateUserWithSyntheticData() {
        UserRequest request = UserDataBuilder.uniqueUser();

        Response response = client().createUser(request);

        ResponseAssertions.assertStatusCode(response, 201);
        ResponseAssertions.assertBodyValueEquals(response, "name", request.getName());
        ResponseAssertions.assertBodyValueEquals(response, "job", request.getJob());
    }
}
