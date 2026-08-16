package com.reqres.automation.assertions;

import io.restassured.response.Response;
import org.testng.Assert;

/**
 * Fluent status/header/latency assertion helpers shared across protocol
 * test classes.
 */
public final class ResponseAssertions {

    private ResponseAssertions() {
    }

    public static Response assertStatusCode(Response response, int expectedStatusCode) {
        Assert.assertEquals(response.getStatusCode(), expectedStatusCode,
                "Unexpected status code. Body: " + response.getBody().asPrettyString());
        return response;
    }

    public static Response assertHeaderPresent(Response response, String headerName) {
        Assert.assertNotNull(response.getHeader(headerName), "Expected header not present: " + headerName);
        return response;
    }

    public static Response assertResponseTimeBelow(Response response, long maxMillis) {
        Assert.assertTrue(response.getTime() <= maxMillis,
                "Response took " + response.getTime() + "ms, expected <= " + maxMillis + "ms");
        return response;
    }

    public static <T> Response assertBodyValueEquals(Response response, String bodyPath, T expectedValue) {
        Object actualValue = response.jsonPath().get(bodyPath);
        Assert.assertEquals(actualValue, expectedValue,
                "Unexpected value at body path '" + bodyPath + "'. Expected: " + expectedValue
                        + ", Actual: " + actualValue);
        return response;
    }

    public static Response assertBodyValuePresent(Response response, String bodyPath) {
        Object actualValue = response.jsonPath().get(bodyPath);
        Assert.assertNotNull(actualValue, "Expected a value present at body path '" + bodyPath + "', but found none");
        return response;
    }
}
