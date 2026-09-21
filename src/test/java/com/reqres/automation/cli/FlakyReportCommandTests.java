package com.reqres.automation.cli;

import com.reqres.automation.utils.FlakyTestDetector;
import com.reqres.automation.utils.RunHistoryStore;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Story("CLI flaky-report command")
public class FlakyReportCommandTests {

    private static final String SCENARIO_A = "com.example.Tests.methodA";
    private static final String SCENARIO_B = "com.example.Tests.methodB";

    private static final File SUMMARY_FILE = new File("target/test-result-summary.json");
    private static final File TEST_OUTPUT_DIR = new File("test-output");

    private Path historyDirectory;

    @AfterMethod
    public void cleanUpHistoryDirectory() throws IOException {
        if (historyDirectory != null) {
            try (var paths = Files.walk(historyDirectory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(FlakyReportCommandTests::deleteQuietly);
            }
        }
    }

    @Test(groups = {"utils", "regression"})
    @Description("flaky-report prints exactly the scenario ids FlakyTestDetector.computeFlakyScenarios flags, "
            + "given the same run history, and triggers no test execution")
    public void shouldPrintFlakyScenariosMatchingDetectorDirectly() throws Exception {
        historyDirectory = Files.createTempDirectory("flaky-report-history");
        RunHistoryStore store = new RunHistoryStore(historyDirectory);
        store.persist(Map.of(SCENARIO_A, true, SCENARIO_B, true));
        store.persist(Map.of(SCENARIO_A, false, SCENARIO_B, true));

        long summaryBefore = SUMMARY_FILE.exists() ? SUMMARY_FILE.lastModified() : -1;
        boolean testOutputExistedBefore = TEST_OUTPUT_DIR.exists();

        Set<String> printed = CliOutputCapture.capturePrintedLines(new FlakyReportCommand(store));

        List<Map<String, Boolean>> outcomeSets = store.load();
        Set<String> expected = FlakyTestDetector.computeFlakyScenarios(
                outcomeSets.get(0), outcomeSets.subList(1, outcomeSets.size()));

        Assert.assertEquals(printed, expected);
        long summaryAfter = SUMMARY_FILE.exists() ? SUMMARY_FILE.lastModified() : -1;
        Assert.assertEquals(summaryAfter, summaryBefore, "call() must not trigger a test run");
        Assert.assertEquals(TEST_OUTPUT_DIR.exists(), testOutputExistedBefore, "call() must not trigger a test run");
    }

    @Test(groups = {"utils", "regression"})
    @Description("flaky-report prints nothing and returns 0 when fewer than two runs are stored")
    public void shouldPrintNothingWithFewerThanTwoStoredRuns() throws Exception {
        historyDirectory = Files.createTempDirectory("flaky-report-single-run");
        RunHistoryStore store = new RunHistoryStore(historyDirectory);
        store.persist(Map.of(SCENARIO_A, true));

        Set<String> printed = CliOutputCapture.capturePrintedLines(new FlakyReportCommand(store));

        Assert.assertTrue(printed.isEmpty(), "Expected no output, got: " + printed);
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // best-effort cleanup, nothing to act on if it fails
        }
    }
}
