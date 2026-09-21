package com.reqres.automation.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Entry point for on-demand tasks invoked via {@code exec:java}, e.g.:
 * {@code ./mvnw exec:java -Dexec.args="impact-analysis --changed-area=users"}
 * {@code ./mvnw exec:java -Dexec.args="flaky-report"}
 * Neither subcommand runs any tests - both are read-only reporting tasks.
 */
@Command(name = "reqres-automation", subcommands = {ImpactAnalysisCommand.class, FlakyReportCommand.class})
public final class CliMain {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CliMain()).execute(args);
        System.exit(exitCode);
    }
}
