package com.reqres.automation.dataproviders;

import org.testng.annotations.DataProvider;

// Fixed existing-vs-nonexistent pair tied to reqres's own fixed /unknown fixture data - hardcoded
// rather than CSV-backed since it's not environment-sensitive and isn't expected to grow
public final class UnknownResourceDataProvider {

    private UnknownResourceDataProvider() {
    }

    @DataProvider(name = "unknownResourceIds")
    public static Object[][] unknownResourceIds() {
        return new Object[][]{
                {"existing color resource", "2", 200},
                {"nonexistent color resource", "23", 404},
        };
    }
}
