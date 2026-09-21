package com.reqres.automation.dataproviders;

import org.testng.annotations.DataProvider;

/**
 * {@code {endpointKey, configuredLimitPerSecond}} pairs for
 * {@code RateLimitTests} - two different configured limits (N != M) so
 * pacing is provably independent per endpoint key, see
 * {@code common.properties}' {@code rateLimitDemoA}/{@code rateLimitDemoB}.
 */
public final class RateLimitDataProvider {

    private RateLimitDataProvider() {
    }

    @DataProvider(name = "rateLimitedEndpoints")
    public static Object[][] rateLimitedEndpoints() {
        return new Object[][] {
                {"rateLimitDemoA", 5},
                {"rateLimitDemoB", 2},
        };
    }
}
