package com.reqres.automation.cli;

import com.reqres.automation.utils.CliHtmlReportWriter;
import com.reqres.automation.utils.Constants;
import com.reqres.automation.utils.TestImpactAnalyzer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/** Prints the test classes impacted by a set of changed areas, per {@link TestImpactAnalyzer}, and
 * writes the same result to an HTML report. With no {@code --changed-area} given, falls back to
 * every test class in {@code src/main/resources/testimpact/dependency-graph.json}. */
@Command(name = "impact-analysis", description = "Print the test classes impacted by the given changed area(s)")
public final class ImpactAnalysisCommand implements Callable<Integer> {

    @Option(names = "--changed-area", split = ",",
            description = "Changed area name(s), comma-separated or repeated. Defaults to every test class in "
                    + "the dependency graph when omitted.")
    private List<String> changedAreas;

    @Override
    public Integer call() {
        TestImpactAnalyzer analyzer = new TestImpactAnalyzer();
        Set<String> impacted = (changedAreas == null || changedAreas.isEmpty())
                ? analyzer.getAllTestClasses()
                : analyzer.getTestcasesImpacted(changedAreas);

        impacted.forEach(System.out::println);
        CliHtmlReportWriter.write(Path.of(Constants.IMPACT_ANALYSIS_REPORT_FILE),
                "Impact Analysis Report", new TreeSet<>(impacted));
        return 0;
    }
}
