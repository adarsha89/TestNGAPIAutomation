package com.reqres.automation.graphql;

import org.testng.annotations.DataProvider;

/**
 * {@link DataProvider} methods feeding {@link CountriesQueryTests}. Kept
 * separate from the test class so provider data stays independent of test
 * logic. TestNG requires these to be {@code static} since they're referenced
 * via {@code dataProviderClass} rather than living on the test class itself.
 */
public final class CountriesQueryDataProvider {

    private CountriesQueryDataProvider() {
    }

    @DataProvider(name = "knownCountries")
    public static Object[][] knownCountries() {
        return new Object[][]{
                {"BR", "Brazil", "Brasília", "BRL"},
                {"IN", "India", "New Delhi", "INR"},
                {"JP", "Japan", "Tokyo", "JPY"},
        };
    }

    @DataProvider(name = "countryContinentPairs")
    public static Object[][] countryContinentPairs() {
        return new Object[][]{
                {"BR", "SA", "South America", "pt", "Portuguese"},
                {"JP", "AS", "Asia", "ja", "Japanese"},
        };
    }
}
