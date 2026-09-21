package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestTest;
import com.reqres.automation.dataproviders.AuthenticationApiDataProvider;
import com.reqres.automation.testdata.ResponseExpectation;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

/**
 * Coverage for {@code /api/login} and {@code /api/register} - no API key
 * required, success only for reqres's documented fixture credentials.
 */
@Story("Reqres login/register API")
public class AuthenticationApiTests extends BaseRestTest {

    @Test(groups = {"rest", "login", "regression"},
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

        restService().loginRawAndVerify(rawJsonBody, expectation);
    }

    @Test(groups = {"rest", "register", "regression"},
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

        restService().registerRawAndVerify(rawJsonBody, expectation);
    }
}
