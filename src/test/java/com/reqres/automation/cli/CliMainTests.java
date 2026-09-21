package com.reqres.automation.cli;

import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.util.Map;

/** Wiring sanity for {@link CliMain}'s subcommand registration - not {@code main()} itself, since
 * that calls {@code System.exit}. */
@Story("CLI subcommand wiring")
public class CliMainTests {

    @Test(groups = {"utils", "regression"})
    @Description("CliMain registers both subcommands under their expected names")
    public void shouldRegisterBothSubcommands() {
        CommandLine commandLine = new CommandLine(new CliMain());

        Map<String, CommandLine> subcommands = commandLine.getSubcommands();

        Assert.assertTrue(subcommands.containsKey("impact-analysis"));
        Assert.assertTrue(subcommands.containsKey("flaky-report"));
    }

    @Test(groups = {"utils", "regression"})
    @Description("Invoking impact-analysis without --changed-area falls back to the full dependency graph and "
            + "succeeds")
    public void shouldSucceedWhenChangedAreaOmitted() {
        int exitCode = new CommandLine(new CliMain()).execute("impact-analysis");

        Assert.assertEquals(exitCode, 0);
    }
}
