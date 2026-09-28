package com.reqres.automation.cli;

import com.reqres.automation.utils.CliHtmlReportWriter;
import com.reqres.automation.utils.Constants;
import com.reqres.automation.utils.FlakyTestDetector;
import com.reqres.automation.utils.RunHistoryStore;
import picocli.CommandLine.Command;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * Prints the scenarios the cross-run comparator would flag as flaky, without
 * running any tests, and writes the same result to an HTML report. With no
 * live run to compare against, the most-recently persisted {@code
 * run-*.json} file stands in as "current" and every other stored file as
 * "priors" - the same comparator a live run's {@code onFinish} would use
 * given that same history.
 */
@Command(name = "flaky-report", description = "Print scenarios flagged flaky from stored run history")
public final class FlakyReportCommand implements Callable<Integer> {

    private final RunHistoryStore runHistoryStore;

    FlakyReportCommand() {
        this(new RunHistoryStore());
    }

    FlakyReportCommand(RunHistoryStore runHistoryStore) {
        this.runHistoryStore = runHistoryStore;
    }

    @Override
    public Integer call() {
        Set<String> flaky = findFlakyScenarios(runHistoryStore.load());
        flaky.forEach(System.out::println);
        CliHtmlReportWriter.write(Path.of(Constants.FLAKY_REPORT_FILE), "Flaky Test Report", new TreeSet<>(flaky));
        return 0;
    }

    // Needs a current run plus at least one prior to compare against.
    private static Set<String> findFlakyScenarios(List<Map<String, Boolean>> outcomeSets) {
        if (outcomeSets.size() < 2) {
            return Set.of();
        }
        Map<String, Boolean> current = outcomeSets.get(0);
        List<Map<String, Boolean>> priors = outcomeSets.subList(1, outcomeSets.size());
        return FlakyTestDetector.computeFlakyScenarios(current, priors);
    }
}
