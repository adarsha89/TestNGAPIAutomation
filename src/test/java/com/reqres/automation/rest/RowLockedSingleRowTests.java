package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.models.rest.User;
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

/**
 * Row-locked, CSV-backed coverage of the six in-scope user/auth endpoints -
 * each test claims one exclusive row from {@link UserRowPool} via
 * {@code @BeforeMethod}, drives the existing {@code RestUserService} with
 * that row's fields, then releases the row via {@code @AfterMethod} (pass,
 * fail, or exception).
 */
@Story("Row-locked CSV data-driven tests")
public class RowLockedSingleRowTests implements BaseRestInterface {

    private final ThreadLocal<ClaimedRow> CLAIMED_ROW = new ThreadLocal<>();
    private final ThreadLocal<ClaimedRow> LAST_FAILURE_TEST_CLAIM = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    public void claimRow() {
        CLAIMED_ROW.set(UserRowPool.instance().claimRow());
    }

    @AfterMethod(alwaysRun = true)
    public void releaseRow() {
        ClaimedRow claimed = CLAIMED_ROW.get();
        if (claimed != null) {
            claimed.close();
        }
        CLAIMED_ROW.remove();
    }

    private UserPoolRow claimedRow() {
        return CLAIMED_ROW.get().row();
    }

    @Test(groups = {"rest", "row-locked", "smoke", "regression", "row-locked-single"})
    @Description("GET /users/{id} using a claimed row's userId")
    public void shouldFetchExistingUserUsingClaimedRow() {
        UserPoolRow row = claimedRow();
        restService().getUserByIdAndVerify(row.userId(),
                ResponseExpectation.status(200)
                        .andBodyValueEquals("data.id", row.userId())
                        .andBodyValuePresent("data.email"));
    }

    @Test(groups = {"rest", "row-locked", "regression", "row-locked-single"})
    @Description("POST /users using a claimed row's name/job")
    public void shouldCreateUserUsingClaimedRow() {
        UserPoolRow row = claimedRow();
        User request = User.builder().name(row.name()).job(row.job()).build();

        restService().createUserAndVerify(request,
                ResponseExpectation.status(201)
                        .andBodyValueEquals("name", row.name())
                        .andBodyValueEquals("job", row.job())
                        .andBodyValuePresent("id")
                        .andBodyValuePresent("createdAt"));
    }

    @Test(groups = {"rest", "row-locked", "regression", "row-locked-single"})
    @Description("PUT /users/{id} at a claimed row's userId with the row's job")
    public void shouldUpdateUserUsingClaimedRow() {
        UserPoolRow row = claimedRow();
        String rawJsonBody = "{\"job\":\"" + row.job() + "\"}";

        restService().updateUserRawAndVerify(row.userId(), "PUT", rawJsonBody,
                ResponseExpectation.status(200)
                        .andBodyValueEquals("job", row.job())
                        .andBodyValuePresent("updatedAt"));
    }

    @Test(groups = {"rest", "row-locked", "regression", "row-locked-single"})
    @Description("DELETE /users/{id} at a claimed row's userId")
    public void shouldDeleteUserUsingClaimedRow() {
        UserPoolRow row = claimedRow();
        restService().deleteUserAndVerify(row.userId(), ResponseExpectation.status(204));
    }

    @Test(groups = {"rest", "row-locked", "register", "regression", "row-locked-single"})
    @Description("POST /register using a claimed row's email/registerPassword")
    public void shouldRegisterUsingClaimedRow() {
        UserPoolRow row = claimedRow();
        String rawJsonBody = "{\"email\":\"" + row.email() + "\",\"password\":\"" + row.registerPassword()
                + "\"}";

        restService().registerRawAndVerify(rawJsonBody,
                ResponseExpectation.status(200)
                        .andBodyValuePresent("id")
                        .andBodyValuePresent("token"));
    }

    @Test(groups = {"rest", "row-locked", "login", "regression", "row-locked-single"})
    @Description("POST /login using a claimed row's email/loginPassword")
    public void shouldLoginUsingClaimedRow() {
        UserPoolRow row = claimedRow();
        String rawJsonBody = "{\"email\":\"" + row.email() + "\",\"password\":\"" + row.loginPassword() + "\"}";

        restService().loginRawAndVerify(rawJsonBody,
                ResponseExpectation.status(200).andBodyValuePresent("token"));
    }

    @Test(groups = {"rest", "row-locked", "regression", "row-locked-single"}, expectedExceptions = AssertionError.class)
    @Description("A row claimed by @BeforeMethod is still released by @AfterMethod once this test fails - "
            + "the release-side proof is in shouldConfirmRowReleasedAfterFailingClaimTest")
    public void shouldReleaseRowBackToPoolEvenWhenClaimBlockFails() {
        LAST_FAILURE_TEST_CLAIM.set(CLAIMED_ROW.get());
        // deliberately fail while the row is still held, to prove @AfterMethod still runs and releases it
        Assert.fail("deliberate failure inside claim scope to verify release-on-failure");
    }

    @Test(groups = {"rest", "row-locked", "regression"},
            dependsOnMethods = "shouldReleaseRowBackToPoolEvenWhenClaimBlockFails", alwaysRun = true)
    @Description("Confirms the exact claim held by the failing test above was released by its @AfterMethod - "
            + "checks that claim's own release state, not pool/row-key membership, so a legitimate reclaim of "
            + "the same row (including by this test's own @BeforeMethod) can never produce a false failure")
    public void shouldConfirmRowReleasedAfterFailingClaimTest() {
        Assert.assertTrue(LAST_FAILURE_TEST_CLAIM.get().isReleased(),
                "row was not released back to the pool after the failing test's teardown ran");
    }
}
