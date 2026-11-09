package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.clients.rest.ProvidedIdMockServer;
import com.reqres.automation.testdata.ProvidedIdDataBuilder;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.utils.Constants;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Sequential accept/duplicate-rejection round trip against the local, CSV-backed
 * {@link ProvidedIdMockServer} - "the provided-ID mock API". Every call goes through
 * {@code RestUserService#postToOverrideBaseUri(String, String, String, String)} /
 * {@code ...AndVerify(...)} unchanged, targeting {@code mockServer.stubUrl("")} with
 * {@link Constants#PROVIDED_IDS_ENDPOINT_KEY} (unauthenticated, unrated - see common.properties).
 */
@Story("Provided-ID duplicate-check mock API")
public class ProvidedIdMockApiTests implements BaseRestInterface {

    private final ProvidedIdMockServer mockServer = new ProvidedIdMockServer();

    @BeforeClass(alwaysRun = true)
    public void startMockServer() {
        mockServer.start();
    }

    @AfterClass(alwaysRun = true)
    public void stopMockServer() {
        mockServer.stop();
    }

    @Test(priority = 3, groups = {"rest", "mock", "smoke", "regression"})
    @Description("Submitting an ID never seen before is accepted: 201, status=accepted, id echoed back")
    public void shouldAcceptIdNeverSubmittedBefore() {
        String id = ProvidedIdDataBuilder.uniqueId();

        restService().postToOverrideBaseUriAndVerify(
                mockServer.stubUrl(""), Constants.PROVIDED_ID_PATH, Constants.PROVIDED_IDS_ENDPOINT_KEY,
                "{\"id\":\"" + id + "\"}",
                ResponseExpectation.status(201)
                        .andBodyValueEquals("id", id)
                        .andBodyValueEquals("status", "accepted"));
    }

    @Test(priority = 3, groups = {"rest", "mock", "negative", "smoke", "regression"})
    @Description("Resubmitting an already-accepted ID is rejected: first submission 201 accepted, second "
            + "submission of the same ID 400 duplicate")
    public void shouldRejectIdWhenResubmittedAfterAcceptance() {
        String id = ProvidedIdDataBuilder.uniqueId();

        restService().postToOverrideBaseUriAndVerify(
                mockServer.stubUrl(""), Constants.PROVIDED_ID_PATH, Constants.PROVIDED_IDS_ENDPOINT_KEY,
                "{\"id\":\"" + id + "\"}",
                ResponseExpectation.status(201)
                        .andBodyValueEquals("id", id)
                        .andBodyValueEquals("status", "accepted"));

        restService().postToOverrideBaseUriAndVerify(
                mockServer.stubUrl(""), Constants.PROVIDED_ID_PATH, Constants.PROVIDED_IDS_ENDPOINT_KEY,
                "{\"id\":\"" + id + "\"}",
                ResponseExpectation.status(400)
                        .andBodyValueEquals("id", id)
                        .andBodyValueEquals("status", "duplicate"));
    }

    @Test(priority = 3, groups = {"rest", "mock", "negative", "regression"})
    @Description("A third submission of an already-rejected ID keeps being rejected - the original record is "
            + "never lost or overwritten by a repeated duplicate attempt")
    public void shouldKeepRejectingSameIdOnFurtherResubmission() {
        String id = ProvidedIdDataBuilder.uniqueId();

        restService().postToOverrideBaseUriAndVerify(
                mockServer.stubUrl(""), Constants.PROVIDED_ID_PATH, Constants.PROVIDED_IDS_ENDPOINT_KEY,
                "{\"id\":\"" + id + "\"}",
                ResponseExpectation.status(201)
                        .andBodyValueEquals("id", id)
                        .andBodyValueEquals("status", "accepted"));

        restService().postToOverrideBaseUriAndVerify(
                mockServer.stubUrl(""), Constants.PROVIDED_ID_PATH, Constants.PROVIDED_IDS_ENDPOINT_KEY,
                "{\"id\":\"" + id + "\"}",
                ResponseExpectation.status(400)
                        .andBodyValueEquals("id", id)
                        .andBodyValueEquals("status", "duplicate"));

        restService().postToOverrideBaseUriAndVerify(
                mockServer.stubUrl(""), Constants.PROVIDED_ID_PATH, Constants.PROVIDED_IDS_ENDPOINT_KEY,
                "{\"id\":\"" + id + "\"}",
                ResponseExpectation.status(400)
                        .andBodyValueEquals("id", id)
                        .andBodyValueEquals("status", "duplicate"));
    }
}
