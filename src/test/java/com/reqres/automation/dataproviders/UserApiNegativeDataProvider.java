package com.reqres.automation.dataproviders;

import org.testng.annotations.DataProvider;

// negative-path rows for UserApiTests; static since TestNG pulls these via dataProviderClass
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
                // caseName, rawJsonBody, nameExpectedPresent, expectedNameValue, jobExpectedPresent, expectedJobValue
                {"missing name", "{\"job\":\"engineer\"}", false, null, true, "engineer"},
                {"missing job", "{\"name\":\"morpheus\"}", true, "morpheus", false, null},
                {"blank name", "{\"name\":\"\",\"job\":\"engineer\"}", true, "", true, "engineer"},
                {"blank job", "{\"name\":\"morpheus\",\"job\":\"\"}", true, "morpheus", true, ""},
        };
    }
}
