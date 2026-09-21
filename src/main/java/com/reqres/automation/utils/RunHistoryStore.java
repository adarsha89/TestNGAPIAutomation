package com.reqres.automation.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * File-based storage for per-run scenario outcomes, used to compare a run against its
 * predecessors across separate JVM invocations. Directory defaults to {@code
 * src/test/resources/data/previousTestResults} but is overridable for tests.
 */
public final class RunHistoryStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunHistoryStore.class);
    private static final String DEFAULT_DIRECTORY = "src/test/resources/data/previousTestResults";
    private static final int MAX_RETAINED_RUNS = 10;
    private static final String SCENARIO_OUTCOMES_FIELD = "scenarioOutcomes";

    private final Path directory;

    public RunHistoryStore() {
        this(Path.of(DEFAULT_DIRECTORY));
    }

    public RunHistoryStore(Path directory) {
        this.directory = directory;
    }

    /** Reads every {@code run-*.json} file in the directory and returns each one's
     * {@code scenarioOutcomes} map, most-recently-modified file first. A missing directory, or
     * an unreadable/malformed file, is skipped rather than thrown - callers always get a usable
     * (possibly empty) list. */
    public List<Map<String, Boolean>> load() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }

        List<Path> runFiles = listRunFiles();
        runFiles.sort(Comparator.comparingLong(this::lastModified).reversed());

        List<Map<String, Boolean>> outcomeSets = new ArrayList<>();
        for (Path file : runFiles) {
            Map<String, Boolean> scenarioOutcomes = readScenarioOutcomes(file);
            if (scenarioOutcomes != null) {
                outcomeSets.add(scenarioOutcomes);
            }
        }
        return outcomeSets;
    }

    /** Persists {@code scenarioOutcomes} as a new timestamped file in the directory, then
     * prunes to the most-recent {@value #MAX_RETAINED_RUNS} files to avoid unbounded growth. */
    public void persist(Map<String, Boolean> scenarioOutcomes) {
        try {
            Files.createDirectories(directory);
            Path runFile = directory.resolve("run-" + Instant.now().toEpochMilli()
                    + "-" + ThreadLocalRandom.current().nextInt(10_000) + ".json");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put(SCENARIO_OUTCOMES_FIELD, scenarioOutcomes);
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(runFile.toFile(), payload);

            pruneToMostRecent();
        } catch (IOException e) {
            LOGGER.warn("Failed to persist run history to {}", directory, e);
        }
    }

    private List<Path> listRunFiles() {
        File[] files = directory.toFile().listFiles((dir, name) -> name.startsWith("run-") && name.endsWith(".json"));
        if (files == null) {
            return List.of();
        }
        List<Path> paths = new ArrayList<>();
        for (File file : files) {
            paths.add(file.toPath());
        }
        return paths;
    }

    private Map<String, Boolean> readScenarioOutcomes(Path file) {
        try {
            JsonRunFile runFile = new ObjectMapper().readValue(file.toFile(), JsonRunFile.class);
            return runFile.scenarioOutcomes;
        } catch (IOException e) {
            LOGGER.warn("Skipping unreadable/malformed prior run file {}", file, e);
            return null;
        }
    }

    private void pruneToMostRecent() throws IOException {
        List<Path> runFiles = listRunFiles();
        if (runFiles.size() <= MAX_RETAINED_RUNS) {
            return;
        }
        runFiles.sort(Comparator.comparingLong(this::lastModified).reversed());
        for (Path staleFile : runFiles.subList(MAX_RETAINED_RUNS, runFiles.size())) {
            Files.deleteIfExists(staleFile);
        }
    }

    private long lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    /** Deserialization target for a persisted run file - only the field this store cares about. */
    private static final class JsonRunFile {
        public Map<String, Boolean> scenarioOutcomes;
    }
}
