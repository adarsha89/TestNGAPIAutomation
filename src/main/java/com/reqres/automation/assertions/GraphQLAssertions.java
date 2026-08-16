package com.reqres.automation.assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.reqres.automation.models.graphql.GraphQLResponse;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * GraphQL-shape-aware assertion helpers shared across GraphQL test classes.
 */
public final class GraphQLAssertions {

    private GraphQLAssertions() {
    }

    public static Response assertNoErrors(Response response) {
        GraphQLResponse parsed = response.as(GraphQLResponse.class);
        List<GraphQLResponse.GraphQLError> errors = parsed.getErrors();
        boolean hasErrors = errors != null && !errors.isEmpty();
        if (hasErrors) {
            String errorMessages = errors.stream()
                    .map(GraphQLResponse.GraphQLError::getMessage)
                    .collect(Collectors.joining("; "));
            Assert.fail("Expected no GraphQL errors, but found: " + errorMessages);
        }
        return response;
    }

    /**
     * Asserts a non-empty {@code errors} array is present in the GraphQL
     * response body, failing with the response body otherwise. Returns the
     * parsed error list for further inspection by the caller.
     */
    public static List<GraphQLResponse.GraphQLError> assertHasErrors(Response response) {
        GraphQLResponse parsed = response.as(GraphQLResponse.class);
        List<GraphQLResponse.GraphQLError> errors = parsed.getErrors();
        boolean hasErrors = errors != null && !errors.isEmpty();
        if (!hasErrors) {
            Assert.fail("Expected GraphQL errors, but found none. Body: " + response.getBody().asPrettyString());
        }
        return errors;
    }

    /**
     * Asserts a non-empty {@code errors} array is present, and that at least
     * one error message contains {@code expectedSubstring}. Returns the
     * parsed error list for further inspection by the caller.
     */
    public static List<GraphQLResponse.GraphQLError> assertErrorMessageContains(Response response, String expectedSubstring) {
        List<GraphQLResponse.GraphQLError> errors = assertHasErrors(response);
        boolean matches = errors.stream()
                .anyMatch(error -> error.getMessage() != null && error.getMessage().contains(expectedSubstring));
        Assert.assertTrue(matches, "Expected an error message containing '" + expectedSubstring
                + "'. Actual errors: " + errors.stream()
                        .map(GraphQLResponse.GraphQLError::getMessage)
                        .collect(Collectors.joining("; ")));
        return errors;
    }

    /** Asserts the value at {@code dataPath} (relative to {@code data}) equals {@code expectedValue}. */
    public static Response assertFieldEquals(Response response, String dataPath, String expectedValue) {
        JsonNode field = navigate(dataFromResponse(response), dataPath);
        Assert.assertNotNull(field, "Expected a value at data path '" + dataPath + "', but navigation returned nothing");
        Assert.assertEquals(field.asText(), expectedValue,
                "Unexpected value at data path '" + dataPath + "'");
        return response;
    }

    /** Asserts the value at {@code dataPath} (relative to {@code data}) is JSON null. */
    public static Response assertFieldIsNull(Response response, String dataPath) {
        JsonNode field = navigate(dataFromResponse(response), dataPath);
        Assert.assertTrue(field == null || field.isNull(),
                "Expected data path '" + dataPath + "' to be JSON null, but was: " + field);
        return response;
    }

    /**
     * Asserts the array at {@code arrayDataPath} (relative to {@code data})
     * is non-empty and every element has a non-blank value for each of
     * {@code fieldNames}.
     */
    public static Response assertEachArrayElementFieldsNotBlank(Response response, String arrayDataPath, String... fieldNames) {
        JsonNode array = navigate(dataFromResponse(response), arrayDataPath);
        Assert.assertTrue(array != null && array.isArray() && array.size() > 0,
                "Expected a non-empty array at data path '" + arrayDataPath + "'");
        for (JsonNode element : array) {
            for (String fieldName : fieldNames) {
                JsonNode fieldValue = element.get(fieldName);
                Assert.assertTrue(fieldValue != null && !fieldValue.asText().isBlank(),
                        "Expected non-blank '" + fieldName + "' on element of '" + arrayDataPath + "': " + element);
            }
        }
        return response;
    }

    /**
     * Asserts the array at {@code arrayDataPath} (relative to {@code data})
     * contains at least one element whose fields match every entry in
     * {@code expectedFieldValues}.
     */
    public static Response assertArrayContainsMatchingElement(Response response, String arrayDataPath,
                                                                Map<String, String> expectedFieldValues) {
        JsonNode array = navigate(dataFromResponse(response), arrayDataPath);
        Assert.assertTrue(array != null && array.isArray(),
                "Expected an array at data path '" + arrayDataPath + "'");
        boolean matches = StreamSupport.stream(array.spliterator(), false)
                .anyMatch(element -> expectedFieldValues.entrySet().stream()
                        .allMatch(entry -> element.get(entry.getKey()) != null
                                && entry.getValue().equals(element.get(entry.getKey()).asText())));
        Assert.assertTrue(matches, "Expected '" + arrayDataPath + "' to contain an element matching "
                + expectedFieldValues + ". Actual: " + array);
        return response;
    }

    /**
     * Asserts the array at {@code arrayDataPath} (relative to {@code data}),
     * collected by {@code fieldName}, contains every value in
     * {@code expectedValues}.
     */
    public static Response assertArrayFieldValuesContainAll(Response response, String arrayDataPath, String fieldName,
                                                              List<String> expectedValues) {
        JsonNode array = navigate(dataFromResponse(response), arrayDataPath);
        Assert.assertTrue(array != null && array.isArray(),
                "Expected an array at data path '" + arrayDataPath + "'");
        List<String> actualValues = StreamSupport.stream(array.spliterator(), false)
                .map(element -> element.get(fieldName).asText())
                .collect(Collectors.toList());
        for (String expectedValue : expectedValues) {
            Assert.assertTrue(actualValues.contains(expectedValue),
                    "Expected '" + arrayDataPath + "' field '" + fieldName + "' values to contain '" + expectedValue
                            + "'. Actual values: " + actualValues);
        }
        return response;
    }

    private static JsonNode dataFromResponse(Response response) {
        return response.as(GraphQLResponse.class).getData();
    }

    /** Dot-path traversal relative to the supplied root node (e.g. {@code "country.continent.code"}). */
    private static JsonNode navigate(JsonNode root, String dotPath) {
        JsonNode current = root;
        for (String segment : dotPath.split("\\.")) {
            if (current == null) {
                return null;
            }
            current = current.get(segment);
        }
        return current;
    }
}
