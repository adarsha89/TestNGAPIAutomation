package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.dataproviders.UnknownResourceDataProvider;
import com.reqres.automation.testdata.ResponseExpectation;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

/**
 * Coverage for the {@code /api/unknown} (Pantone color) resource family -
 * same pagination/lookup shape as the user endpoints, no auth required.
 */
@Story("Reqres unknown/color resource API")
public class UnknownResourceApiTests implements BaseRestInterface {

    @Test(groups = {"rest", "regression"})
    @Description("GET /unknown lists color resources with consistent pagination fields")
    public void shouldListColorResources() {
        restService().getUnknownResourcesAndVerify(
                ResponseExpectation.status(200)
                        .andBodyValuePresent("page")
                        .andPaginationConsistent("data")
                        .andBodyValuePresent("data[0].id"));
    }

    @Test(groups = {"rest", "regression"},
            dataProvider = "unknownResourceIds", dataProviderClass = UnknownResourceDataProvider.class)
    @Description("GET /unknown/{id} returns 200 with the resource for an existing id, 404 with no data key "
            + "for a nonexistent id")
    public void shouldGetColorResourceByRawId(String caseName, String rawId, int expectedStatus) {
        ResponseExpectation expectation = ResponseExpectation.status(expectedStatus);
        if (expectedStatus == 200) {
            expectation.andBodyValuePresent("data.id");
        } else {
            expectation.andBodyValueAbsent("data");
        }

        restService().getUnknownResourceByRawIdAndVerify(rawId, expectation);
    }
}
