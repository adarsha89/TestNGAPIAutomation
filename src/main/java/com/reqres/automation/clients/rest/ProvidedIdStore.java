package com.reqres.automation.clients.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CSV-backed, durable record of every ID the {@link ProvidedIdResponseTransformer} has accepted,
 * used to reject a resubmission of the same ID with HTTP 400. Default file lives at
 * {@code src/test/resources/data/providedIds/provided-ids.csv} (test-generated, gitignored - see
 * {@code RunHistoryStore} for the precedent), but {@link #at(Path)} lets a test point an isolated
 * instance at its own file.
 *
 * <p>Read-indexed in memory via a {@link ConcurrentHashMap} key set for O(1) duplicate checks.
 * {@link #recordIfAbsent(String)} serializes the check-then-write under one lock so exactly one
 * caller wins a concurrent race for the same ID; the durable CSV append happens before the
 * in-memory set is updated, so a crash between the two steps can at worst duplicate a CSV line
 * (harmless - the duplicate check is set-membership, not line-count), never produce a false
 * "not yet seen" result after a successful accept.</p>
 */
public final class ProvidedIdStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvidedIdStore.class);
    private static final Path DEFAULT_FILE = Path.of("src/test/resources/data/providedIds/provided-ids.csv");
    private static final String CSV_HEADER = "id,recordedAtEpochMillis";

    private static volatile ProvidedIdStore instance;

    private final Path file;
    private final Set<String> recordedIds = ConcurrentHashMap.newKeySet();
    private final Object writeLock = new Object();

    private ProvidedIdStore(Path file) {
        this.file = file;
        loadExisting();
    }

    /** Process-wide singleton backed by the default file - what the embedded mock server uses. */
    public static ProvidedIdStore instance() {
        ProvidedIdStore local = instance;
        if (local == null) {
            synchronized (ProvidedIdStore.class) {
                local = instance;
                if (local == null) {
                    local = new ProvidedIdStore(DEFAULT_FILE);
                    instance = local;
                }
            }
        }
        return local;
    }

    /** Test-support: a fresh, isolated instance backed by {@code file}, independent of the
     * process-wide singleton. */
    public static ProvidedIdStore at(Path file) {
        return new ProvidedIdStore(file);
    }

    /**
     * Records {@code id} iff it has never been recorded before (by this instance, or a prior
     * instance pointed at the same file). Returns {@code true} iff this call is the one that
     * recorded it.
     */
    public boolean recordIfAbsent(String id) {
        synchronized (writeLock) {
            if (recordedIds.contains(id)) {
                return false;
            }
            append(id);
            recordedIds.add(id);
            return true;
        }
    }

    /** Test-support read: whether {@code id} has already been recorded. */
    public boolean isRecorded(String id) {
        return recordedIds.contains(id);
    }

    private void append(String id) {
        try {
            Path parentDirectory = file.toAbsolutePath().getParent();
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }
            boolean isNewFile = !Files.exists(file);
            List<String> linesToWrite = isNewFile
                    ? List.of(CSV_HEADER, csvLine(id))
                    : List.of(csvLine(id));
            Files.write(file, linesToWrite, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to durably append provided ID '" + id + "' to " + file, e);
        }
    }

    private static String csvLine(String id) {
        return id + "," + System.currentTimeMillis();
    }

    private void loadExisting() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (i == 0 && line.equals(CSV_HEADER)) {
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                recordedIds.add(line.substring(0, line.indexOf(',')));
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load existing provided IDs from {} - starting from an empty in-memory set", file,
                    e);
        }
    }
}
