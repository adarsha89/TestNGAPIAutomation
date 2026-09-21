package com.reqres.automation.models.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * Standard GraphQL-over-HTTP response body: {@code data} kept as a raw
 * {@link JsonNode} since the shape varies per query, plus any
 * {@code errors}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLResponse {

    private JsonNode data;
    private List<GraphQLError> errors;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphQLError {
        private String message;
    }
}
