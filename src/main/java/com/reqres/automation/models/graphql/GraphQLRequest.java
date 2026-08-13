package com.reqres.automation.models.graphql;

import java.util.Map;

/**
 * Standard GraphQL-over-HTTP request body: a query document plus its
 * variables.
 */
public class GraphQLRequest {

    private String query;
    private Map<String, Object> variables;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}
