package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.mutation.ApiMutationSupport;
import com.reqres.automation.testdata.ResponseExpectation;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

/**
 * Dedicated contract-validation coverage for the {@code GET /api/users/{id}}
 * response shape, isolated from {@link UserApiTests}'s functional field
 * assertions so a contract regression (e.g. a renamed/removed/added field)
 * is reported independently of functional test outcomes.
 */
@Story("Reqres user API — contract")
public class UserSchemaContractTests implements BaseRestInterface {

    @Test(priority = 0, groups = {"rest", "contract", "mutation"})
    @Description("GET a known user by id and validate the response against the tightened JSON schema contract")
    public void shouldMatchUserResponseSchemaContract() {
        ResponseExpectation expectation = ResponseExpectation.status(200).withSchema("schemas/user-schema.json");
        if (ApiMutationSupport.enabled()) {
            String valid = "{\"data\":{\"id\":2,\"email\":\"janet.weaver@example.com\","
                    + "\"first_name\":\"Janet\",\"last_name\":\"Weaver\","
                    + "\"avatar\":\"https://example.test/2.png\"},"
                    + "\"support\":{\"url\":\"https://example.test/support\",\"text\":\"support\"}}";
            String mutant = "{\"data\":{\"id\":2,\"first_name\":\"Janet\",\"last_name\":\"Weaver\","
                    + "\"avatar\":\"https://example.test/2.png\"},"
                    + "\"support\":{\"url\":\"https://example.test/support\",\"text\":\"support\"}}";
            ApiMutationSupport.verifyGetMutation(restService(), "USER-SCHEMA-01", "/users/2", 200, valid,
                    mutant, expectation, "email");
            return;
        }
        restService().getUserByIdAndVerify(2, expectation);
    }
}
