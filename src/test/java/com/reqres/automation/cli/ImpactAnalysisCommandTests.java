package com.reqres.automation.cli;

import com.reqres.automation.utils.TestImpactAnalyzer;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.Set;

@Story("CLI impact-analysis command")
public class ImpactAnalysisCommandTests {

    private static final File SUMMARY_FILE = new File("target/test-result-summary.json");
    private static final File TEST_OUTPUT_DIR = new File("test-output");

    @Test(groups = {"utils", "regression"})
    @Description("impact-analysis prints exactly the test names TestImpactAnalyzer.getTestcasesImpacted "
            + "returns for the same changed area, and triggers no test execution")
    public void shouldPrintImpactedTestsMatchingAnalyzerDirectly() throws Exception {
        long summaryBefore = SUMMARY_FILE.exists() ? SUMMARY_FILE.lastModified() : -1;
        boolean testOutputExistedBefore = TEST_OUTPUT_DIR.exists();

        ImpactAnalysisCommand command = CommandLine.populateCommand(new ImpactAnalysisCommand(),
                "--changed-area=users");

        Set<String> printed = CliOutputCapture.capturePrintedLines(command);
        Set<String> expected = new TestImpactAnalyzer().getTestcasesImpacted(List.of("users"));

        Assert.assertEquals(printed, expected);
        long summaryAfter = SUMMARY_FILE.exists() ? SUMMARY_FILE.lastModified() : -1;
        Assert.assertEquals(summaryAfter, summaryBefore, "call() must not trigger a test run");
        Assert.assertEquals(TEST_OUTPUT_DIR.exists(), testOutputExistedBefore, "call() must not trigger a test run");
    }

    @Test(groups = {"utils", "regression"})
    @Description("impact-analysis with no --changed-area prints every test class in the dependency graph")
    public void shouldPrintAllTestsWhenChangedAreaOmitted() throws Exception {
        ImpactAnalysisCommand command = CommandLine.populateCommand(new ImpactAnalysisCommand());

        Set<String> printed = CliOutputCapture.capturePrintedLines(command);
        Set<String> expected = new TestImpactAnalyzer().getAllTestClasses();

        Assert.assertEquals(printed, expected);
    }
}
