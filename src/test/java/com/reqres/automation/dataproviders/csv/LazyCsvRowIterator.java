package com.reqres.automation.dataproviders.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Backing iterator for {@link CsvLazyDataProvider}. Opens a
 * {@link BufferedReader} on the classpath CSV at construction time, reads
 * and discards the header line, then only reads/parses the next data line
 * inside {@link #hasNext()}/{@link #next()} - never the whole file up
 * front - closing the reader once exhausted. Handles standard CSV quoting
 * ({@code "..."} fields with {@code ""}-escaped embedded quotes) since some
 * rows carry a raw JSON string as one field.
 */
final class LazyCsvRowIterator implements Iterator<Object[]> {

    private final BufferedReader reader;
    private final Function<String[], Object[]> rowMapper;
    private String nextLine;
    private boolean exhausted;

    LazyCsvRowIterator(String classpathCsvPath, Function<String[], Object[]> rowMapper) {
        this.rowMapper = rowMapper;
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(classpathCsvPath);
        if (inputStream == null) {
            throw new IllegalArgumentException("CSV data file not found on classpath: " + classpathCsvPath);
        }
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        skipHeader();
    }

    private void skipHeader() {
        try {
            reader.readLine();
        } catch (IOException e) {
            closeQuietly();
            throw new UncheckedIOException("Failed to read header line from CSV data file", e);
        }
    }

    @Override
    public boolean hasNext() {
        if (exhausted) {
            return false;
        }
        if (nextLine != null) {
            return true;
        }
        try {
            String line = reader.readLine();
            while (line != null && line.isBlank()) {
                line = reader.readLine();
            }
            nextLine = line;
        } catch (IOException e) {
            closeQuietly();
            throw new UncheckedIOException("Failed to read next line from CSV data file", e);
        }
        if (nextLine == null) {
            exhausted = true;
            closeQuietly();
            return false;
        }
        return true;
    }

    @Override
    public Object[] next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more rows in CSV data file");
        }
        String line = nextLine;
        nextLine = null;
        return rowMapper.apply(parseLine(line));
    }

    private void closeQuietly() {
        try {
            reader.close();
        } catch (IOException ignored) {
            // best-effort close once exhausted/errored
        }
    }

    private String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
