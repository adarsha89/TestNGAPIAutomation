package com.reqres.automation;

import com.reqres.automation.assertions.GraphQLAssertions;
import com.reqres.automation.assertions.ResponseAssertions;
import com.reqres.automation.assertions.SchemaAssertions;
import com.reqres.automation.base.BaseGraphQLInterface;
import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.clients.graphql.GraphQLStubServer;
import com.reqres.automation.models.graphql.GraphQLRequest;
import com.reqres.automation.testdata.GraphQLPayloadBuilder;
import io.qameta.allure.Description;
import io.restassured.response.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CompositeTests implements BaseRestInterface, BaseGraphQLInterface {


    private static final String USER_NAME = "George Bluth";
    private static final String STUB_PATH = "/graphql";
    private final GraphQLStubServer stubServer = new GraphQLStubServer();

    @BeforeClass(alwaysRun = true)
    public void startGraphQLStub() {
        stubServer.start();
        stubServer.stubQueryResponse(STUB_PATH,
                "{ \"data\": { \"user\": { \"id\": \"1\", \"name\": \"" + USER_NAME + "\" } } }");
    }

    @AfterClass(alwaysRun = true)
    public void stopGraphQLStub() {
        stubServer.stop();
    }

    @Test(groups = {"graphql", "smoke", "regression"})
    @Description("Post a canned user query to the local GraphQL stub and validate the parsed response data")
    public void shouldReturnUserDataFromGraphQLStub() {
        GraphQLRequest request = GraphQLPayloadBuilder.forUserQuery("1");

        Response response = graphQLClient().postQuery(stubServer.graphQLUrl(STUB_PATH), request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLAssertions.assertFieldEquals(response, "user.name", USER_NAME);
    }

    @Test(groups = {"rest", "smoke", "regression"})
    @Description("GET a known user by id, validate status, JSON schema, and a field value")
    public void shouldFetchExistingUserById() {
        Response response = restClient().getUserById(2);

        ResponseAssertions.assertStatusCode(response, 200);
        SchemaAssertions.assertMatchesSchema(response, "schemas/user-schema.json");
        ResponseAssertions.assertBodyValueEquals(response, "data.id", 2);
        ResponseAssertions.assertBodyValuePresent(response, "data.email");
    }
}
