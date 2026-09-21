package com.reqres.automation.dataproviders;

import com.reqres.automation.dataproviders.csv.CsvLazyDataProvider;
import org.testng.annotations.DataProvider;

import java.util.Iterator;

// listUsersPagination is CSV-backed (data-driven, plausibly grows with the target dataset);
// updateUserCases is a fixed PUT-vs-PATCH matrix, hardcoded rather than externalized
public final class UserApiDataProvider {

    private UserApiDataProvider() {
    }

    @DataProvider(name = "listUsersPagination")
    public static Iterator<Object[]> listUsersPagination() {
        return CsvLazyDataProvider.read("testdata/list-users-pagination.csv",
                row -> new Object[]{row[0], Integer.parseInt(row[1])});
    }

    @DataProvider(name = "updateUserCases")
    public static Object[][] updateUserCases() {
        return new Object[][]{
                {"full update (PUT)", "PUT", "{\"name\":\"morpheus\",\"job\":\"zion resident\"}",
                        "morpheus", "zion resident"},
                {"partial update (PATCH)", "PATCH", "{\"job\":\"zion resident\"}", null, "zion resident"},
        };
    }
}
