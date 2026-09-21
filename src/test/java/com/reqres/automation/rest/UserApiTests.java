package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestTest;
import com.reqres.automation.dataproviders.UserApiDataProvider;
import com.reqres.automation.dataproviders.UserApiNegativeDataProvider;
import com.reqres.automation.models.rest.User;
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
public class UserApiTests extends BaseRestTest {

    @Test(groups = {"rest", "smoke", "regression"})
    @Description("GET a known user by id, validate status and a field value")
    public void shouldFetchExistingUserById() {
        restService().getUserByIdAndVerify(2,
                ResponseExpectation.status(200)
                        .andBodyValueEquals("data.id", 2)
                        .andBodyValuePresent("data.email"));
    }

    @Test(groups = {"rest", "smoke", "regression"})
    @Description("Create a user with unique synthetic data and validate the echoed fields")
    public void shouldCreateUserWithSyntheticData() {
        User request = UserDataBuilder.uniqueUser();

        restService().createUserAndVerify(request,
                ResponseExpectation.status(201)
                        .andBodyValueEquals("name", request.getName())
                        .andBodyValueEquals("job", request.getJob())
                        .andBodyValuePresent("id")
                        .andBodyValuePresent("createdAt"));
    }

    @Test(groups = {"rest", "regression"},
            dataProvider = "listUsersPagination", dataProviderClass = UserApiDataProvider.class)
    @Description("GET /users?page={page} returns the requested page with consistent pagination fields")
    public void shouldListUsersWithPagination(String caseName, int page) {
        restService().listUsersAndVerify(page,
                ResponseExpectation.status(200)
                        .andBodyValueEquals("page", page)
                        .andPaginationConsistent("data")
                        .andBodyValuePresent("data[0].id")
                        .andBodyValuePresent("data[0].email")
                        .andBodyValuePresent("data[0].avatar"));
    }

    @Test(groups = {"rest", "regression"},
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

    @Test(groups = {"rest", "regression"})
    @Description("DELETE /users/{id} returns 204 with an empty body")
    public void shouldDeleteUser() {
        restService().deleteUserAndVerify(2, ResponseExpectation.status(204));
    }

    @Test(groups = {"rest", "negative", "regression"},
            dataProvider = "invalidUserIds", dataProviderClass = UserApiNegativeDataProvider.class)
    @Description("GET /users/{id} with an invalid id (nonexistent numeric, non-numeric, negative, zero) "
            + "returns 404 with no data key")
    public void shouldReturnNotFoundForInvalidUserId(String caseName, String rawId) {
        restService().getUserByRawIdAndVerify(rawId,
                ResponseExpectation.status(404).andBodyValueAbsent("data"));
    }

    @Test(groups = {"rest", "negative", "regression"},
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

    @Test(groups = {"rest", "negative", "regression"})
    @Description("POST /users with a genuinely empty request body is rejected with 400")
    public void shouldRejectCreateUserRequestWithEmptyBody() {
        restService().createUserRawAndVerify(null,
                ResponseExpectation.status(400)
                        .andBodyValueEquals("error", "Empty request body")
                        .andBodyValueEquals("message", "Request body cannot be empty for JSON endpoints"));
    }
}
