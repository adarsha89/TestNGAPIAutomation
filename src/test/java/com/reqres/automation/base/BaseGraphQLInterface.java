package com.reqres.automation.base;

import com.reqres.automation.clients.ClientFactory;
import com.reqres.automation.clients.graphql.GraphQLClient;
import com.reqres.automation.config.ConfigLoader;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public interface BaseGraphQLInterface {
    ThreadLocal<GraphQLClient> CLIENT = new ThreadLocal<>();

    @BeforeClass(alwaysRun = true)
    default void setUpGraphQLClient() {
        GraphQLClient client = (GraphQLClient) ClientFactory.create(ClientFactory.Protocol.REST, ConfigLoader.load());
        CLIENT.set(client);
    }

    @AfterClass(alwaysRun = true)
    default void tearDownGraphQLClient() {
        GraphQLClient client = CLIENT.get();
        if (client != null) {
            client.close();
        }
        CLIENT.remove();
    }

    default GraphQLClient graphQLClient() {
        return CLIENT.get();
    }
}
