package com.reqres.automation.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.reqres.automation.assertions.GraphQLAssertions;
import com.reqres.automation.assertions.ResponseAssertions;
import com.reqres.automation.base.BaseGraphQLTest;
import com.reqres.automation.models.graphql.GraphQLRequest;
import com.reqres.automation.models.graphql.GraphQLResponse;
import com.reqres.automation.testdata.GraphQLPayloadBuilder;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Coverage against the real, public countries.trevorblades.com GraphQL
 * endpoint (config-driven via {@code graphql.base.url} - see
 * {@code BaseGraphQLTest}/per-env properties).
 */
@Story("Public countries GraphQL API")
public class CountriesQueryTests extends BaseGraphQLTest {

    /**
     * Aliased dual-root-field query combining {@code country(code)} and
     * {@code continent(code)} in a single request, specific to the
     * cross-consistency case below - not a generally reusable shape, so it
     * is kept as a dedicated constant here rather than added to
     * {@link GraphQLPayloadBuilder}.
     */
    private static final String COUNTRY_CONTINENT_CONSISTENCY_QUERY =
            "query GetCountryContinentConsistency($countryCode: ID!, $continentCode: ID!) { "
                    + "countryResult: country(code: $countryCode) { code name continent { code name } "
                    + "languages { code name } } "
                    + "continentResult: continent(code: $continentCode) { name countries { code name } } }";

    @Test(groups = {"graphql", "external", "regression"})
    @Description("List all countries and assert every entry has a non-blank code and name")
    public void shouldListCountriesWithCodeAndName() {
        GraphQLRequest request = GraphQLPayloadBuilder.forCountriesList();

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLResponse parsed = response.as(GraphQLResponse.class);
        JsonNode countries = parsed.getData().get("countries");
        Assert.assertTrue(countries.isArray() && countries.size() > 0, "Expected a non-empty countries array");
        for (JsonNode country : countries) {
            Assert.assertFalse(country.get("code").asText().isBlank(), "Expected non-blank country code");
            Assert.assertFalse(country.get("name").asText().isBlank(), "Expected non-blank country name");
        }
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
        GraphQLResponse parsed = response.as(GraphQLResponse.class);
        JsonNode country = parsed.getData().get("country");
        Assert.assertEquals(country.get("name").asText(), expectedName);
        Assert.assertEquals(country.get("capital").asText(), expectedCapital);
        Assert.assertEquals(country.get("currency").asText(), expectedCurrency);
    }

    @Test(groups = {"graphql", "external", "regression"})
    @Description("Look up an unknown country code and assert data.country is null with no errors key")
    public void shouldReturnNullDataForUnknownCountryCode() {
        GraphQLRequest request = GraphQLPayloadBuilder.forCountryByCode("ZZ");

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLResponse parsed = response.as(GraphQLResponse.class);
        Assert.assertTrue(parsed.getData().get("country").isNull(), "Expected data.country to be JSON null");
    }

    @Test(groups = {"graphql", "external", "regression"})
    @Description("List all continents and assert every entry has a non-blank code and name")
    public void shouldListContinentsWithCodeAndName() {
        GraphQLRequest request = GraphQLPayloadBuilder.forContinentsList();

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLResponse parsed = response.as(GraphQLResponse.class);
        JsonNode continents = parsed.getData().get("continents");
        Assert.assertTrue(continents.isArray() && continents.size() > 0, "Expected a non-empty continents array");
        for (JsonNode continent : continents) {
            Assert.assertFalse(continent.get("code").asText().isBlank(), "Expected non-blank continent code");
            Assert.assertFalse(continent.get("name").asText().isBlank(), "Expected non-blank continent name");
        }
    }

    @Test(groups = {"graphql", "external", "regression"})
    @Description("A well-formed query referencing a non-existent field returns HTTP 200 with a GraphQL errors array")
    public void shouldReturnGraphQLErrorForNonExistentField() {
        GraphQLRequest request = GraphQLPayloadBuilder.forInvalidFieldQuery();

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        List<GraphQLResponse.GraphQLError> errors = GraphQLAssertions.assertHasErrors(response);
        boolean referencesBadField = errors.stream()
                .anyMatch(error -> error.getMessage() != null && error.getMessage().contains("nonExistentField"));
        Assert.assertTrue(referencesBadField, "Expected an error message referencing the bad field 'nonExistentField'");
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
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(COUNTRY_CONTINENT_CONSISTENCY_QUERY);
        request.setVariables(Map.of("countryCode", countryCode, "continentCode", continentCode));

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLResponse parsed = response.as(GraphQLResponse.class);
        JsonNode country = parsed.getData().get("countryResult");
        JsonNode continent = parsed.getData().get("continentResult");

        JsonNode countryContinent = country.get("continent");
        Assert.assertEquals(countryContinent.get("code").asText(), continentCode,
                "Expected country.continent.code to match the queried continent code");
        Assert.assertEquals(countryContinent.get("name").asText(), expectedContinentName,
                "Expected country.continent.name to match the expected continent name");

        boolean hasExpectedLanguage = StreamSupport.stream(country.get("languages").spliterator(), false)
                .anyMatch(language -> expectedLanguageCode.equals(language.get("code").asText())
                        && expectedLanguageName.equals(language.get("name").asText()));
        Assert.assertTrue(hasExpectedLanguage,
                "Expected country.languages to contain " + expectedLanguageName + " (" + expectedLanguageCode + ")");

        Assert.assertEquals(continent.get("name").asText(), expectedContinentName,
                "Expected continent.name to match the expected continent name");
        boolean continentListsCountry = StreamSupport.stream(continent.get("countries").spliterator(), false)
                .anyMatch(entry -> countryCode.equals(entry.get("code").asText()));
        Assert.assertTrue(continentListsCountry,
                "Expected continent.countries (from the same response) to contain " + countryCode);
    }

    @Test(groups = {"graphql", "external", "regression"})
    @Description("Introspect the Country type and assert the field names this suite asserts on are actually "
            + "present in the live schema")
    public void shouldExposeExpectedFieldsPerLiveSchemaIntrospection() {
        GraphQLRequest request = GraphQLPayloadBuilder.forCountryTypeIntrospection();

        Response response = client().postQuery(request);

        ResponseAssertions.assertStatusCode(response, 200);
        GraphQLAssertions.assertNoErrors(response);
        GraphQLResponse parsed = response.as(GraphQLResponse.class);
        JsonNode fields = parsed.getData().get("__type").get("fields");
        List<String> fieldNames = StreamSupport.stream(fields.spliterator(), false)
                .map(field -> field.get("name").asText())
                .collect(Collectors.toList());

        List<String> expectedFields = List.of("code", "name", "capital", "currency", "continent", "languages");
        for (String expectedField : expectedFields) {
            Assert.assertTrue(fieldNames.contains(expectedField),
                    "Expected Country type to expose field '" + expectedField + "'. Actual fields: " + fieldNames);
        }
    }
}
