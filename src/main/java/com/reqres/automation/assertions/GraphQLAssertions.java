package com.reqres.automation.assertions;

import com.reqres.automation.models.graphql.GraphQLResponse;
import io.restassured.response.Response;
import org.testng.Assert;

import java.util.List;
import java.util.stream.Collectors;

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
}
