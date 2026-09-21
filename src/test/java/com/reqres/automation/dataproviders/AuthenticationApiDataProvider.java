package com.reqres.automation.dataproviders;

import com.reqres.automation.dataproviders.csv.CsvLazyDataProvider;
import org.testng.annotations.DataProvider;

import java.util.Iterator;
import java.util.function.Function;

// CSV-backed rows for AuthenticationApiTests (login and register cases), read lazily one row at
// a time rather than drained into memory up front
public final class AuthenticationApiDataProvider {

    private AuthenticationApiDataProvider() {
    }

    @DataProvider(name = "loginCases")
    public static Iterator<Object[]> loginCases() {
        return CsvLazyDataProvider.read("testdata/login-cases.csv", rowMapper());
    }

    @DataProvider(name = "registerCases")
    public static Iterator<Object[]> registerCases() {
        return CsvLazyDataProvider.read("testdata/register-cases.csv", rowMapper());
    }

    private static Function<String[], Object[]> rowMapper() {
        return row -> {
            String caseName = row[0];
            String rawJsonBody = row[1];
            int expectedStatus = Integer.parseInt(row[2]);
            boolean expectTokenPresent = Boolean.parseBoolean(row[3]);
            String expectedErrorMessage = row[4].isEmpty() ? null : row[4];
            return new Object[]{caseName, rawJsonBody, expectedStatus, expectTokenPresent, expectedErrorMessage};
        };
    }
}
