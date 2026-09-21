package com.reqres.automation.rest;

import com.reqres.automation.assertions.ResponseAssertions;
import com.reqres.automation.base.BaseRestTest;
import com.reqres.automation.dataproviders.UserApiNegativeDataProvider;
import com.reqres.automation.models.rest.User;
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
    @Description("GET a known user by id, validate status and a field value")
    public void shouldFetchExistingUserById() {
        Response response = restClient().getUserById(2);
        ResponseAssertions.assertStatusCode(response, 200);
        ResponseAssertions.assertBodyValueEquals(response, "data.id", 2);
        ResponseAssertions.assertBodyValuePresent(response, "data.email");
    }

    @Test(groups = {"rest", "smoke", "regression"})
    @Description("Create a user with unique synthetic data and validate the echoed fields")
    public void shouldCreateUserWithSyntheticData() {
        User request = UserDataBuilder.uniqueUser();

        Response response = restClient().createUser(request);

        ResponseAssertions.assertStatusCode(response, 201);
        ResponseAssertions.assertBodyValueEquals(response, "name", request.getName());
        ResponseAssertions.assertBodyValueEquals(response, "job", request.getJob());
    }

    @Test(groups = {"rest", "negative", "regression"},
            dataProvider = "invalidUserIds", dataProviderClass = UserApiNegativeDataProvider.class)
    @Description("GET /users/{id} with an invalid id (nonexistent numeric, non-numeric, negative, zero) "
            + "returns 404 with no data key")
    public void shouldReturnNotFoundForInvalidUserId(String caseName, String rawId) {
        Response response = restClient().getUserByRawId(rawId);

        ResponseAssertions.assertStatusCode(response, 404);
        ResponseAssertions.assertBodyValueAbsent(response, "data");
    }

    @Test(groups = {"rest", "negative", "regression"},
            dataProvider = "incompleteCreateUserPayloads", dataProviderClass = UserApiNegativeDataProvider.class)
    @Description("POST /users with a missing or blank name/job field is accepted (no server-side required-field "
            + "validation) and echoes back only the fields actually sent")
    public void shouldAcceptCreateUserRequestDespiteMissingOrBlankFields(String caseName, String rawJsonBody,
            boolean nameExpectedPresent, String expectedNameValue, boolean jobExpectedPresent,
            String expectedJobValue) {
        Response response = restClient().createUserRaw(rawJsonBody);

        ResponseAssertions.assertStatusCode(response, 201);

        if (nameExpectedPresent) {
            ResponseAssertions.assertBodyValueEquals(response, "name", expectedNameValue);
        } else {
            ResponseAssertions.assertBodyValueAbsent(response, "name");
        }

        if (jobExpectedPresent) {
            ResponseAssertions.assertBodyValueEquals(response, "job", expectedJobValue);
        } else {
            ResponseAssertions.assertBodyValueAbsent(response, "job");
        }
    }

    @Test(groups = {"rest", "negative", "regression"})
    @Description("POST /users with a genuinely empty request body is rejected with 400")
    public void shouldRejectCreateUserRequestWithEmptyBody() {
        Response response = restClient().createUserRaw(null);

        ResponseAssertions.assertStatusCode(response, 400);
        ResponseAssertions.assertBodyValueEquals(response, "error", "Empty request body");
        ResponseAssertions.assertBodyValueEquals(response, "message",
                "Request body cannot be empty for JSON endpoints");
    }
}
