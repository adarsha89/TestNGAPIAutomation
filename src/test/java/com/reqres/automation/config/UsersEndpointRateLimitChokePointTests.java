package com.reqres.automation.config;

import com.reqres.automation.clients.rest.RestStubServer;
import com.reqres.automation.clients.rest.UserRestClient;
import com.reqres.automation.services.RestUserService;
import com.reqres.automation.utils.Constants;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Properties;

// Proves RestClientBase.given()'s choke point paces a plain, un-opted-in call against the real
// "users" endpoint key with zero caller opt-in. Lives here (not in the rest package) so it can
// build EnvConfig directly via its package-private constructor with a stub-scoped, low
// users.rate.limit.per.second override - see EnvConfigSensitiveDataTests for the same pattern.
//
// Documented exception to the "no direct UserRestClient construction outside a service" rule
// (see docs/plans/code-change-plan-rate-limit-visibility-and-layering-2026-09-21.md, Section 6,
// Open Question B): the isolated rate-limit override above can't be built through the shared
// ClientFactory/ConfigLoader path, so @BeforeClass constructs the client/service directly. Its
// @Test bodies only ever call service.* and assert on the raw Response - no assertions/* call
// site here to migrate to an AndVerify overload.
@Story("Rate-limit choke point applies with no caller opt-in")
public class UsersEndpointRateLimitChokePointTests {

    private static final String STUB_PATH = "/users/2";
    private static final int PERMITS_PER_SECOND = 4;

    private final RestStubServer stubServer = new RestStubServer();
    private UserRestClient client;
    private RestUserService service;

    @BeforeClass(alwaysRun = true)
    public void startStubServerAndClient() {
        stubServer.start();
        stubServer.stubGetResponse(STUB_PATH, 200, "{}");

        Properties properties = new Properties();
        properties.setProperty(Constants.REST_BASE_URL_PROPERTY, stubServer.stubUrl(""));
        properties.setProperty(Constants.USERS_ENDPOINT_KEY + Constants.RATE_LIMIT_PER_SECOND_SUFFIX,
                String.valueOf(PERMITS_PER_SECOND));
        EnvConfig config = new EnvConfig("qa", properties);

        client = new UserRestClient();
        client.init(config);
        service = new RestUserService(client, config);
    }

    @AfterClass(alwaysRun = true)
    public void stopStubServerAndClient() {
        service.close();
        stubServer.stop();
    }

    @Test(groups = {"rest", "rate-limit", "regression"})
    @Description("A burst of plain getUserById(id) calls against the client's own configured 'users' endpoint "
            + "key is paced according to users.rate.limit.per.second with zero caller opt-in")
    public void shouldPaceUnoptedInGetUserByIdCallsAccordingToConfiguredUsersLimit() {
        int burstSize = PERMITS_PER_SECOND + 2;
        long minGapMs = (long) Math.ceil(1000.0 / PERMITS_PER_SECOND);
        long theoreticalMinimumMs = minGapMs * (burstSize - 1);

        long startedAt = System.currentTimeMillis();
        for (int i = 0; i < burstSize; i++) {
            Response response = service.getUserById(2);
            Assert.assertEquals(response.getStatusCode(), 200);
        }
        long elapsedMs = System.currentTimeMillis() - startedAt;

        Assert.assertTrue(elapsedMs >= theoreticalMinimumMs,
                "Expected elapsed time (" + elapsedMs + "ms) for " + burstSize + " plain getUserById(id) calls "
                        + "(limit " + PERMITS_PER_SECOND + "/s) to be >= the theoretical minimum ("
                        + theoreticalMinimumMs + "ms)");
    }

    @Test(groups = {"rest", "rate-limit", "regression"})
    @Description("A burst of getUserByIdWithFallback calls (fallback never triggers here since the primary "
            + "call succeeds) is still paced according to users.rate.limit.per.second")
    public void shouldPaceGetUserByIdWithFallbackCalls() {
        int burstSize = PERMITS_PER_SECOND + 2;
        long minGapMs = (long) Math.ceil(1000.0 / PERMITS_PER_SECOND);
        long theoreticalMinimumMs = minGapMs * (burstSize - 1);

        long startedAt = System.currentTimeMillis();
        for (int i = 0; i < burstSize; i++) {
            Response response = service.getUserByIdWithFallback(2, null);
            Assert.assertEquals(response.getStatusCode(), 200);
        }
        long elapsedMs = System.currentTimeMillis() - startedAt;

        Assert.assertTrue(elapsedMs >= theoreticalMinimumMs,
                "Expected elapsed time (" + elapsedMs + "ms) for " + burstSize + " getUserByIdWithFallback calls "
                        + "(limit " + PERMITS_PER_SECOND + "/s) to be >= the theoretical minimum ("
                        + theoreticalMinimumMs + "ms)");
    }

    @Test(groups = {"rest", "rate-limit", "regression"})
    @Description("A burst of getUserByIdWithRetry calls (retry never triggers here since the primary call "
            + "succeeds) is still paced according to users.rate.limit.per.second")
    public void shouldPaceGetUserByIdWithRetryCalls() {
        int burstSize = PERMITS_PER_SECOND + 2;
        long minGapMs = (long) Math.ceil(1000.0 / PERMITS_PER_SECOND);
        long theoreticalMinimumMs = minGapMs * (burstSize - 1);

        long startedAt = System.currentTimeMillis();
        for (int i = 0; i < burstSize; i++) {
            Response response = service.getUserByIdWithRetry(2);
            Assert.assertEquals(response.getStatusCode(), 200);
        }
        long elapsedMs = System.currentTimeMillis() - startedAt;

        Assert.assertTrue(elapsedMs >= theoreticalMinimumMs,
                "Expected elapsed time (" + elapsedMs + "ms) for " + burstSize + " getUserByIdWithRetry calls "
                        + "(limit " + PERMITS_PER_SECOND + "/s) to be >= the theoretical minimum ("
                        + theoreticalMinimumMs + "ms)");
    }
}
