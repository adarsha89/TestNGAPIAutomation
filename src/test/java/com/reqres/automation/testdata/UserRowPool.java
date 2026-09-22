package com.reqres.automation.testdata;

import com.reqres.automation.config.ConfigLoader;
import com.reqres.automation.dataproviders.csv.CsvLazyDataProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe pool of the fixed rows in {@code row-locked-user-pool.csv}. Each row is held by
 * exactly one caller at a time; {@link #claimRow()} blocks until one frees up. Singleton, not
 * {@code ThreadLocal} - the pool itself is the shared resource under test.
 */
public final class UserRowPool {

    private static final String CSV_PATH = "testdata/row-locked-user-pool.csv";
    private static final Duration DEFAULT_CLAIM_TIMEOUT =
            Duration.ofMillis(ConfigLoader.load().getRowPoolClaimTimeoutMs());

    private static volatile UserRowPool instance;

    private final ArrayBlockingQueue<UserPoolRow> availableRows;
    private final Set<String> claimedRowKeys = ConcurrentHashMap.newKeySet();
    private final int capacity;

    private UserRowPool(List<UserPoolRow> rows) {
        this.capacity = rows.size();
        this.availableRows = new ArrayBlockingQueue<>(rows.size());
        this.availableRows.addAll(rows);
    }

    public static UserRowPool instance() {
        UserRowPool local = instance;
        if (local == null) {
            synchronized (UserRowPool.class) {
                local = instance;
                if (local == null) {
                    local = new UserRowPool(loadRows());
                    instance = local;
                }
            }
        }
        return local;
    }

    private static List<UserPoolRow> loadRows() {
        List<UserPoolRow> rows = new ArrayList<>();
        Iterator<Object[]> csvRows = CsvLazyDataProvider.read(CSV_PATH, UserRowPool::toPoolRow);
        while (csvRows.hasNext()) {
            rows.add((UserPoolRow) csvRows.next()[0]);
        }
        return rows;
    }

    private static Object[] toPoolRow(String[] row) {
        UserPoolRow poolRow = new UserPoolRow(row[0], Integer.parseInt(row[1]), row[2], row[3], row[4], row[5],
                row[6]);
        return new Object[]{poolRow};
    }

    /** Claims one row, blocking up to the default timeout if none is free. */
    public ClaimedRow claimRow() {
        return claimRow(DEFAULT_CLAIM_TIMEOUT);
    }

    public ClaimedRow claimRow(Duration timeout) {
        UserPoolRow row = poll(timeout);
        claimedRowKeys.add(row.rowKey());
        return new ClaimedRow(row, this::release);
    }

    /** Claims {@code count} distinct rows, blocking up to the default timeout per row. */
    public ClaimedRows claimRows(int count) {
        return claimRows(count, DEFAULT_CLAIM_TIMEOUT);
    }

    public ClaimedRows claimRows(int count, Duration timeout) {
        List<ClaimedRow> claimed = new ArrayList<>(count);
        try {
            for (int i = 0; i < count; i++) {
                claimed.add(claimRow(timeout));
            }
        } catch (RuntimeException e) {
            claimed.forEach(ClaimedRow::close);
            throw e;
        }
        return new ClaimedRows(claimed);
    }

    private UserPoolRow poll(Duration timeout) {
        try {
            UserPoolRow row = availableRows.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (row == null) {
                throw new IllegalStateException("Timed out after " + timeout + " waiting for a free row from the "
                        + "pool (capacity=" + capacity + ")");
            }
            return row;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a free pool row", e);
        }
    }

    private void release(UserPoolRow row) {
        claimedRowKeys.remove(row.rowKey());
        availableRows.offer(row);
    }

    /** Row count the pool was seeded with. */
    public int capacity() {
        return capacity;
    }

    // test-support only
    public int availableCount() {
        return availableRows.size();
    }

    // test-support only - snapshot of currently claimed row keys
    public Set<String> claimedRowKeysSnapshot() {
        return Set.copyOf(claimedRowKeys);
    }
}
