package com.reqres.automation.fixtures;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Minimal fixture used only by {@code FlakyTestDetectorTests}, driven via a
 * standalone TestNG subprocess run (see {@code NestedRunSandbox}) - never
 * registered in {@code testng.xml}. Its single invocation's outcome is
 * controlled by the {@code flappy.shouldPass} system property (default
 * {@code true}), so successive separate subprocess runs can produce a pass
 * then a fail for the same scenario id - proving cross-run flaky detection,
 * not just same-run variation. See {@link CountingFixture}'s Javadoc for why
 * this lives in its own top-level file rather than nested inside a
 * {@code *Tests.java} file.
 */
public final class FlappyFixture {

    @Test
    public void flappy() {
        boolean shouldPass = Boolean.parseBoolean(System.getProperty("flappy.shouldPass", "true"));
        Assert.assertTrue(shouldPass, "Deliberately toggled outcome for flaky-detection testing");
    }
}
