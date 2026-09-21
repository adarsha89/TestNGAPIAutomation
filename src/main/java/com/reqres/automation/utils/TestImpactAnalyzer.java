package com.reqres.automation.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test-selection tool: reads a committed mapping of this suite's test class
 * names to the concern areas they cover
 * ({@code src/main/resources/testimpact/dependency-graph.json}) and resolves
 * which test classes are impacted by a given set of changed areas. Loaded
 * from the classpath so it works the same whether run from an IDE or Maven.
 */
public final class TestImpactAnalyzer {

    private static final String DEPENDENCY_GRAPH_RESOURCE = "testimpact/dependency-graph.json";

    private final Map<String, List<String>> testToAreas;

    public TestImpactAnalyzer() {
        this.testToAreas = loadDependencyGraph();
    }

    /** Returns every test class name in the dependency graph - the default scope when no
     * changed area is given. */
    public Set<String> getAllTestClasses() {
        return new HashSet<>(testToAreas.keySet());
    }

    /** Returns the exact set of test class names mapped to at least one of {@code changedAreas} -
     * empty (never null, never an exception) if none match. */
    public Set<String> getTestcasesImpacted(List<String> changedAreas) {
        Set<String> impacted = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : testToAreas.entrySet()) {
            if (!Collections.disjoint(entry.getValue(), changedAreas)) {
                impacted.add(entry.getKey());
            }
        }
        return impacted;
    }

    private static Map<String, List<String>> loadDependencyGraph() {
        try (InputStream in = TestImpactAnalyzer.class.getClassLoader()
                .getResourceAsStream(DEPENDENCY_GRAPH_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Missing required test-impact fixture on classpath: " + DEPENDENCY_GRAPH_RESOURCE);
            }
            return new ObjectMapper().readValue(in, new TypeReference<Map<String, List<String>>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load test-impact fixture: " + DEPENDENCY_GRAPH_RESOURCE, e);
        }
    }
}
