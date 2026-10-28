package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.dataproviders.UserApiDataProvider;
import com.reqres.automation.dataproviders.UserApiNegativeDataProvider;
import com.reqres.automation.models.rest.User;
import com.reqres.automation.mutation.ApiMutationSupport;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.testdata.UserDataBuilder;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

/**
 * Sample REST coverage against the real Reqres API
 * (https://reqres.in/api/users).
 */
@Story("Reqres user API")
public class UserApiTests implements BaseRestInterface {

    @Test(priority = 0, groups = {"rest", "smoke", "regression", "mutation"})
    @Description("GET a known user by id, validate status and a field value")
    public void shouldFetchExistingUserById() {
        ResponseExpectation expectation = ResponseExpectation.status(200)
                .andBodyValueEquals("data.id", 2)
                .andBodyValuePresent("data.email");
        if (ApiMutationSupport.enabled()) {
            ApiMutationSupport.verifyGetMutation(restService(), "USER-ID-01", "/users/2", 200,
                    "{\"data\":{\"id\":2,\"email\":\"janet.weaver@example.com\"}}",
                    "{\"data\":{\"id\":3,\"email\":\"janet.weaver@example.com\"}}", expectation,
                    "body path 'data.id'");
            return;
        }
        restService().getUserByIdAndVerify(2, expectation);
    }

    @Test(priority = 2, groups = {"rest", "smoke", "regression", "mutation"})
    @Description("Create a user with unique synthetic data and validate the echoed fields")
    public void shouldCreateUserWithSyntheticData() {
        User request = UserDataBuilder.uniqueUser();
        ResponseExpectation expectation = ResponseExpectation.status(201)
                .andBodyValueEquals("name", request.getName())
                .andBodyValueEquals("job", request.getJob())
                .andBodyValuePresent("id")
                .andBodyValuePresent("createdAt");
        if (ApiMutationSupport.enabled()) {
            ApiMutationSupport.verifyPostMutation(restService(), "USER-CREATE-01", "/users",
                    ApiMutationSupport.json(java.util.Map.of("name", request.getName(), "job", request.getJob())),
                    201, ApiMutationSupport.createUserResponse(request.getName(), request.getJob()),
                    ApiMutationSupport.createUserResponseWithWrongJob(request.getName(), request.getJob()),
                    expectation, "body path 'job'");
            return;
        }
        restService().createUserAndVerify(request, expectation);
    }

    @Test(priority = 1, groups = {"rest", "regression", "mutation"},
            dataProvider = "listUsersPagination", dataProviderClass = UserApiDataProvider.class)
    @Description("GET /users?page={page} returns the requested page with consistent pagination fields")
    public void shouldListUsersWithPagination(String caseName, int page) {
        ResponseExpectation expectation = ResponseExpectation.status(200)
                .andBodyValueEquals("page", page)
                .andPaginationConsistent("data")
                .andBodyValuePresent("data[0].id")
                .andBodyValuePresent("data[0].email")
                .andBodyValuePresent("data[0].avatar");
        if (ApiMutationSupport.enabled()) {
            String body = "{\"page\":" + page + ",\"per_page\":2,\"total\":4,\"total_pages\":2,"
                    + "\"data\":[{\"id\":3,\"email\":\"user3@example.com\","
                    + "\"avatar\":\"https://example.test/3.png\"}]}";
            int wrongPage = page == 1 ? 2 : 1;
            String mutatedBody = body.replace("\"page\":" + page, "\"page\":" + wrongPage);
            ApiMutationSupport.verifyGetMutation(restService(), "USER-PAGE-01", "/users?page=" + page, 200,
                    body, mutatedBody, expectation, "body path 'page'");
            return;
        }
        restService().listUsersAndVerify(page, expectation);
    }

    @Test(priority = 3, groups = {"rest", "regression"},
            dataProvider = "updateUserCases", dataProviderClass = UserApiDataProvider.class)
    @Description("PUT (full) / PATCH (partial) /users/{id} echoes back the submitted fields and adds updatedAt")
    public void shouldUpdateUser(String caseName, String httpMethod, String rawJsonBody, String expectedName,
            String expectedJob) {
        ResponseExpectation expectation = ResponseExpectation.status(200)
                .andBodyValueEquals("job", expectedJob)
                .andBodyValuePresent("updatedAt");
        if (expectedName != null) {
            expectation.andBodyValueEquals("name", expectedName);
        }

        restService().updateUserRawAndVerify(2, httpMethod, rawJsonBody, expectation);
    }

    @Test(priority = 3, groups = {"rest", "regression"})
    @Description("DELETE /users/{id} returns 204 with an empty body")
    public void shouldDeleteUser() {
        restService().deleteUserAndVerify(2, ResponseExpectation.status(204));
    }

    @Test(priority = 3, groups = {"rest", "negative", "regression"},
            dataProvider = "invalidUserIds", dataProviderClass = UserApiNegativeDataProvider.class)
    @Description("GET /users/{id} with an invalid id (nonexistent numeric, non-numeric, negative, zero) "
            + "returns 404 with no data key")
    public void shouldReturnNotFoundForInvalidUserId(String caseName, String rawId) {
        restService().getUserByRawIdAndVerify(rawId,
                ResponseExpectation.status(404).andBodyValueAbsent("data"));
    }

    @Test(priority = 3, groups = {"rest", "negative", "regression"},
            dataProvider = "incompleteCreateUserPayloads", dataProviderClass = UserApiNegativeDataProvider.class)
    @Description("POST /users with a missing or blank name/job field is accepted (no server-side required-field "
            + "validation) and echoes back only the fields actually sent")
    public void shouldAcceptCreateUserRequestDespiteMissingOrBlankFields(String caseName, String rawJsonBody,
            boolean nameExpectedPresent, String expectedNameValue, boolean jobExpectedPresent,
            String expectedJobValue) {
        ResponseExpectation expectation = ResponseExpectation.status(201);

        if (nameExpectedPresent) {
            expectation.andBodyValueEquals("name", expectedNameValue);
        } else {
            expectation.andBodyValueAbsent("name");
        }

        if (jobExpectedPresent) {
            expectation.andBodyValueEquals("job", expectedJobValue);
        } else {
            expectation.andBodyValueAbsent("job");
        }

        restService().createUserRawAndVerify(rawJsonBody, expectation);
    }

    @Test(priority = 3, groups = {"rest", "negative", "regression"})
    @Description("POST /users with a genuinely empty request body is rejected with 400")
    public void shouldRejectCreateUserRequestWithEmptyBody() {
        restService().createUserRawAndVerify(null,
                ResponseExpectation.status(400)
                        .andBodyValueEquals("error", "Empty request body")
                        .andBodyValueEquals("message", "Request body cannot be empty for JSON endpoints"));
    }
}
