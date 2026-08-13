package com.reqres.automation.clients.graphql;

import com.reqres.automation.clients.ApiClient;
import com.reqres.automation.clients.RequestSpecFactory;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.models.graphql.GraphQLRequest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * POST /graphql wrapper (query + variables). No dedicated GraphQL client
 * library is used - RestAssured is sufficient for a POST-based GraphQL
 * transport.
 */
public class GraphQLClient implements ApiClient {

    private RequestSpecification spec;

    @Override
    public void init(EnvConfig config) {
        this.spec = RequestSpecFactory.baseSpecBuilder(config.getGraphQLBaseUrl(), config).build();
    }

    /** Posts against the configured base URL (real endpoint usage). */
    public Response postQuery(GraphQLRequest request) {
        return RestAssured.given()
                .spec(spec)
                .body(request)
                .when()
                .post();
    }

    /** Posts against an explicit URL - used to target a local stub/mock. */
    public Response postQuery(String overrideBaseUri, GraphQLRequest request) {
        return RestAssured.given()
                .spec(spec)
                .baseUri(overrideBaseUri)
                .body(request)
                .when()
                .post();
    }

    @Override
    public void close() {
        this.spec = null;
    }
}
