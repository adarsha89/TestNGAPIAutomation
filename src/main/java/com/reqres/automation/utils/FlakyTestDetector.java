package com.reqres.automation.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cross-run flaky-test detection. Records the current run's scenario
 * outcomes and compares them, together with prior runs' outcomes (see
 * {@link RunHistoryStore}), to flag scenarios that pass in some runs and
 * fail in others. Pure in-memory logic, no file I/O and no TestNG
 * dependency, so both {@code CustomTestNGListener} and the CLI's
 * flaky-report task can share this exact comparator.
 */
public final class FlakyTestDetector {

    private final Map<String, Boolean> currentOutcomes = new ConcurrentHashMap<>();

    /** Records this run's outcome for {@code scenarioId}, keyed by method + parameters so
     * different data-provider rows of the same method are tracked independently. */
    public void recordResult(String scenarioId, boolean passed) {
        currentOutcomes.put(scenarioId, passed);
    }

    /** This run's recorded scenario outcomes so far. */
    public Map<String, Boolean> getCurrentOutcomes() {
        return Map.copyOf(currentOutcomes);
    }

    /**
     * A scenario is flaky if, across {@code currentOutcomes} and every map in
     * {@code priorOutcomeSets}, it has at least one recorded pass and at least one recorded
     * fail. A scenario with no prior history at all is never flagged flaky on that basis alone.
     */
    public static Set<String> computeFlakyScenarios(Map<String, Boolean> currentOutcomes,
            List<Map<String, Boolean>> priorOutcomeSets) {
        Set<String> sawPass = new HashSet<>();
        Set<String> sawFail = new HashSet<>();

        recordOutcomes(currentOutcomes, sawPass, sawFail);
        for (Map<String, Boolean> priorOutcomes : priorOutcomeSets) {
            recordOutcomes(priorOutcomes, sawPass, sawFail);
        }

        Set<String> flaky = new HashSet<>(sawPass);
        flaky.retainAll(sawFail);
        return flaky;
    }

    private static void recordOutcomes(Map<String, Boolean> outcomes, Set<String> sawPass, Set<String> sawFail) {
        for (Map.Entry<String, Boolean> entry : outcomes.entrySet()) {
            if (entry.getValue()) {
                sawPass.add(entry.getKey());
            } else {
                sawFail.add(entry.getKey());
            }
        }
    }
}
