package com.reqres.automation.dataproviders;

import org.testng.annotations.DataProvider;

/**
 * Yields 8 identical, parallel-eligible invocations - used only to drive N
 * concurrent claim attempts against the 5-row {@code UserRowPool}, not N
 * distinct CSV rows. Paired with {@code dataProviderThreadCount = 8} on the
 * consuming {@code @Test} in {@code RowLockedConcurrencyTests}.
 */
public final class RowLockedConcurrencyDataProvider {

    private RowLockedConcurrencyDataProvider() {
    }

    @DataProvider(name = "concurrentClaimAttempts", parallel = true)
    public static Object[][] concurrentClaimAttempts() {
        return new Object[][]{{}, {}, {}, {}, {}, {}, {}, {}};
    }
}
