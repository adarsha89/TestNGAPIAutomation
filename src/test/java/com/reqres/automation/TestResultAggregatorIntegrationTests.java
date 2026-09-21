package com.reqres.automation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reqres.automation.fixtures.CountingFixture;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Drives a standalone TestNG run (a separate JVM, see
 * {@link NestedRunSandbox}) of {@link CountingFixture} through a real
 * {@link CustomTestNGListener} and asserts the
 * {@code target/test-result-summary.json} it wrote matches the run's actual
 * outcomes. {@link CountingFixture} isn't registered in {@code testng.xml},
 * so it never runs as part of the main suite - only this test drives it.
 */
@Story("Real-time test result aggregation via the TestNG listener")
public class TestResultAggregatorIntegrationTests {

    @Test(groups = {"utils", "regression"})
    @Description("A real, standalone TestNG run of a small fixture (2 passing, 1 failing test method) drives "
            + "CustomTestNGListener's aggregator, whose written summary exactly matches the actual outcomes, each "
            + "test counted exactly once")
    public void shouldAggregateExactCountsFromARealRun() throws IOException {
        Path workingDir = NestedRunSandbox.runFixtureInSubprocess(CountingFixture.class);

        File summaryFile = workingDir.resolve("target/test-result-summary.json").toFile();
        Assert.assertTrue(summaryFile.isFile(),
                "Expected CustomTestNGListener#onFinish to have written " + summaryFile);

        JsonNode counts = new ObjectMapper().readTree(summaryFile).get("counts");

        Assert.assertEquals(counts.get("PASS").asLong(), 2L);
        Assert.assertEquals(counts.get("FAIL").asLong(), 1L);
        Assert.assertEquals(counts.get("SKIP").asLong(), 0L);
    }
}
