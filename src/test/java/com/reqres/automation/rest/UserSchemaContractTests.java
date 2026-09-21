package com.reqres.automation.rest;

import com.reqres.automation.assertions.ResponseAssertions;
import com.reqres.automation.assertions.SchemaAssertions;
import com.reqres.automation.base.BaseRestTest;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

/**
 * Dedicated contract-validation coverage for the {@code GET /api/users/{id}}
 * response shape, isolated from {@link UserApiTests}'s functional field
 * assertions so a contract regression (e.g. a renamed/removed/added field)
 * is reported independently of functional test outcomes.
 */
@Story("Reqres user API — contract")
public class UserSchemaContractTests extends BaseRestTest {

    @Test(groups = {"rest", "contract"})
    @Description("GET a known user by id and validate the response against the tightened JSON schema contract")
    public void shouldMatchUserResponseSchemaContract() {
        Response response = restClient().getUserById(2);
        ResponseAssertions.assertStatusCode(response, 200);
        SchemaAssertions.assertMatchesSchema(response, "schemas/user-schema.json");
    }
}
