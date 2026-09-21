package com.reqres.automation.utils;

import com.reqres.automation.dataproviders.TestImpactAnalyzerDataProvider;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

@Story("Mapping changed areas to impacted test classes")
public class TestImpactAnalyzerTests {

    private final TestImpactAnalyzer analyzer = new TestImpactAnalyzer();

    @Test(groups = {"utils", "regression"},
            dataProvider = "changedAreasAndExpectedTests", dataProviderClass = TestImpactAnalyzerDataProvider.class)
    @Description("getTestcasesImpacted(changedAreas) returns exactly the expected test class names for a known "
            + "changed area - no extra, no missing")
    public void shouldReturnExactExpectedTestsForKnownChangedAreas(String caseName, List<String> changedAreas,
            Set<String> expectedTestNames) {
        Set<String> impacted = analyzer.getTestcasesImpacted(changedAreas);

        Assert.assertEquals(impacted, expectedTestNames, "Case: " + caseName);
    }

    @Test(groups = {"utils", "regression"})
    @Description("A changed area with no mapped tests returns an empty set, no exception, no false-positive match")
    public void shouldReturnEmptySetForUnmappedArea() {
        Set<String> impacted = analyzer.getTestcasesImpacted(List.of("nonexistent-area"));

        Assert.assertTrue(impacted.isEmpty(), "Expected no impacted tests for an unmapped area");
    }
}
