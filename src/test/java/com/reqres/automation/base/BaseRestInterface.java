package com.reqres.automation.base;

import com.reqres.automation.clients.ClientFactory;
import com.reqres.automation.clients.rest.UserRestClient;
import com.reqres.automation.config.ConfigLoader;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public interface BaseRestInterface {
    ThreadLocal<UserRestClient> CLIENT = new ThreadLocal<>();

    @BeforeClass(alwaysRun = true)
    default void setUpRestClient() {
        UserRestClient client = (UserRestClient) ClientFactory.create(ClientFactory.Protocol.REST, ConfigLoader.load());
        CLIENT.set(client);
    }

    @AfterClass(alwaysRun = true)
    default void tearDownRestClient() {
        UserRestClient client = CLIENT.get();
        if (client != null) {
            client.close();
        }
        CLIENT.remove();
    }

    default UserRestClient restClient() {
        return CLIENT.get();
    }
}
