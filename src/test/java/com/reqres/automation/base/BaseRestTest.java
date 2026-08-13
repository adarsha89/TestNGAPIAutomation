package com.reqres.automation.base;

import com.reqres.automation.clients.ClientFactory;
import com.reqres.automation.clients.rest.UserRestClient;
import com.reqres.automation.config.ConfigLoader;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Builds a thread-local {@link UserRestClient} (and its underlying
 * RequestSpecification) once per test class, released after the class
 * finishes. Parallel-safe: no shared mutable instance state between test
 * methods running on different threads.
 */
public abstract class BaseRestTest {

    private static final ThreadLocal<UserRestClient> CLIENT = new ThreadLocal<>();

    @BeforeClass(alwaysRun = true)
    public void setUpRestClient() {
        UserRestClient client = (UserRestClient) ClientFactory.create(ClientFactory.Protocol.REST, ConfigLoader.load());
        CLIENT.set(client);
    }

    @AfterClass(alwaysRun = true)
    public void tearDownRestClient() {
        UserRestClient client = CLIENT.get();
        if (client != null) {
            client.close();
        }
        CLIENT.remove();
    }

    protected UserRestClient client() {
        return CLIENT.get();
    }
}
