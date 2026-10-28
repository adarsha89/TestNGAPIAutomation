package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.dataproviders.AuthenticationApiDataProvider;
import com.reqres.automation.mutation.ApiMutationSupport;
import com.reqres.automation.testdata.ResponseExpectation;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

/**
 * Coverage for {@code /api/login} and {@code /api/register} - no API key
 * required, success only for reqres's documented fixture credentials.
 */
@Story("Reqres login/register API")
public class AuthenticationApiTests implements BaseRestInterface {

    @Test(priority = 0, groups = {"rest", "login", "regression", "mutation"},
            dataProvider = "loginCases", dataProviderClass = AuthenticationApiDataProvider.class)
    @Description("POST /login succeeds with a token for fixture credentials, fails 400 with an error field "
            + "for missing password")
    public void shouldLoginWithFixtureCredentials(String caseName, String rawJsonBody, int expectedStatus,
            boolean expectTokenPresent, String expectedErrorMessage) {
        ResponseExpectation expectation = ResponseExpectation.status(expectedStatus);
        if (expectTokenPresent) {
            expectation.andBodyValuePresent("token");
        } else {
            expectation.andBodyValueEquals("error", expectedErrorMessage);
        }

        if (ApiMutationSupport.enabled()) {
            String baselineBody = expectTokenPresent
                    ? "{\"token\":\"fixture-token\"}"
                    : ApiMutationSupport.json(java.util.Map.of("error", expectedErrorMessage));
            String mutatedBody = expectTokenPresent
                    ? "{}"
                    : ApiMutationSupport.json(java.util.Map.of("error", "Invalid credentials"));
            ApiMutationSupport.verifyPostMutation(restService(), "AUTH-LOGIN-01", "/login", rawJsonBody,
                    expectedStatus, baselineBody, mutatedBody, expectation,
                    expectTokenPresent ? "body path 'token'" : "body path 'error'");
            return;
        }
        restService().loginRawAndVerify(rawJsonBody, expectation);
    }

    @Test(priority = 0, groups = {"rest", "register", "regression", "mutation"},
            dataProvider = "registerCases", dataProviderClass = AuthenticationApiDataProvider.class)
    @Description("POST /register succeeds with an id and token for fixture credentials, fails 400 with an "
            + "error field for missing password")
    public void shouldRegisterWithFixtureCredentials(String caseName, String rawJsonBody, int expectedStatus,
            boolean expectTokenPresent, String expectedErrorMessage) {
        ResponseExpectation expectation = ResponseExpectation.status(expectedStatus);
        if (expectTokenPresent) {
            expectation.andBodyValuePresent("id").andBodyValuePresent("token");
        } else {
            expectation.andBodyValueEquals("error", expectedErrorMessage);
        }

        if (ApiMutationSupport.enabled()) {
            String baselineBody = expectTokenPresent
                    ? "{\"id\":4,\"token\":\"fixture-token\"}"
                    : ApiMutationSupport.json(java.util.Map.of("error", expectedErrorMessage));
            String mutatedBody = expectTokenPresent
                    ? "{\"token\":\"fixture-token\"}"
                    : ApiMutationSupport.json(java.util.Map.of("error", "Invalid credentials"));
            ApiMutationSupport.verifyPostMutation(restService(), "AUTH-REGISTER-01", "/register", rawJsonBody,
                    expectedStatus, baselineBody, mutatedBody, expectation,
                    expectTokenPresent ? "body path 'id'" : "body path 'error'");
            return;
        }
        restService().registerRawAndVerify(rawJsonBody, expectation);
    }
}
