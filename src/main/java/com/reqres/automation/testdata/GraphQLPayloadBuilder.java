package com.reqres.automation.testdata;

import com.reqres.automation.models.graphql.GraphQLRequest;

import java.util.Map;

/**
 * Builds canned GraphQL query payloads for sample/smoke coverage.
 */
public final class GraphQLPayloadBuilder {

    private static final String USER_QUERY =
            "query GetUser($id: ID!) { user(id: $id) { id name } }";

    private static final String COUNTRIES_LIST_QUERY =
            "{ countries { code name } }";

    private static final String COUNTRY_BY_CODE_QUERY =
            "query GetCountry($code: ID!) { country(code: $code) { name capital currency } }";

    private static final String CONTINENTS_LIST_QUERY =
            "{ continents { code name } }";

    private static final String INVALID_FIELD_QUERY =
            "{ country(code: \"BR\") { nonExistentField } }";

    private static final String MALFORMED_SYNTAX_QUERY =
            "{ country(code: \"BR\") { name ";

    private static final String COUNTRY_WITH_CONTINENT_AND_LANGUAGES_QUERY =
            "query GetCountryWithContinentAndLanguages($code: ID!) { "
                    + "country(code: $code) { code name continent { code name } "
                    + "languages { code name } } }";

    private static final String COUNTRY_TYPE_INTROSPECTION_QUERY =
            "{ __type(name: \"Country\") { fields { name } } }";

    /**
     * Aliased dual-root-field query combining {@code country(code)} and
     * {@code continent(code)} in a single request, used to cross-check
     * consistency between the two root fields against the same response.
     */
    private static final String COUNTRY_CONTINENT_CONSISTENCY_QUERY =
            "query GetCountryContinentConsistency($countryCode: ID!, $continentCode: ID!) { "
                    + "countryResult: country(code: $countryCode) { code name continent { code name } "
                    + "languages { code name } } "
                    + "continentResult: continent(code: $continentCode) { name countries { code name } } }";

    private GraphQLPayloadBuilder() {
    }

    public static GraphQLRequest forUserQuery(String id) {
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(USER_QUERY);
        request.setVariables(Map.of("id", id));
        return request;
    }

    /** Lists every country's code and name. */
    public static GraphQLRequest forCountriesList() {
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(COUNTRIES_LIST_QUERY);
        return request;
    }

    /** Looks up a single country's name/capital/currency by its ISO code. */
    public static GraphQLRequest forCountryByCode(String code) {
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(COUNTRY_BY_CODE_QUERY);
        request.setVariables(Map.of("code", code));
        return request;
    }

    /** Lists every continent's code and name. */
    public static GraphQLRequest forContinentsList() {
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(CONTINENTS_LIST_QUERY);
        return request;
    }

    /** Well-formed query referencing a field that doesn't exist on the schema. */
    public static GraphQLRequest forInvalidFieldQuery() {
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(INVALID_FIELD_QUERY);
        return request;
    }

    /** Syntactically broken query (unbalanced braces). */
    public static GraphQLRequest forMalformedSyntaxQuery() {
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(MALFORMED_SYNTAX_QUERY);
        return request;
    }

    /** Looks up a single country's nested continent and languages by its ISO code. */
    public static GraphQLRequest forCountryWithContinentAndLanguages(String code) {
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(COUNTRY_WITH_CONTINENT_AND_LANGUAGES_QUERY);
        request.setVariables(Map.of("code", code));
        return request;
    }

    /** Introspects the {@code Country} type's field names (schema/contract check). */
    public static GraphQLRequest forCountryTypeIntrospection() {
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(COUNTRY_TYPE_INTROSPECTION_QUERY);
        return request;
    }

    /** Aliased query cross-checking a country's continent/languages against the continent's own data. */
    public static GraphQLRequest forCountryContinentConsistencyQuery(String countryCode, String continentCode) {
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(COUNTRY_CONTINENT_CONSISTENCY_QUERY);
        request.setVariables(Map.of("countryCode", countryCode, "continentCode", continentCode));
        return request;
    }
}
