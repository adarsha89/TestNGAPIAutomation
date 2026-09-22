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

    // System.out is process-global; classes in this package can run concurrently under
    // parallel="classes", so the redirect/restore must be serialized or one test's capture
    // window can pick up another thread's output (or clobber the other's restore).
    private static final Object SYSTEM_OUT_LOCK = new Object();

    static Set<String> capturePrintedLines(Callable<Integer> command) throws Exception {
        synchronized (SYSTEM_OUT_LOCK) {
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
}
