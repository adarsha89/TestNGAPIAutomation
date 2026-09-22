package com.reqres.automation.testdata;

import java.util.List;

/**
 * {@code AutoCloseable} aggregate of {@link ClaimedRow}s claimed via {@link UserRowPool#claimRows(int)}.
 * {@code close()} is invoked from the owning test class's {@code @AfterMethod}.
 */
public final class ClaimedRows implements AutoCloseable {

    private final List<ClaimedRow> claimedRows;

    ClaimedRows(List<ClaimedRow> claimedRows) {
        this.claimedRows = claimedRows;
    }

    public int size() {
        return claimedRows.size();
    }

    public UserPoolRow row(int index) {
        return claimedRows.get(index).row();
    }

    public List<UserPoolRow> rows() {
        return claimedRows.stream().map(ClaimedRow::row).toList();
    }

    @Override
    public void close() {
        RuntimeException firstFailure = null;
        for (ClaimedRow claimedRow : claimedRows) {
            try {
                claimedRow.close();
            } catch (RuntimeException e) {
                if (firstFailure == null) {
                    firstFailure = e;
                }
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }
}
