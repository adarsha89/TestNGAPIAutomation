package com.reqres.automation.cli;

import org.testng.Assert;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/** Shared output-capture helper for CLI command tests in this package. */
final class CliOutputCapture {

    private CliOutputCapture() {
    }

    static Set<String> capturePrintedLines(Callable<Integer> command) throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(capturedOut));
            Integer exitCode = command.call();
            Assert.assertEquals(exitCode, Integer.valueOf(0));
        } finally {
            System.setOut(originalOut);
        }
        String output = capturedOut.toString();
        return output.isBlank() ? Set.of() : output.lines().collect(Collectors.toSet());
    }
}
