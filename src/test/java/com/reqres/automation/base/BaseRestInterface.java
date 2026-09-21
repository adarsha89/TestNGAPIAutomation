package com.reqres.automation.base;

import com.reqres.automation.clients.rest.UserRestClient;
import com.reqres.automation.helpers.ClientLifecycleHelper;
import com.reqres.automation.services.RestUserService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public interface BaseRestInterface {
    ThreadLocal<RestUserService> SERVICE = new ThreadLocal<>();

    @BeforeClass(alwaysRun = true)
    default void setUpRestClient() {
        UserRestClient client = ClientLifecycleHelper.createRestClient();
        SERVICE.set(new RestUserService(client));
    }

    @AfterClass(alwaysRun = true)
    default void tearDownRestClient() {
        RestUserService service = SERVICE.get();
        if (service != null) {
            service.close();
        }
        SERVICE.remove();
    }

    default RestUserService restService() {
        return SERVICE.get();
    }
}
