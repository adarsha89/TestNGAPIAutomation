package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestInterface;
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

    @Test(groups = {"rest", "contract"})
    @Description("GET a known user by id and validate the response against the tightened JSON schema contract")
    public void shouldMatchUserResponseSchemaContract() {
        restService().getUserByIdAndVerify(2,
                ResponseExpectation.status(200).withSchema("schemas/user-schema.json"));
    }
}
