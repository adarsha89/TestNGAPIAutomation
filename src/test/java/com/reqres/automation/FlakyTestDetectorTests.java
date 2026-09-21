package com.reqres.automation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reqres.automation.fixtures.FlappyFixture;
import com.reqres.automation.utils.FlakyTestDetector;
import com.reqres.automation.utils.RunHistoryStore;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Direct unit coverage of {@link FlakyTestDetector#computeFlakyScenarios} and
 * {@link RunHistoryStore}, plus a real cross-run integration case proving
 * {@link CustomTestNGListener} persists and reads history across separate
 * JVM runs (not just its own unit logic).
 */
@Story("Cross-run flaky-test detection")
public class FlakyTestDetectorTests {

    private static final String SCENARIO_A = "com.example.Tests.methodA";
    private static final String SCENARIO_B = "com.example.Tests.methodB";

    @Test(groups = {"utils", "regression"})
    @Description("A scenario that passes in every run (current + priors) is never flagged flaky")
    public void shouldNotFlagAlwaysPassingScenario() {
        Set<String> flaky = FlakyTestDetector.computeFlakyScenarios(
                Map.of(SCENARIO_A, true),
                List.of(Map.of(SCENARIO_A, true), Map.of(SCENARIO_A, true)));

        Assert.assertTrue(flaky.isEmpty(), "Expected no flaky scenarios, got: " + flaky);
    }

    @Test(groups = {"utils", "regression"})
    @Description("A scenario that fails in every run (current + priors) is never flagged flaky")
    public void shouldNotFlagAlwaysFailingScenario() {
        Set<String> flaky = FlakyTestDetector.computeFlakyScenarios(
                Map.of(SCENARIO_A, false),
                List.of(Map.of(SCENARIO_A, false), Map.of(SCENARIO_A, false)));

        Assert.assertTrue(flaky.isEmpty(), "Expected no flaky scenarios, got: " + flaky);
    }

    @Test(groups = {"utils", "regression"})
    @Description("A scenario with a mix of pass and fail across current + prior runs is flagged flaky")
    public void shouldFlagInconsistentScenarioAcrossRuns() {
        Set<String> flaky = FlakyTestDetector.computeFlakyScenarios(
                Map.of(SCENARIO_A, true),
                List.of(Map.of(SCENARIO_A, false)));

        Assert.assertEquals(flaky, Set.of(SCENARIO_A));
    }

    @Test(groups = {"utils", "regression"})
    @Description("A scenario with no prior history is never flagged flaky on that basis alone")
    public void shouldNotFlagScenarioWithNoPriorHistory() {
        Set<String> flaky = FlakyTestDetector.computeFlakyScenarios(Map.of(SCENARIO_A, false), List.of());

        Assert.assertTrue(flaky.isEmpty(), "Expected no flaky scenarios, got: " + flaky);
    }

    @Test(groups = {"utils", "regression"})
    @Description("Two distinct scenarios (different data-provider rows of the same method, keyed by method + "
            + "parameters) each with their own consistent history are tracked independently - neither is flagged "
            + "flaky just because the other one's outcome differs")
    public void shouldKeyScenariosByMethodAndParametersIndependently() {
        Map<String, Boolean> current = Map.of(SCENARIO_A, true, SCENARIO_B, false);
        List<Map<String, Boolean>> priors = List.of(Map.of(SCENARIO_A, true, SCENARIO_B, false));

        Set<String> flaky = FlakyTestDetector.computeFlakyScenarios(current, priors);

        Assert.assertTrue(flaky.isEmpty(), "Expected no flaky scenarios, got: " + flaky);
    }

    @Test(groups = {"utils", "regression"})
    @Description("RunHistoryStore.load() returns an empty history (no exception) when the directory doesn't exist")
    public void shouldReturnEmptyHistoryForMissingDirectory() throws IOException {
        Path missingDirectory = Files.createTempDirectory("run-history-missing").resolve("does-not-exist");
        RunHistoryStore store = new RunHistoryStore(missingDirectory);

        Assert.assertTrue(store.load().isEmpty());
    }

    @Test(groups = {"utils", "regression"})
    @Description("RunHistoryStore.load() skips an unreadable/malformed prior-run file rather than throwing")
    public void shouldSkipMalformedPriorRunFile() throws IOException {
        Path directory = Files.createTempDirectory("run-history-malformed");
        Files.writeString(directory.resolve("run-bad.json"), "not valid json {{{", StandardCharsets.UTF_8);

        RunHistoryStore store = new RunHistoryStore(directory);

        Assert.assertTrue(store.load().isEmpty());
    }

    @Test(groups = {"utils", "regression"})
    @Description("A real standalone TestNG run of FlappyFixture (separate JVM, see NestedRunSandbox) that passes, "
            + "followed by a second separate run that fails - both pointed at the same isolated previousTestResults "
            + "directory via a system-property override - has the second run's summary flag the scenario as flaky, "
            + "proving CustomTestNGListener really persists and reads history across JVM runs")
    public void shouldFlagScenarioFlakyAcrossSeparateSubprocessRuns() throws IOException {
        Path historyDirectory = Files.createTempDirectory("flaky-cross-run-history");

        Path firstRunWorkingDir = NestedRunSandbox.runFixtureInSubprocess(FlappyFixture.class,
                Map.of("flappy.shouldPass", "true", "runHistoryStore.directory", historyDirectory.toString()));
        assertSummaryWasWritten(firstRunWorkingDir);

        Path secondRunWorkingDir = NestedRunSandbox.runFixtureInSubprocess(FlappyFixture.class,
                Map.of("flappy.shouldPass", "false", "runHistoryStore.directory", historyDirectory.toString()));
        File summaryFile = assertSummaryWasWritten(secondRunWorkingDir);

        JsonNode flakyTests = new ObjectMapper().readTree(summaryFile).get("flakyTests");
        String expectedQualifiedName = FlappyFixture.class.getName() + ".flappy";

        boolean reportedFlaky = false;
        for (JsonNode flakyTestName : flakyTests) {
            if (expectedQualifiedName.equals(flakyTestName.asText())) {
                reportedFlaky = true;
            }
        }
        Assert.assertTrue(reportedFlaky,
                "Expected the second run's summary to flag '" + expectedQualifiedName + "' as flaky "
                        + "after a pass then a fail across separate runs, actual flakyTests: " + flakyTests);
    }

    private File assertSummaryWasWritten(Path workingDir) {
        File summaryFile = workingDir.resolve("target/test-result-summary.json").toFile();
        Assert.assertTrue(summaryFile.isFile(),
                "Expected CustomTestNGListener#onFinish to have written " + summaryFile);
        return summaryFile;
    }
}
