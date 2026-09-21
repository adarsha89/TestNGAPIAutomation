package com.reqres.automation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reqres.automation.utils.FlakyTestDetector;
import com.reqres.automation.utils.RunHistoryStore;
import com.reqres.automation.utils.TestResultAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Feeds every TestNG result into a {@link TestResultAggregator} (pass/fail/
 * skip counts) and a {@link FlakyTestDetector} (this run's scenario
 * outcomes), then on {@code onFinish} compares this run against prior runs
 * persisted via {@link RunHistoryStore} and writes the aggregate, flaky
 * scenarios, and this run's outcomes to {@code target/test-result-summary.json}.
 */
public class CustomTestNGListener implements ITestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTestNGListener.class);
    private static final String SUMMARY_FILE_PATH = "target/test-result-summary.json";

    /** System property to override {@link RunHistoryStore}'s default directory in tests. */
    private static final String RUN_HISTORY_DIRECTORY_PROPERTY = "runHistoryStore.directory";

    private final TestResultAggregator aggregator = new TestResultAggregator();
    private final FlakyTestDetector flakyTestDetector = new FlakyTestDetector();
    private final RunHistoryStore runHistoryStore = createRunHistoryStore();

    @Override
    public void onTestSuccess(ITestResult result) {
        recordResult(result, TestResultAggregator.Status.PASS, true);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        recordResult(result, TestResultAggregator.Status.FAIL, false);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        recordResult(result, TestResultAggregator.Status.SKIP, false);
    }

    @Override
    public void onFinish(ITestContext context) {
        Map<String, Boolean> currentOutcomes = flakyTestDetector.getCurrentOutcomes();
        Set<String> flakyScenarios = FlakyTestDetector.computeFlakyScenarios(currentOutcomes, runHistoryStore.load());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("counts", aggregator.snapshot());
        summary.put("flakyTests", flakyScenarios);
        summary.put("scenarioOutcomes", currentOutcomes);

        File summaryFile = new File(SUMMARY_FILE_PATH);
        File parentDir = summaryFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
        try {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(summaryFile, summary);
        } catch (IOException e) {
            LOGGER.warn("Failed to write test result summary to {}", SUMMARY_FILE_PATH, e);
        }

        runHistoryStore.persist(currentOutcomes);
    }

    private void recordResult(ITestResult result, TestResultAggregator.Status status, boolean passed) {
        String testName = result.getMethod().getQualifiedName();
        aggregator.record(testName, status);
        flakyTestDetector.recordResult(scenarioId(result, testName), passed);
    }

    /** Method name plus parameters, so different data-provider rows of the same method are
     * tracked as distinct scenarios rather than sharing one history. */
    private String scenarioId(ITestResult result, String testName) {
        Object[] parameters = result.getParameters();
        if (parameters == null || parameters.length == 0) {
            return testName;
        }
        return testName + "::" + Arrays.deepToString(parameters);
    }

    private static RunHistoryStore createRunHistoryStore() {
        String overrideDirectory = System.getProperty(RUN_HISTORY_DIRECTORY_PROPERTY);
        return overrideDirectory == null ? new RunHistoryStore() : new RunHistoryStore(Path.of(overrideDirectory));
    }

    /** Test-support accessor for {@code TestResultAggregatorIntegrationTests}. */
    TestResultAggregator getAggregator() {
        return aggregator;
    }

    /** Test-support accessor for {@code FlakyTestDetectorTests}' listener-integration case. */
    FlakyTestDetector getFlakyTestDetector() {
        return flakyTestDetector;
    }
}
