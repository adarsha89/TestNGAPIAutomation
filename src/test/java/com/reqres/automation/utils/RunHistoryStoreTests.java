package com.reqres.automation.utils;

import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

/**
 * Tests {@link RunHistoryStore#persist(Map)} directly against the filesystem, independent of
 * any CLI command's printed output (see {@code FlakyReportCommandTests} for that integration).
 */
@Story("RunHistoryStore persisted-run filename uniqueness")
public class RunHistoryStoreTests {

    private static final String SCENARIO_A = "com.example.Tests.methodA";

    private Path historyDirectory;

    @AfterMethod
    public void cleanUpHistoryDirectory() throws IOException {
        if (historyDirectory != null) {
            try (var paths = Files.walk(historyDirectory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(RunHistoryStoreTests::deleteQuietly);
            }
        }
    }

    @Test(groups = {"utils", "regression"})
    @Description("Two persist() calls on the same store produce two distinct run-*.json files on disk, "
            + "even when issued back-to-back")
    public void shouldWriteDistinctFilesForConsecutivePersistCalls() throws IOException {
        historyDirectory = Files.createTempDirectory("run-history-store-collision");
        RunHistoryStore store = new RunHistoryStore(historyDirectory);

        store.persist(Map.of(SCENARIO_A, true));
        store.persist(Map.of(SCENARIO_A, false));

        try (var runFiles = Files.list(historyDirectory)) {
            long count = runFiles.filter(p -> p.getFileName().toString().startsWith("run-")).count();
            Assert.assertEquals(count, 2, "Expected each persist() call to write a distinct file");
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // best-effort cleanup, nothing to act on if it fails
        }
    }
}
