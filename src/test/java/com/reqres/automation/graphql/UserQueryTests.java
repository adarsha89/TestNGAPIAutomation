package com.reqres.automation.graphql;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.reqres.automation.assertions.GraphQLAssertions;
import com.reqres.automation.assertions.ResponseAssertions;
import com.reqres.automation.base.BaseGraphQLTest;
import com.reqres.automation.models.graphql.GraphQLRequest;
import com.reqres.automation.models.graphql.GraphQLResponse;
import com.reqres.automation.testdata.GraphQLPayloadBuilder;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Sample GraphQL coverage. No real GraphQL endpoint exists for this repo
 * yet, so this posts a canned query to a local WireMock stub returning a
 * fixed response - swap for a real endpoint the moment one exists.
 */
@Story("GraphQL user query (stubbed)")
public class UserQueryTests extends BaseGraphQLTest {

    private static final String USER_NAME = "George Bluth";
    private WireMockServer wireMockServer;

    @BeforeClass(alwaysRun = true)
    public void startGraphQLStub() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(0));
        wireMockServer.start();
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/graphql"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"data\": { \"user\": { \"id\": \"1\", \"name\": \"" + USER_NAME + "\" } } }")));
    }

    @AfterClass(alwaysRun = true)
    public void stopGraphQLStub() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test(groups = {"graphql", "smoke", "regression"})
    @Description("Post a canned user query to the local GraphQL stub and validate the parsed response data")
    public void shouldReturnUserDataFromGraphQLStub() {
        GraphQLRequest request = GraphQLPayloadBuilder.forUserQuery("1");

        Response response = client().postQuery(wireMockServer.baseUrl() + "/graphql", request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLResponse parsed = response.as(GraphQLResponse.class);
        Assert.assertEquals(parsed.getData().get("user").get("name").asText(), USER_NAME);
    }
}
