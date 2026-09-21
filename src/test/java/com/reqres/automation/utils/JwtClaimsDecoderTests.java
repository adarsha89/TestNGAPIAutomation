package com.reqres.automation.utils;

import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.Optional;

/**
 * Pure unit coverage of {@link JwtClaimsDecoder} - decodes the {@code exp}
 * claim only, deliberately without verifying the JWT's signature (see the
 * class Javadoc).
 */
@Story("JWT exp claim decoding for cache bookkeeping")
public class JwtClaimsDecoderTests {

    // header {"alg":"HS256","typ":"JWT"}, payload {"sub":"test-client","exp":4102444800}
    // (exp=4102444800 -> 2100-01-01T00:00:00Z)
    private static final String WELL_FORMED_JWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LWNsaWVudCIsImV4cCI6NDEwMjQ0NDgwMH0.sig";

    @Test(groups = {"utils", "regression"})
    @Description("A well-formed JWT with a numeric exp claim decodes to the exact Instant that epoch-seconds value "
            + "represents")
    public void shouldDecodeNumericExpClaimToMatchingInstant() {
        Optional<Instant> decoded = JwtClaimsDecoder.decodeExpiry(WELL_FORMED_JWT);

        Assert.assertTrue(decoded.isPresent(), "Expected a decodable exp claim");
        Assert.assertEquals(decoded.get(), Instant.ofEpochSecond(4102444800L));
    }

    @DataProvider(name = "malformedOrMissingExp")
    public Object[][] malformedOrMissingExp() {
        return new Object[][]{
                {null},
                {""},
                {"not-a-jwt"},
                {"only.two-segments"},
                // payload {"sub":"test-client"} - no exp claim at all
                {"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LWNsaWVudCJ9.sig"},
                // payload {"sub":"test-client","exp":"not-a-number"} - non-numeric exp
                {"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
                        + ".eyJzdWIiOiJ0ZXN0LWNsaWVudCIsImV4cCI6Im5vdC1hLW51bWJlciJ9.sig"},
                // segment 2 isn't valid base64url/JSON at all
                {"eyJhbGciOiJIUzI1NiJ9.###not-base64###.sig"},
        };
    }

    @Test(groups = {"utils", "regression"}, dataProvider = "malformedOrMissingExp")
    @Description("A missing/non-numeric exp claim, or a malformed compact serialization, returns an absent result "
            + "rather than throwing")
    public void shouldReturnEmptyForMalformedOrMissingExp(String jwt) {
        Optional<Instant> decoded = JwtClaimsDecoder.decodeExpiry(jwt);

        Assert.assertTrue(decoded.isEmpty(), "Expected no derivable exp claim for input: " + jwt);
    }
}
