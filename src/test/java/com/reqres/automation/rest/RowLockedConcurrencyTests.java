package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.dataproviders.RowLockedConcurrencyDataProvider;
import com.reqres.automation.testdata.ClaimedRow;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.testdata.UserPoolRow;
import com.reqres.automation.testdata.UserRowPool;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Concurrency-proof test, kept separate from {@link RowLockedSingleRowTests} (and its sibling
 * {@link RowLockedMultiRowTests}) so its {@code dataProviderThreadCount} setting doesn't affect
 * those classes' functional coverage.
 */
@Story("Row-locked CSV data-driven tests")
public class RowLockedConcurrencyTests implements BaseRestInterface {

    private final ThreadLocal<ClaimedRow> CLAIMED_ROW = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    public void claimRowForConcurrentInvocation() {
        CLAIMED_ROW.set(UserRowPool.instance().claimRow());
    }

    @AfterMethod(alwaysRun = true)
    public void releaseRowForConcurrentInvocation() {
        ClaimedRow claimed = CLAIMED_ROW.get();
        if (claimed != null) {
            claimed.close();
        }
        CLAIMED_ROW.remove();
    }

    private UserPoolRow claimedRow() {
        return CLAIMED_ROW.get().row();
    }

    // dataProviderThreadCount isn't a @Test attribute on TestNG 7.9.0; parallel invocations come from
    // @DataProvider(parallel = true) plus data-provider-thread-count="8" in testng.xml instead.
    @Test(groups = {"rest", "row-locked", "concurrency", "regression", "row-locked-concurrency"},
            dataProvider = "concurrentClaimAttempts", dataProviderClass = RowLockedConcurrencyDataProvider.class)
    @Description("8 parallel invocations claim from a 5-row pool - the claimed-row set never exceeds pool "
            + "capacity (min(threadCount, numRows) = min(8, 5) = 5) and never holds a duplicate while a row is "
            + "held; no invocation deadlocks")
    public void shouldNeverExceedPoolCapacityUnderConcurrentClaims() {
        UserRowPool pool = UserRowPool.instance();
        UserPoolRow row = claimedRow();

        // check pool state while this row is still held
        Set<String> claimedKeysWhileHeld = pool.claimedRowKeysSnapshot();
        Assert.assertTrue(claimedKeysWhileHeld.contains(row.rowKey()),
                "claimed row " + row.rowKey() + " should be visible as claimed while held");
        Assert.assertTrue(claimedKeysWhileHeld.size() <= pool.capacity(),
                "claimed-row set size (" + claimedKeysWhileHeld.size() + ") exceeded pool capacity ("
                        + pool.capacity() + ")");

        // real outbound call, holds the row long enough to overlap with other parallel invocations
        restService().getUserByIdAndVerify(row.userId(),
                ResponseExpectation.status(200).andBodyValueEquals("data.id", row.userId()));
    }
}
