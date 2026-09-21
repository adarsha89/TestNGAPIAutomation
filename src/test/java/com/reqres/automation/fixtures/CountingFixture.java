package com.reqres.automation.fixtures;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Minimal fixture suite used only by
 * {@code TestResultAggregatorIntegrationTests}, driven via a standalone
 * TestNG subprocess run (see {@code NestedRunSandbox}) - never registered in
 * {@code testng.xml}. Named/packaged so its file name doesn't match
 * Surefire's default test-class discovery pattern ({@code **&#47;*Tests.java}):
 * nesting it inside a {@code *Tests.java} file makes Surefire also run it
 * directly as part of the real suite, breaking the build on its deliberate
 * failure.
 */
public final class CountingFixture {

    @Test
    public void passOne() {
        Assert.assertTrue(true);
    }

    @Test
    public void passTwo() {
        Assert.assertTrue(true);
    }

    @Test
    public void failOne() {
        Assert.fail("Deliberate failure to exercise CustomTestNGListener#onTestFailure");
    }
}
