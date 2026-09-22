package com.reqres.automation.base;

import com.reqres.automation.clients.rest.UserRestClient;
import com.reqres.automation.helpers.ClientLifecycleHelper;
import com.reqres.automation.services.RestUserService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public interface BaseRestInterface {
    ThreadLocal<RestUserService> SERVICE = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    default void setUpRestClient() {
        UserRestClient client = ClientLifecycleHelper.createRestClient();
        SERVICE.set(new RestUserService(client));
    }

    @AfterMethod(alwaysRun = true)
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
