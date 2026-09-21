package com.reqres.automation.models.graphql;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Standard GraphQL-over-HTTP request body: a query document plus its
 * variables.
 */
@Data
@NoArgsConstructor
public class GraphQLRequest {

    private String query;
    private Map<String, Object> variables;
}
