package com.reqres.automation.assertions;

import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;

/**
 * JSON schema validation helper, backed by rest-assured's
 * json-schema-validator module.
 */
public final class SchemaAssertions {

    private SchemaAssertions() {
    }

    /**
     * @param classpathSchemaPath path to a schema file on the test classpath,
     *                            e.g. {@code "schemas/user-schema.json"}
     */
    public static void assertMatchesSchema(Response response, String classpathSchemaPath) {
        response.then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(classpathSchemaPath));
    }
}
