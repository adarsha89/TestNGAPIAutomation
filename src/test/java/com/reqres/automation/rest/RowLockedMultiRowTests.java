package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.models.rest.User;
import com.reqres.automation.testdata.ClaimedRows;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.testdata.UserPoolRow;
import com.reqres.automation.testdata.UserRowPool;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Multi-row claim/release shape: claims 3 exclusive rows from
 * {@link UserRowPool} via {@code @BeforeMethod}, drives the existing
 * {@code RestUserService} once per row, then releases all 3 rows via
 * {@code @AfterMethod} (pass, fail, or exception).
 */
@Story("Row-locked CSV data-driven tests")
public class RowLockedMultiRowTests implements BaseRestInterface {

    private final ThreadLocal<ClaimedRows> CLAIMED_ROWS = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    public void claimThreeRows() {
        CLAIMED_ROWS.set(UserRowPool.instance().claimRows(3));
    }

    @AfterMethod(alwaysRun = true)
    public void releaseThreeRows() {
        ClaimedRows claimed = CLAIMED_ROWS.get();
        if (claimed != null) {
            claimed.close();
        }
        CLAIMED_ROWS.remove();
    }

    private ClaimedRows claimedRows() {
        return CLAIMED_ROWS.get();
    }

    @Test(groups = {"rest", "row-locked", "multi-row", "regression", "row-locked-multi"})
    @Description("Claim 3 rows in one test, run fetch+create+update+delete once per row, and confirm no two "
            + "rows are ever the same claimed slot at once")
    public void shouldDriveFullLifecycleAcrossThreeClaimedRows() {
        Assert.assertEquals(claimedRows().size(), 3, "expected 3 distinct rows to be claimed");

        Set<String> rowKeysSeen = new HashSet<>();
        for (UserPoolRow row : claimedRows().rows()) {
            // exclusivity check: this row's key must not already be in use by another claim in this set
            Assert.assertTrue(rowKeysSeen.add(row.rowKey()),
                    "row " + row.rowKey() + " was claimed more than once within the same multi-row claim");

            restService().getUserByIdAndVerify(row.userId(),
                    ResponseExpectation.status(200).andBodyValueEquals("data.id", row.userId()));

            User createRequest = User.builder().name(row.name()).job(row.job()).build();
            restService().createUserAndVerify(createRequest,
                    ResponseExpectation.status(201)
                            .andBodyValueEquals("name", row.name())
                            .andBodyValueEquals("job", row.job()));

            restService().updateUserRawAndVerify(row.userId(), "PUT", "{\"job\":\"" + row.job() + "\"}",
                    ResponseExpectation.status(200).andBodyValueEquals("job", row.job()));

            restService().deleteUserAndVerify(row.userId(), ResponseExpectation.status(204));
        }
    }
}
