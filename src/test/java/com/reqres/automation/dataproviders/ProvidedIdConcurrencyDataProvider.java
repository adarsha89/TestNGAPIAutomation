package com.reqres.automation.dataproviders;

import com.reqres.automation.testdata.ProvidedIdDataBuilder;
import org.testng.annotations.DataProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Feeds {@code ProvidedIdMockApiConcurrencyTests}' two parallel scenarios - 5 distinct fresh IDs
 * (TC-4a) and 8 copies of one shared fresh ID (TC-5a) - and tallies the same-ID scenario's raw
 * outcomes, mirroring {@code RowLockedConcurrencyDataProvider}'s "N identical parallel rows
 * against a shared singleton" shape instead of inventing raw {@code ExecutorService}/{@code
 * Thread} code in a test.
 */
public final class ProvidedIdConcurrencyDataProvider {

    private static volatile List<String> lastGeneratedDistinctIds = List.of();
    private static volatile String lastGeneratedSameId;

    private static final AtomicInteger SUCCESS_COUNT = new AtomicInteger();
    private static final AtomicInteger DUPLICATE_COUNT = new AtomicInteger();
    private static final AtomicInteger UNEXPECTED_COUNT = new AtomicInteger();

    private ProvidedIdConcurrencyDataProvider() {
    }

    @DataProvider(name = "distinctIdConcurrentSubmissions", parallel = true)
    public static Object[][] distinctIdConcurrentSubmissions() {
        List<String> ids = new ArrayList<>(5);
        Object[][] rows = new Object[5][];
        for (int i = 0; i < 5; i++) {
            String id = ProvidedIdDataBuilder.uniqueId();
            ids.add(id);
            rows[i] = new Object[]{id};
        }
        lastGeneratedDistinctIds = Collections.unmodifiableList(ids);
        return rows;
    }

    @DataProvider(name = "sameIdConcurrentSubmissions", parallel = true)
    public static Object[][] sameIdConcurrentSubmissions() {
        resetCounters();
        String sharedId = ProvidedIdDataBuilder.uniqueId();
        lastGeneratedSameId = sharedId;
        Object[][] rows = new Object[8][];
        for (int i = 0; i < 8; i++) {
            rows[i] = new Object[]{sharedId};
        }
        return rows;
    }

    public static List<String> lastGeneratedDistinctIds() {
        return lastGeneratedDistinctIds;
    }

    public static String lastGeneratedSameId() {
        return lastGeneratedSameId;
    }

    public static void resetCounters() {
        SUCCESS_COUNT.set(0);
        DUPLICATE_COUNT.set(0);
        UNEXPECTED_COUNT.set(0);
    }

    public static void recordSuccess() {
        SUCCESS_COUNT.incrementAndGet();
    }

    public static void recordDuplicate() {
        DUPLICATE_COUNT.incrementAndGet();
    }

    public static void recordUnexpected(int status) {
        UNEXPECTED_COUNT.incrementAndGet();
    }

    public static int successCount() {
        return SUCCESS_COUNT.get();
    }

    public static int duplicateCount() {
        return DUPLICATE_COUNT.get();
    }

    public static int unexpectedCount() {
        return UNEXPECTED_COUNT.get();
    }
}
