package com.reqres.automation.testdata;

import java.util.function.Consumer;

/**
 * {@code AutoCloseable} wrapper around one {@link UserPoolRow} claimed from
 * {@link UserRowPool}. {@code close()} always releases the row back to the
 * pool - the owning test class's {@code @AfterMethod} is responsible for
 * calling it, so release happens on pass, assertion failure, or any other
 * exception. Safe to close more than once (idempotent). {@link #isReleased()}
 * lets tests prove a specific claim was released without relying on
 * subsequent, independent claims of the same row key.
 */
public final class ClaimedRow implements AutoCloseable {

    private final UserPoolRow row;
    private final Consumer<UserPoolRow> onClose;
    private volatile boolean released;

    ClaimedRow(UserPoolRow row, Consumer<UserPoolRow> onClose) {
        this.row = row;
        this.onClose = onClose;
    }

    public UserPoolRow row() {
        return row;
    }

    // test-support only - true once close() has run for this specific claim
    public boolean isReleased() {
        return released;
    }

    @Override
    public void close() {
        if (!released) {
            released = true;
            onClose.accept(row);
        }
    }
}
