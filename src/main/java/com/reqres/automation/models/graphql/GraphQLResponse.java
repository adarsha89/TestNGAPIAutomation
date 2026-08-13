package com.reqres.automation.models.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Standard GraphQL-over-HTTP response body: {@code data} kept as a raw
 * {@link JsonNode} since the shape varies per query, plus any
 * {@code errors}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLResponse {

    private JsonNode data;
    private List<GraphQLError> errors;

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    public List<GraphQLError> getErrors() {
        return errors;
    }

    public void setErrors(List<GraphQLError> errors) {
        this.errors = errors;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphQLError {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
