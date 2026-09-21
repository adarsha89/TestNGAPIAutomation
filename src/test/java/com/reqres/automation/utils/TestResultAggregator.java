package com.reqres.automation.utils;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Real-time pass/fail/skip counter with an instance API
 * ({@link #record(String, Status)} / {@link #snapshot()}) that a TestNG
 * listener can feed as results arrive. Thread-safe for parallel
 * ("classes", thread-count=4) runs.
 */
public final class TestResultAggregator {

    public enum Status {
        PASS,
        FAIL,
        SKIP
    }

    private final Map<Status, LongAdder> countsByStatus = new ConcurrentHashMap<>();

    public TestResultAggregator() {
        for (Status status : Status.values()) {
            countsByStatus.put(status, new LongAdder());
        }
    }

    /** Called once per real TestNG result - every test is counted exactly once, since each
     * TestNG callback fires exactly once per test result. */
    public void record(String testName, Status status) {
        countsByStatus.get(status).increment();
    }

    /** Current aggregate counts, safe to call mid-run or after {@code onFinish}. */
    public Map<Status, Long> snapshot() {
        Map<Status, Long> snapshot = new EnumMap<>(Status.class);
        for (Map.Entry<Status, LongAdder> entry : countsByStatus.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().sum());
        }
        return snapshot;
    }
}
