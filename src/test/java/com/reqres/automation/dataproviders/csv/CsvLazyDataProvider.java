package com.reqres.automation.dataproviders.csv;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Reusable lazy CSV-backed TestNG DataProvider utility. Each row is read and
 * parsed one at a time as the iterator is consumed - the whole file is never
 * loaded into memory up front. Backed by {@link LazyCsvRowIterator}.
 */
public final class CsvLazyDataProvider {

    private CsvLazyDataProvider() {
    }

    /**
     * @param classpathCsvPath path resolved against the test classpath (e.g. {@code testdata/foo.csv})
     * @param rowMapper        maps a parsed CSV row (header line already skipped) to the
     *                         {@code Object[]} a TestNG {@code @Test} method receives
     */
    public static Iterator<Object[]> read(String classpathCsvPath, Function<String[], Object[]> rowMapper) {
        return new LazyCsvRowIterator(classpathCsvPath, rowMapper);
    }
}
