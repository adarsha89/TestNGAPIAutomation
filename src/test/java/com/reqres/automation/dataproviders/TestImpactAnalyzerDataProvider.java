package com.reqres.automation.dataproviders;

import org.testng.annotations.DataProvider;

import java.util.List;
import java.util.Set;

// {changedAreas, expectedTestNames} pairs for TestImpactAnalyzerTests, built directly from the
// real committed fixture at src/test/resources/testimpact/dependency-graph.json
public final class TestImpactAnalyzerDataProvider {

    private TestImpactAnalyzerDataProvider() {
    }

    @DataProvider(name = "changedAreasAndExpectedTests")
    public static Object[][] changedAreasAndExpectedTests() {
        return new Object[][]{
                {"single area, one mapped test",
                        List.of("webhook"), Set.of("WebhookReceiptTests")},
                {"multiple areas, union of mapped tests",
                        List.of("auth", "rate-limit"), Set.of("AuthStrategyTests", "RateLimitTests")},
                {"area shared by several rest tests",
                        List.of("users"),
                        Set.of("UserApiTests", "RequestResponseMaskingTests", "AuthStrategyTests", "RateLimitTests",
                                "Fallback503Tests")},
        };
    }
}
