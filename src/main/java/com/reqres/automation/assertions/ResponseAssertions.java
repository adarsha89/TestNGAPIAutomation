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

    public static Response assertRawBodyEquals(Response response, String expectedRawBody) {
        Assert.assertEquals(response.getBody().asString(), expectedRawBody,
                "Unexpected raw response body");
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

    public static Response assertBodyValueAbsent(Response response, String bodyPath) {
        Object actualValue = response.jsonPath().get(bodyPath);
        Assert.assertNull(actualValue,
                "Expected no value at body path '" + bodyPath + "', but found: " + actualValue);
        return response;
    }

    /**
     * Checks the value-relationships between a paginated list response's
     * metadata fields, not just their presence: {@code per_page} positive,
     * {@code total} non-negative, {@code total_pages} consistent with
     * {@code ceil(total / per_page)}, and the returned item count not
     * exceeding {@code per_page}.
     */
    public static Response assertPaginationMetadataConsistent(Response response, String dataArrayPath) {
        int perPage = response.jsonPath().getInt("per_page");
        int total = response.jsonPath().getInt("total");
        int totalPages = response.jsonPath().getInt("total_pages");
        int returnedCount = response.jsonPath().getList(dataArrayPath).size();

        Assert.assertTrue(perPage > 0, "Expected per_page > 0, was: " + perPage);
        Assert.assertTrue(total >= 0, "Expected total >= 0, was: " + total);
        int expectedTotalPages = (int) Math.ceil((double) total / perPage);
        Assert.assertEquals(totalPages, expectedTotalPages,
                "total_pages inconsistent with total/per_page. total=" + total + ", per_page=" + perPage
                        + ", expected total_pages=" + expectedTotalPages + ", actual=" + totalPages);
        Assert.assertTrue(returnedCount <= perPage,
                "Returned item count (" + returnedCount + ") exceeds per_page (" + perPage + ")");
        return response;
    }
}
