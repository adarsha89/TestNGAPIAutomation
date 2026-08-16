package com.reqres.automation.graphql;

import com.reqres.automation.assertions.GraphQLAssertions;
import com.reqres.automation.assertions.ResponseAssertions;
import com.reqres.automation.base.BaseGraphQLTest;
import com.reqres.automation.models.graphql.GraphQLRequest;
import com.reqres.automation.testdata.GraphQLPayloadBuilder;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * Coverage against the real, public countries.trevorblades.com GraphQL
 * endpoint (config-driven via {@code graphql.base.url} - see
 * {@code BaseGraphQLTest}/per-env properties).
 */
@Story("Public countries GraphQL API")
public class CountriesQueryTests extends BaseGraphQLTest {

    @Test(groups = {"graphql", "external", "regression"})
    @Description("List all countries and assert every entry has a non-blank code and name")
    public void shouldListCountriesWithCodeAndName() {
        GraphQLRequest request = GraphQLPayloadBuilder.forCountriesList();

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLAssertions.assertEachArrayElementFieldsNotBlank(response, "countries", "code", "name");
    }

    @DataProvider(name = "knownCountries")
    public Object[][] knownCountries() {
        return new Object[][]{
                {"BR", "Brazil", "Brasília", "BRL"},
                {"IN", "India", "New Delhi", "INR"},
                {"JP", "Japan", "Tokyo", "JPY"},
        };
    }

    @Test(dataProvider = "knownCountries", groups = {"graphql", "external", "regression"})
    @Description("Look up a known country by its ISO code and assert its real-world name/capital/currency")
    public void shouldReturnKnownCountryByValidCode(String code, String expectedName, String expectedCapital,
                                                      String expectedCurrency) {
        GraphQLRequest request = GraphQLPayloadBuilder.forCountryByCode(code);

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLAssertions.assertFieldEquals(response, "country.name", expectedName);
        GraphQLAssertions.assertFieldEquals(response, "country.capital", expectedCapital);
        GraphQLAssertions.assertFieldEquals(response, "country.currency", expectedCurrency);
    }

    @Test(groups = {"graphql", "external", "regression"})
    @Description("Look up an unknown country code and assert data.country is null with no errors key")
    public void shouldReturnNullDataForUnknownCountryCode() {
        GraphQLRequest request = GraphQLPayloadBuilder.forCountryByCode("ZZ");

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLAssertions.assertFieldIsNull(response, "country");
    }

    @Test(groups = {"graphql", "external", "regression"})
    @Description("List all continents and assert every entry has a non-blank code and name")
    public void shouldListContinentsWithCodeAndName() {
        GraphQLRequest request = GraphQLPayloadBuilder.forContinentsList();

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLAssertions.assertEachArrayElementFieldsNotBlank(response, "continents", "code", "name");
    }

    @Test(groups = {"graphql", "external", "regression"})
    @Description("A well-formed query referencing a non-existent field returns HTTP 200 with a GraphQL errors array")
    public void shouldReturnGraphQLErrorForNonExistentField() {
        GraphQLRequest request = GraphQLPayloadBuilder.forInvalidFieldQuery();

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertErrorMessageContains(response, "nonExistentField");
    }

    @Test(groups = {"graphql", "external", "regression"})
    @Description("A syntactically malformed query (unbalanced braces) returns HTTP 400 with a GraphQL errors array")
    public void shouldReturnGraphQLErrorForSyntacticallyMalformedQuery() {
        GraphQLRequest request = GraphQLPayloadBuilder.forMalformedSyntaxQuery();

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 400);
        GraphQLAssertions.assertHasErrors(response);
    }

    @DataProvider(name = "countryContinentPairs")
    public Object[][] countryContinentPairs() {
        return new Object[][]{
                {"BR", "SA", "South America", "pt", "Portuguese"},
                {"JP", "AS", "Asia", "ja", "Japanese"},
        };
    }

    @Test(dataProvider = "countryContinentPairs", groups = {"graphql", "external", "regression"})
    @Description("Retrieve a country's nested continent/languages and the continent's country list in one "
            + "aliased query, cross-checked for internal consistency against the same response")
    public void shouldReturnNestedContinentAndLanguagesConsistently(String countryCode, String continentCode,
                                                                      String expectedContinentName,
                                                                      String expectedLanguageCode,
                                                                      String expectedLanguageName) {
        GraphQLRequest request = GraphQLPayloadBuilder.forCountryContinentConsistencyQuery(countryCode, continentCode);

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLAssertions.assertFieldEquals(response, "countryResult.continent.code", continentCode);
        GraphQLAssertions.assertFieldEquals(response, "countryResult.continent.name", expectedContinentName);
        GraphQLAssertions.assertArrayContainsMatchingElement(response, "countryResult.languages",
                Map.of("code", expectedLanguageCode, "name", expectedLanguageName));
        GraphQLAssertions.assertFieldEquals(response, "continentResult.name", expectedContinentName);
        GraphQLAssertions.assertArrayContainsMatchingElement(response, "continentResult.countries",
                Map.of("code", countryCode));
    }

    @Test(groups = {"graphql", "external", "regression"})
    @Description("Introspect the Country type and assert the field names this suite asserts on are actually "
            + "present in the live schema")
    public void shouldExposeExpectedFieldsPerLiveSchemaIntrospection() {
        GraphQLRequest request = GraphQLPayloadBuilder.forCountryTypeIntrospection();

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        List<String> expectedFields = List.of("code", "name", "capital", "currency", "continent", "languages");
        GraphQLAssertions.assertArrayFieldValuesContainAll(response, "__type.fields", "name", expectedFields);
    }
}
