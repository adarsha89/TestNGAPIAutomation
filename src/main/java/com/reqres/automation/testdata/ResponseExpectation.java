package com.reqres.automation.testdata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fluent value/builder object describing the expected outcome of a service
 * call - status code plus any combination of body/header/pagination/schema/
 * raw-body/timing checks. Carries no {@code Assert}/{@code Response}
 * reference and performs no assertion itself; a service's {@code verify(...)}
 * method is what turns this into actual checks against a {@code Response}.
 */
public final class ResponseExpectation {

    private final int expectedStatusCode;
    private final Map<String, Object> bodyValuesEqual = new LinkedHashMap<>();
    private final List<String> bodyValuesPresent = new ArrayList<>();
    private final List<String> bodyValuesAbsent = new ArrayList<>();
    private final List<String> headersPresent = new ArrayList<>();
    private String paginationDataArrayPath;
    private String schemaClasspath;
    private String rawBodyEquals;
    private Long maxResponseTimeMillis;

    private ResponseExpectation(int expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    public static ResponseExpectation status(int expectedStatusCode) {
        return new ResponseExpectation(expectedStatusCode);
    }

    public ResponseExpectation andBodyValueEquals(String bodyPath, Object expectedValue) {
        bodyValuesEqual.put(bodyPath, expectedValue);
        return this;
    }

    public ResponseExpectation andBodyValuePresent(String bodyPath) {
        bodyValuesPresent.add(bodyPath);
        return this;
    }

    public ResponseExpectation andBodyValueAbsent(String bodyPath) {
        bodyValuesAbsent.add(bodyPath);
        return this;
    }

    public ResponseExpectation andHeaderPresent(String headerName) {
        headersPresent.add(headerName);
        return this;
    }

    public ResponseExpectation andPaginationConsistent(String dataArrayPath) {
        this.paginationDataArrayPath = dataArrayPath;
        return this;
    }

    public ResponseExpectation withSchema(String classpathSchemaPath) {
        this.schemaClasspath = classpathSchemaPath;
        return this;
    }

    public ResponseExpectation andRawBodyEquals(String expectedRawBody) {
        this.rawBodyEquals = expectedRawBody;
        return this;
    }

    public ResponseExpectation andMaxResponseTimeMillis(long maxMillis) {
        this.maxResponseTimeMillis = maxMillis;
        return this;
    }

    public int expectedStatusCode() {
        return expectedStatusCode;
    }

    public Map<String, Object> bodyValuesEqual() {
        return bodyValuesEqual;
    }

    public List<String> bodyValuesPresent() {
        return bodyValuesPresent;
    }

    public List<String> bodyValuesAbsent() {
        return bodyValuesAbsent;
    }

    public List<String> headersPresent() {
        return headersPresent;
    }

    public Optional<String> paginationDataArrayPath() {
        return Optional.ofNullable(paginationDataArrayPath);
    }

    public Optional<String> schemaClasspath() {
        return Optional.ofNullable(schemaClasspath);
    }

    public Optional<String> rawBodyEquals() {
        return Optional.ofNullable(rawBodyEquals);
    }

    public Optional<Long> maxResponseTimeMillis() {
        return Optional.ofNullable(maxResponseTimeMillis);
    }
}
