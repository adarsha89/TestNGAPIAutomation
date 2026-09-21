package com.reqres.automation.helpers;

import com.reqres.automation.assertions.ResponseAssertions;
import com.reqres.automation.assertions.SchemaAssertions;
import com.reqres.automation.testdata.ResponseExpectation;
import io.restassured.response.Response;

/**
 * Runs every {@code ResponseExpectation}-driven check for both
 * {@code RestUserService} and {@code WebhookService}.
 */
public final class RestUserAssertionHelper {

    private RestUserAssertionHelper() {
    }

    /** Runs every check set on {@code expectation} against {@code response}, in order, failing fast
     * on the first mismatch. */
    public static Response verify(Response response, ResponseExpectation expectation) {
        ResponseAssertions.assertStatusCode(response, expectation.expectedStatusCode());
        expectation.bodyValuesEqual().forEach((path, value) -> ResponseAssertions.assertBodyValueEquals(
                response, path, value));
        expectation.bodyValuesPresent().forEach(path -> ResponseAssertions.assertBodyValuePresent(response, path));
        expectation.bodyValuesAbsent().forEach(path -> ResponseAssertions.assertBodyValueAbsent(response, path));
        expectation.headersPresent().forEach(header -> ResponseAssertions.assertHeaderPresent(response, header));
        expectation.paginationDataArrayPath().ifPresent(
                path -> ResponseAssertions.assertPaginationMetadataConsistent(response, path));
        expectation.schemaClasspath().ifPresent(path -> SchemaAssertions.assertMatchesSchema(response, path));
        expectation.rawBodyEquals().ifPresent(body -> ResponseAssertions.assertRawBodyEquals(response, body));
        expectation.maxResponseTimeMillis().ifPresent(max -> ResponseAssertions.assertResponseTimeBelow(response, max));
        return response;
    }
}
