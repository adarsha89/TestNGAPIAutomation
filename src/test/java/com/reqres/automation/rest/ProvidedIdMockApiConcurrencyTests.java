package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.clients.rest.ProvidedIdMockServer;
import com.reqres.automation.dataproviders.ProvidedIdConcurrencyDataProvider;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.utils.Constants;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Concurrency-proof coverage for the provided-ID mock API, kept separate from
 * {@link ProvidedIdMockApiTests} so its parallel data-provider-driven methods don't interleave
 * with that class's sequential accept/duplicate round trip. Follows
 * {@code RowLockedConcurrencyTests}/{@code RowLockedConcurrencyDataProvider}'s precedent: parallel
 * {@code @DataProvider} rows against a shared singleton ({@code ProvidedIdStore}), not raw
 * {@code ExecutorService}/{@code Thread} code in a test.
 */
@Story("Provided-ID duplicate-check mock API - concurrency")
public class ProvidedIdMockApiConcurrencyTests implements BaseRestInterface {

    private final ProvidedIdMockServer mockServer = new ProvidedIdMockServer();

    @BeforeClass(alwaysRun = true)
    public void startMockServer() {
        mockServer.start();
    }

    @AfterClass(alwaysRun = true)
    public void stopMockServer() {
        mockServer.stop();
    }

    @Test(priority = 3, groups = {"rest", "mock", "concurrency", "regression"},
            dataProvider = "distinctIdConcurrentSubmissions",
            dataProviderClass = ProvidedIdConcurrencyDataProvider.class)
    @Description("5 distinct, never-before-seen IDs submitted concurrently are each independently accepted: "
            + "201, status=accepted, id echoed back")
    public void shouldAcceptDistinctIdsSubmittedConcurrently(String id) {
        restService().postToOverrideBaseUriAndVerify(
                mockServer.stubUrl(""), Constants.PROVIDED_ID_PATH, Constants.PROVIDED_IDS_ENDPOINT_KEY,
                "{\"id\":\"" + id + "\"}",
                ResponseExpectation.status(201)
                        .andBodyValueEquals("id", id)
                        .andBodyValueEquals("status", "accepted"));
    }

    @Test(priority = 3, groups = {"rest", "mock", "concurrency", "negative", "regression"},
            dependsOnMethods = "shouldAcceptDistinctIdsSubmittedConcurrently", alwaysRun = true)
    @Description("Each of the 5 IDs concurrently accepted above is independently rejected on resubmission - "
            + "none was lost or overwritten by the concurrent acceptance")
    public void shouldShowEachConcurrentlyAcceptedIdAsIndependentlyDuplicateAfterward() {
        for (String id : ProvidedIdConcurrencyDataProvider.lastGeneratedDistinctIds()) {
            restService().postToOverrideBaseUriAndVerify(
                    mockServer.stubUrl(""), Constants.PROVIDED_ID_PATH, Constants.PROVIDED_IDS_ENDPOINT_KEY,
                    "{\"id\":\"" + id + "\"}",
                    ResponseExpectation.status(400)
                            .andBodyValueEquals("id", id)
                            .andBodyValueEquals("status", "duplicate"));
        }
    }

    @Test(priority = 3, groups = {"rest", "mock", "concurrency", "regression"},
            dataProvider = "sameIdConcurrentSubmissions", dataProviderClass = ProvidedIdConcurrencyDataProvider.class)
    @Description("8 concurrent submissions of the same never-before-seen ID resolve to exactly one winner - "
            + "tallied here (not per-row asserted) since only one row in the batch can legitimately see 201")
    public void shouldResolveConcurrentSameIdSubmissionsToExactlyOneWinner(String id) {
        Response response = restService().postToOverrideBaseUri(
                mockServer.stubUrl(""), Constants.PROVIDED_ID_PATH, Constants.PROVIDED_IDS_ENDPOINT_KEY,
                "{\"id\":\"" + id + "\"}");

        int status = response.getStatusCode();
        if (status == 201) {
            ProvidedIdConcurrencyDataProvider.recordSuccess();
        } else if (status == 400) {
            ProvidedIdConcurrencyDataProvider.recordDuplicate();
        } else {
            ProvidedIdConcurrencyDataProvider.recordUnexpected(status);
        }
    }

    @Test(priority = 3, groups = {"rest", "mock", "concurrency", "negative", "regression"},
            dependsOnMethods = "shouldResolveConcurrentSameIdSubmissionsToExactlyOneWinner", alwaysRun = true)
    @Description("The tally from the 8 concurrent same-ID submissions above is exactly one 201 and seven 400s, "
            + "no unexpected statuses, and the ID keeps being rejected on a further resubmission")
    public void shouldLeaveExactlyOneWinnerRecordedAfterConcurrentSameIdSubmissions() {
        Assert.assertEquals(ProvidedIdConcurrencyDataProvider.successCount(), 1,
                "Expected exactly one of the 8 concurrent same-ID submissions to be accepted");
        Assert.assertEquals(ProvidedIdConcurrencyDataProvider.duplicateCount(), 7,
                "Expected exactly seven of the 8 concurrent same-ID submissions to be rejected as duplicates");
        Assert.assertEquals(ProvidedIdConcurrencyDataProvider.unexpectedCount(), 0,
                "Expected no unexpected status codes among the 8 concurrent same-ID submissions");

        String sharedId = ProvidedIdConcurrencyDataProvider.lastGeneratedSameId();
        restService().postToOverrideBaseUriAndVerify(
                mockServer.stubUrl(""), Constants.PROVIDED_ID_PATH, Constants.PROVIDED_IDS_ENDPOINT_KEY,
                "{\"id\":\"" + sharedId + "\"}",
                ResponseExpectation.status(400)
                        .andBodyValueEquals("id", sharedId)
                        .andBodyValueEquals("status", "duplicate"));
    }
}
