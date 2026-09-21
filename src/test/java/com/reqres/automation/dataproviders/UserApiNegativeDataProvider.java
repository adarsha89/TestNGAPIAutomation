package com.reqres.automation.dataproviders;

import org.testng.annotations.DataProvider;

// Fixed negative-path edge sets for UserApiTests - hardcoded rather than CSV-backed since these
// are textbook fixed edge/negative cases, not data that's expected to grow or vary by environment
public final class UserApiNegativeDataProvider {

    private UserApiNegativeDataProvider() {
    }

    @DataProvider(name = "invalidUserIds")
    public static Object[][] invalidUserIds() {
        return new Object[][]{
                {"nonexistent numeric id", "999"},
                {"non-numeric id", "abc"},
                {"negative id", "-1"},
                {"zero id", "0"},
        };
    }

    @DataProvider(name = "incompleteCreateUserPayloads")
    public static Object[][] incompleteCreateUserPayloads() {
        return new Object[][]{
                {"missing name", "{\"job\":\"engineer\"}", false, null, true, "engineer"},
                {"missing job", "{\"name\":\"morpheus\"}", true, "morpheus", false, null},
                {"blank name", "{\"name\":\"\",\"job\":\"engineer\"}", true, "", true, "engineer"},
                {"blank job", "{\"name\":\"morpheus\",\"job\":\"\"}", true, "morpheus", true, ""},
        };
    }
}
