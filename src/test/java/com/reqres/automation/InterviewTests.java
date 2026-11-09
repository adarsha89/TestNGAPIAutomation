package com.reqres.automation;

import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Standalone interview/skills exercise: detect whether an identifier has
 * already been used earlier in the same run. This is {@code not} part of the
 * suite's regular coverage.
 *
 * <p>(a) This class is a standalone demonstration of duplicate-ID detection,
 * not suite coverage - it makes no HTTP call and exercises no client,
 * service, or assertion layer.
 *
 * <p>(b) It is deliberately absent from {@code testng.xml} and
 * {@code dependency-graph.json} and carries no TestNG {@code groups} - it
 * must never be registered in either file, and must never run via
 * {@code ./mvnw test}, {@code -Dgroups=smoke}, or {@code -Dgroups=regression}.
 * Run it only with {@code ./mvnw test -Dtest=InterviewTests}.
 *
 * <p>(c) Invocations must run sequentially: {@link #idSequence()} must never
 * be marked {@code parallel = true}, and this class must never be added to
 * {@code testng.xml}, whose suite-level {@code data-provider-thread-count="8"}
 * would otherwise apply. Duplicate detection depends on observing invocations
 * in a defined order.
 *
 * <p>(d) See {@code CountingFixture}/{@code FlappyFixture} for repo precedent
 * on deliberately-failing demonstration tests kept out of {@code testng.xml}.
 */
@Story("Duplicate/already-used identifier detection within a single run")
public class InterviewTests {

    private static final Set<String> SEEN_IDS = new HashSet<>();

    @DataProvider(name = "idSequence")
    public Object[][] idSequence() {
        return new Object[][]{
                {"first use of ID-1", "ID-1"},
                {"first use of ID-2", "ID-2"},
                {"repeat of ID-1", "ID-1"},
                {"first use of ID-3 (after a failure)", "ID-3"},
        };
    }

    @Test(dataProvider = "idSequence")
    @Description("The first occurrence of an identifier in a run passes and is recorded; a repeat occurrence fails "
            + "with a message naming the duplicate, and a failed invocation doesn't corrupt tracking for later ones")
    public void shouldFailWhenIdAlreadyUsedInRun(String caseName, String id) {
        boolean firstSeen = SEEN_IDS.add(id);


        Assert.assertTrue(firstSeen, "Identifier already used earlier in this run: " + id);
    }
}
