package com.reqres.automation.clients.rest;

import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Framework self-test for {@link ProvidedIdStore}'s mechanics directly, bypassing HTTP entirely -
 * see {@code ProvidedIdMockApiTests}/{@code ProvidedIdMockApiConcurrencyTests} for the HTTP-level
 * coverage that exercises the same store through {@link ProvidedIdResponseTransformer}. Each
 * sequential test ({@code shouldRecordNeverSeenId}/{@code shouldRejectAlreadyRecordedId}/
 * {@code shouldPersistAcrossNewInstanceAtSameFile}) gets its own temp directory so runs never
 * interact; the concurrency test ({@code shouldRecordExactlyOneWinnerUnderConcurrentAttempts})
 * shares one store instance across 8 parallel invocations, mirroring
 * {@code RowLockedConcurrencyDataProvider}'s "N identical parallel rows against a shared
 * singleton" shape.
 */
@Story("ProvidedIdStore CSV-backed duplicate-check mechanics")
public class ProvidedIdStoreTests {

    private Path tempDirectory;

    private static Path sharedConcurrencyDirectory;
    private static ProvidedIdStore sharedConcurrencyStore;
    private static String sharedConcurrencyId;
    private static CountDownLatch concurrentAttemptsLatch;
    private static final AtomicInteger RECORDED_COUNT = new AtomicInteger();
    private static final AtomicInteger REJECTED_COUNT = new AtomicInteger();
    private static final AtomicBoolean CONCURRENCY_CLEANUP_CLAIMED = new AtomicBoolean();

    @AfterMethod(alwaysRun = true)
    public void cleanUpTempDirectory() throws IOException {
        deleteRecursively(tempDirectory);
        tempDirectory = null;
    }

    @Test(priority = 3, groups = {"mock", "regression"})
    @Description("recordIfAbsent on a never-seen ID returns true, and isRecorded now reports true for it")
    public void shouldRecordNeverSeenId() throws IOException {
        tempDirectory = Files.createTempDirectory("provided-id-store-record");
        ProvidedIdStore store = ProvidedIdStore.at(tempDirectory.resolve("provided-ids.csv"));
        String id = "never-seen-" + UUID.randomUUID();

        Assert.assertTrue(store.recordIfAbsent(id), "Expected a never-seen ID to be recorded");
        Assert.assertTrue(store.isRecorded(id), "Expected the ID to be reported as recorded immediately after");
    }

    @Test(priority = 3, groups = {"mock", "negative", "regression"})
    @Description("recordIfAbsent on an already-recorded ID returns false and leaves the original record unchanged")
    public void shouldRejectAlreadyRecordedId() throws IOException {
        tempDirectory = Files.createTempDirectory("provided-id-store-reject");
        ProvidedIdStore store = ProvidedIdStore.at(tempDirectory.resolve("provided-ids.csv"));
        String id = "already-seen-" + UUID.randomUUID();

        Assert.assertTrue(store.recordIfAbsent(id), "Expected the first call to record the ID");
        Assert.assertFalse(store.recordIfAbsent(id), "Expected a second call for the same ID to be rejected");
        Assert.assertTrue(store.isRecorded(id), "Expected the original record to remain in place");
    }

    @Test(priority = 3, groups = {"mock", "regression"})
    @Description("A new ProvidedIdStore.at(path) instance pointed at the same file after a prior instance "
            + "recorded an ID reports that ID as already recorded without it being resubmitted")
    public void shouldPersistAcrossNewInstanceAtSameFile() throws IOException {
        tempDirectory = Files.createTempDirectory("provided-id-store-persist");
        Path file = tempDirectory.resolve("provided-ids.csv");
        String id = "durable-" + UUID.randomUUID();

        ProvidedIdStore firstInstance = ProvidedIdStore.at(file);
        Assert.assertTrue(firstInstance.recordIfAbsent(id), "Expected the first instance to record the ID");

        ProvidedIdStore secondInstance = ProvidedIdStore.at(file);
        Assert.assertTrue(secondInstance.isRecorded(id),
                "Expected a new instance at the same file to see the ID as already recorded");
    }

    @DataProvider(name = "concurrentRecordAttempts", parallel = true)
    public Object[][] concurrentRecordAttempts() throws IOException {
        sharedConcurrencyDirectory = Files.createTempDirectory("provided-id-store-concurrency");
        sharedConcurrencyStore = ProvidedIdStore.at(sharedConcurrencyDirectory.resolve("provided-ids.csv"));
        sharedConcurrencyId = "concurrent-" + UUID.randomUUID();
        concurrentAttemptsLatch = new CountDownLatch(8);
        RECORDED_COUNT.set(0);
        REJECTED_COUNT.set(0);
        CONCURRENCY_CLEANUP_CLAIMED.set(false);
        return new Object[][]{{}, {}, {}, {}, {}, {}, {}, {}};
    }

    // dataProviderThreadCount isn't a @Test attribute on TestNG 7.9.0; parallel invocations come from
    // @DataProvider(parallel = true) plus data-provider-thread-count="8" in testng.xml (see RowLockedConcurrencyTests).
    // Every one of the 8 parallel invocations makes its own recordIfAbsent attempt, then waits on a shared
    // latch for all 8 to finish before asserting the aggregate tally - keeps the tally assertion inside this
    // one @Test method (no separate dependent method needed, unlike the HTTP-level TC-5a/b pair).
    @Test(priority = 3, groups = {"mock", "concurrency", "regression"}, dataProvider = "concurrentRecordAttempts")
    @Description("8 parallel recordIfAbsent(\"same-id\") calls against one shared store instance resolve to "
            + "exactly one true and seven false")
    public void shouldRecordExactlyOneWinnerUnderConcurrentAttempts() throws IOException {
        boolean recorded = sharedConcurrencyStore.recordIfAbsent(sharedConcurrencyId);
        if (recorded) {
            RECORDED_COUNT.incrementAndGet();
        } else {
            REJECTED_COUNT.incrementAndGet();
        }
        concurrentAttemptsLatch.countDown();

        boolean allAttemptsFinished = awaitLatch(concurrentAttemptsLatch);
        Assert.assertTrue(allAttemptsFinished,
                "Timed out waiting for all 8 concurrent recordIfAbsent attempts to finish");
        Assert.assertEquals(RECORDED_COUNT.get(), 1,
                "Expected exactly one of the 8 concurrent recordIfAbsent attempts to succeed");
        Assert.assertEquals(REJECTED_COUNT.get(), 7,
                "Expected exactly seven of the 8 concurrent recordIfAbsent attempts to be rejected");

        if (CONCURRENCY_CLEANUP_CLAIMED.compareAndSet(false, true)) {
            deleteRecursively(sharedConcurrencyDirectory);
            sharedConcurrencyStore = null;
        }
    }

    private static boolean awaitLatch(CountDownLatch latch) {
        try {
            return latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent recordIfAbsent attempts", e);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(ProvidedIdStoreTests::deleteQuietly);
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
